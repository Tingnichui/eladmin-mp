/*
*  Copyright 2019-2025 Zheng Jie
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

import me.zhengjie.invest.domain.BinanceSpotTradeMatch;
import me.zhengjie.utils.FileUtil;
import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import me.zhengjie.invest.service.BinanceSpotTradeMatchService;
import me.zhengjie.invest.domain.dto.BinanceSpotTradeMatchQueryCriteria;
import me.zhengjie.invest.mapper.BinanceSpotTradeMatchMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import me.zhengjie.utils.PageUtil;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import me.zhengjie.utils.PageResult;

/**
* @description 服务实现
* @author genghui
* @date 2026-09-19
**/
@Service
@RequiredArgsConstructor
public class BinanceSpotTradeMatchServiceImpl extends ServiceImpl<BinanceSpotTradeMatchMapper, BinanceSpotTradeMatch> implements BinanceSpotTradeMatchService {

    private final BinanceSpotTradeMatchMapper binanceSpotTradeMatchMapper;

    @Override
    public PageResult<BinanceSpotTradeMatch> queryAll(BinanceSpotTradeMatchQueryCriteria criteria, Page<Object> page){
        return PageUtil.toPage(binanceSpotTradeMatchMapper.findAll(criteria, page));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(BinanceSpotTradeMatch resources) {
        binanceSpotTradeMatchMapper.insert(resources);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(BinanceSpotTradeMatch resources) {
        BinanceSpotTradeMatch binanceSpotTradeMatch = getById(resources.getId());
        binanceSpotTradeMatch.copy(resources);
        binanceSpotTradeMatchMapper.updateById(binanceSpotTradeMatch);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAll(List<Long> ids) {
        binanceSpotTradeMatchMapper.deleteBatchIds(ids);
    }

    @Override
    public void download(BinanceSpotTradeMatchQueryCriteria criteria, HttpServletResponse response) throws IOException {
        List<String> headers = new ArrayList<>();
        headers.add("币安账户用户编号");
        headers.add("现货交易对");
        headers.add("买入成交 ID");
        headers.add("卖出成交 ID");
        headers.add("撮合数量");
        headers.add("买入价格快照");
        headers.add("卖出价格快照");
        headers.add("买入成交时间");
        headers.add("卖出成交时间");
        headers.add("撮合买入金额");
        headers.add("撮合卖出金额");
        headers.add("手续费率");
        headers.add("已实现盈亏");
        headers.add("手续费");
        headers.add("扣除手续费后的净盈亏");
        headers.add("创建时间");
        FileUtil.downloadExcel(headers, response, writer -> {
            long current = 1L;
            final long pageSize = 10000L;
            while (true) {
                Page<Object> page = new Page<>(current, pageSize, false);
                List<BinanceSpotTradeMatch> records = binanceSpotTradeMatchMapper.findAll(criteria, page).getRecords();
                if (records.isEmpty()) {
                    break;
                }
                List<Map<String, Object>> list = new ArrayList<>(records.size());
                for (BinanceSpotTradeMatch binanceSpotTradeMatch : records) {
                    Map<String,Object> map = new LinkedHashMap<>();
                    map.put("币安账户用户编号", binanceSpotTradeMatch.getUid());
                    map.put("现货交易对", binanceSpotTradeMatch.getSymbol());
                    map.put("买入成交 ID", getExportValue(binanceSpotTradeMatch.getBuyTradeId()));
                    map.put("卖出成交 ID", getExportValue(binanceSpotTradeMatch.getSellTradeId()));
                    map.put("撮合数量", binanceSpotTradeMatch.getMatchedQty());
                    map.put("买入价格快照", binanceSpotTradeMatch.getBuyPrice());
                    map.put("卖出价格快照", binanceSpotTradeMatch.getSellPrice());
                    map.put("买入成交时间", binanceSpotTradeMatch.getBuyTime());
                    map.put("卖出成交时间", binanceSpotTradeMatch.getSellTime());
                    map.put("撮合买入金额", binanceSpotTradeMatch.getBuyAmount());
                    map.put("撮合卖出金额", binanceSpotTradeMatch.getSellAmount());
                    map.put("手续费率", binanceSpotTradeMatch.getFeeRate());
                    map.put("已实现盈亏", binanceSpotTradeMatch.getPnl());
                    map.put("手续费", binanceSpotTradeMatch.getFee());
                    map.put("扣除手续费后的净盈亏", binanceSpotTradeMatch.getNetPnl());
                    map.put("创建时间", binanceSpotTradeMatch.getCreateTime());
                    list.add(map);
                }
                writer.write(list);
                if (records.size() < pageSize) {
                    break;
                }
                current++;
            }
        });
    }

    private Object getExportValue(Object value) {
        return value instanceof Long ? String.valueOf(value) : value;
    }
}
