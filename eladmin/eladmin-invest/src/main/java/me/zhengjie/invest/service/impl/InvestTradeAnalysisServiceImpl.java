/*
*  Copyright 2019-2023 Zheng Jie
*
*  Licensed under the Apache License, Version 2.0 (the "License");
*  you may not use this file except in compliance with the License.
*  You may obtain a copy of the License at
*
*  http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software
*  distributed under the License is distributed on an "AS IS" BASIS,
*  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*  See the License for the specific language governing permissions and
*  limitations under the License.
*/
package me.zhengjie.invest.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import me.zhengjie.invest.domain.InvestTradeAnalysis;
import me.zhengjie.invest.domain.vo.InvestTradeAnalysisOcrVO;
import me.zhengjie.utils.FileUtil;
import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import me.zhengjie.invest.service.InvestTradeAnalysisService;
import me.zhengjie.invest.domain.vo.InvestTradeAnalysisQueryCriteria;
import me.zhengjie.invest.mapper.InvestTradeAnalysisMapper;
import me.zhengjie.utils.AliyunOcrUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import me.zhengjie.utils.PageUtil;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import me.zhengjie.utils.PageResult;

/**
* @description 服务实现
* @author genghui
* @date 2026-06-07
**/
@Service
@RequiredArgsConstructor
public class InvestTradeAnalysisServiceImpl extends ServiceImpl<InvestTradeAnalysisMapper, InvestTradeAnalysis> implements InvestTradeAnalysisService {

    private final InvestTradeAnalysisMapper investTradeAnalysisMapper;
    private final AliyunOcrUtil aliyunOcrUtil;

    @Override
    public PageResult<InvestTradeAnalysis> queryAll(InvestTradeAnalysisQueryCriteria criteria, Page<Object> page){
        return PageUtil.toPage(investTradeAnalysisMapper.findAll(criteria, page));
    }

    @Override
    public List<InvestTradeAnalysis> queryAll(InvestTradeAnalysisQueryCriteria criteria){
        return investTradeAnalysisMapper.findAll(criteria);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(InvestTradeAnalysis resources) {
        save(resources);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(InvestTradeAnalysis resources) {
        InvestTradeAnalysis investTradeAnalysis = getById(resources.getId());
        investTradeAnalysis.copy(resources);
        saveOrUpdate(investTradeAnalysis);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAll(List<Long> ids) {
        removeBatchByIds(ids);
    }

    @Override
    public InvestTradeAnalysisOcrVO ocr(MultipartFile file) throws IOException {
        File tempFile = File.createTempFile("trade-analysis-ocr-", ".tmp");
        try {
            file.transferTo(tempFile);
            return parseOrderOcr(aliyunOcrUtil.recognizeGeneral(tempFile), tempFile);
        } finally {
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    private InvestTradeAnalysisOcrVO parseOrderOcr(String ocrData, File imageFile) {
        InvestTradeAnalysisOcrVO result = new InvestTradeAnalysisOcrVO();
        if (ocrData == null || ocrData.trim().isEmpty()) {
            return result;
        }
        JSONObject raw = JSON.parseObject(ocrData);
        String content = normalizeOcrText(raw.getString("content"));
        List<String> words = getOcrWords(raw);

        result.setDirection(detectDirection(imageFile, content));
        result.setAmount(extractAmount(words, content));
        result.setOpenPrice(firstNotBlank(extractOpenPrice(words), extractNumberAfterLabel(content, "Open Price", true)));
        result.setOpenTime(formatOcrTime(firstNotBlank(extractOpenTime(words), extractTimeAfterLabel(content, "Open Time"))));
        result.setClosePrice(firstNotBlank(extractClosePrice(words), extractNumberAfterLabel(content, "Close Price", true)));
        result.setCloseTime(formatOcrTime(firstNotBlank(extractCloseTime(words), extractTimeAfterLabel(content, "Close Time"))));
        result.setNetProfit(extractNetProfit(content));
        return result;
    }

    private List<String> getOcrWords(JSONObject raw) {
        List<String> words = new ArrayList<>();
        JSONArray wordsInfo = raw.getJSONArray("prism_wordsInfo");
        if (wordsInfo == null) {
            return words;
        }
        for (int i = 0; i < wordsInfo.size(); i++) {
            JSONObject item = wordsInfo.getJSONObject(i);
            if (item != null && item.getString("word") != null) {
                words.add(normalizeOcrText(item.getString("word")));
            }
        }
        return words;
    }

    private String normalizeOcrText(String text) {
        return text == null ? "" : text.replace('：', ':').replaceAll("\\s+", " ").trim();
    }

    private String detectDirection(File imageFile, String content) {
        if (content.matches("(?i).*(看跌|下跌|做空|short|sell).*")) {
            return "看跌";
        }
        if (content.matches("(?i).*(看涨|上涨|做多|long|buy).*")) {
            return "看涨";
        }
        return detectDirectionByIconColor(imageFile);
    }

    private String detectDirectionByIconColor(File imageFile) {
        try {
            BufferedImage image = ImageIO.read(imageFile);
            if (image == null) {
                return null;
            }
            int maxX = Math.max(1, image.getWidth() / 4);
            int maxY = Math.max(1, image.getHeight() / 3);
            int redPixels = 0;
            int greenPixels = 0;
            for (int y = 0; y < maxY; y++) {
                for (int x = 0; x < maxX; x++) {
                    int rgb = image.getRGB(x, y);
                    int r = (rgb >> 16) & 0xff;
                    int g = (rgb >> 8) & 0xff;
                    int b = rgb & 0xff;
                    if (r > 180 && g < 120 && b < 160) {
                        redPixels++;
                    } else if (g > 150 && r < 140 && b < 180) {
                        greenPixels++;
                    }
                }
            }
            if (redPixels > greenPixels && redPixels > 20) {
                return "看跌";
            }
            if (greenPixels > redPixels && greenPixels > 20) {
                return "看涨";
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String extractAmount(List<String> words, String content) {
        int start = indexOfWord(words, "数量");
        if (start == -1) {
            start = indexOfWordIgnoreCase(words, "Amount");
        }
        if (start != -1) {
            for (int i = start + 1; i < words.size(); i++) {
                String number = match(words.get(i), "^\\d+(?:\\.\\d+)?$");
                if (number != null) {
                    return number;
                }
            }
        }
        return extractNumberAfterLabel(content, "Amount(USDT)", false);
    }

    private String extractOpenPrice(List<String> words) {
        return extractPriceFromValues(valuesAfterLabels(words, new String[]{"开仓价", "指数价格", "开仓时间"}, new String[]{"奖金支付率", "平仓价", "平仓时间"}));
    }

    private String extractOpenTime(List<String> words) {
        return extractTimeFromValues(valuesAfterLabels(words, new String[]{"开仓价", "指数价格", "开仓时间"}, new String[]{"奖金支付率", "平仓价", "平仓时间"}));
    }

    private String extractClosePrice(List<String> words) {
        if (indexOfWord(words, "平仓价") == -1) {
            return null;
        }
        return extractPriceFromValues(valuesAfterLabels(words, new String[]{"奖金支付率", "平仓价", "平仓时间"}, new String[]{}));
    }

    private String extractCloseTime(List<String> words) {
        if (indexOfWord(words, "平仓时间") == -1) {
            return null;
        }
        return extractTimeFromValues(valuesAfterLabels(words, new String[]{"奖金支付率", "平仓价", "平仓时间"}, new String[]{}));
    }

    private List<String> valuesAfterLabels(List<String> words, String[] labels, String[] stopLabels) {
        int start = -1;
        for (String label : labels) {
            int index = indexOfWord(words, label);
            if (index != -1) {
                start = Math.max(start, index);
            }
        }
        List<String> values = new ArrayList<>();
        if (start == -1) {
            return values;
        }
        for (int i = start + 1; i < words.size(); i++) {
            String word = words.get(i);
            boolean stop = false;
            for (String stopLabel : stopLabels) {
                if (word.contains(stopLabel)) {
                    stop = true;
                    break;
                }
            }
            if (stop) {
                break;
            }
            values.add(word);
        }
        return values;
    }

    private String extractPriceFromValues(List<String> values) {
        for (String value : values) {
            String number = match(value, "\\d+(?:\\.\\d+)?");
            if (number != null && Double.parseDouble(number) > 1000) {
                return number;
            }
        }
        return null;
    }

    private String extractTimeFromValues(List<String> values) {
        for (String value : values) {
            String time = extractTimeFromText(value);
            if (time != null) {
                return time;
            }
        }
        return null;
    }

    private String extractNumberAfterLabel(String text, String label, boolean preferDecimal) {
        int index = text.toLowerCase().indexOf(label.toLowerCase());
        if (index == -1) {
            return null;
        }
        String segment = text.substring(index + label.length(), Math.min(text.length(), index + label.length() + 48));
        Matcher matcher = Pattern.compile("[+-]?\\d+(?:\\.\\d+)?").matcher(segment);
        String first = null;
        while (matcher.find()) {
            String number = matcher.group();
            if (first == null) {
                first = number;
            }
            if (preferDecimal && number.contains(".")) {
                return number;
            }
        }
        return first;
    }

    private String extractTimeAfterLabel(String text, String label) {
        int index = text.toLowerCase().indexOf(label.toLowerCase());
        if (index == -1) {
            return null;
        }
        String segment = text.substring(index + label.length(), Math.min(text.length(), index + label.length() + 32));
        return extractTimeFromText(segment);
    }

    private String extractTimeFromText(String text) {
        return match(normalizeOcrText(text), "(\\d{2}-\\d{2})\\s*(\\d{2}:\\d{2}:\\d{2})", "$1 $2");
    }

    private String extractNetProfit(String text) {
        return match(text, "([+-]\\d+(?:\\.\\d+)?)\\s*USDT", "$1");
    }

    private String formatOcrTime(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        if (value.matches("^\\d{4}-.*")) {
            return value;
        }
        return java.time.LocalDate.now().getYear() + "-" + value;
    }

    private int indexOfWord(List<String> words, String label) {
        for (int i = 0; i < words.size(); i++) {
            if (words.get(i).contains(label)) {
                return i;
            }
        }
        return -1;
    }

    private int indexOfWordIgnoreCase(List<String> words, String label) {
        for (int i = 0; i < words.size(); i++) {
            if (words.get(i).toLowerCase().contains(label.toLowerCase())) {
                return i;
            }
        }
        return -1;
    }

    private String match(String text, String regex) {
        return match(text, regex, null);
    }

    private String match(String text, String regex, String replacement) {
        Matcher matcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(text);
        if (!matcher.find()) {
            return null;
        }
        if (replacement == null) {
            return matcher.group();
        }
        String result = replacement;
        for (int i = 1; i <= matcher.groupCount(); i++) {
            result = result.replace("$" + i, matcher.group(i));
        }
        return result;
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return null;
    }

    @Override
    public void download(List<InvestTradeAnalysis> all, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (InvestTradeAnalysis investTradeAnalysis : all) {
            Map<String,Object> map = new LinkedHashMap<>();
            map.put("方向", investTradeAnalysis.getDirection());
            map.put("数量(USDT)", investTradeAnalysis.getAmount());
            map.put("入场类型", investTradeAnalysis.getEntryType());
            map.put("开仓时间", investTradeAnalysis.getOpenTime());
            map.put("平仓时间", investTradeAnalysis.getCloseTime());
            map.put("开仓价", investTradeAnalysis.getOpenPrice());
            map.put("平仓价", investTradeAnalysis.getClosePrice());
            map.put("净盈亏", investTradeAnalysis.getNetProfit());
            map.put("开仓原因", investTradeAnalysis.getOpenReason());
            map.put("开仓K线图", investTradeAnalysis.getOpenKlineImages());
            map.put("平仓K线图", investTradeAnalysis.getCloseKlineImages());
            map.put("开仓评分", investTradeAnalysis.getScore());
            map.put("复盘结论", investTradeAnalysis.getReviewConclusion());
            map.put("交易质量", investTradeAnalysis.getQualityLevel());
            map.put("备注", investTradeAnalysis.getRemark());
            map.put("创建者", investTradeAnalysis.getCreateBy());
            map.put("更新者", investTradeAnalysis.getUpdateBy());
            map.put("创建时间", investTradeAnalysis.getCreateTime());
            map.put("更新时间", investTradeAnalysis.getUpdateTime());
            list.add(map);
        }
        FileUtil.downloadExcel(list, response);
    }
}
