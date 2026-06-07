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

import me.zhengjie.invest.domain.InvestTradeAnalysis;
import me.zhengjie.utils.FileUtil;
import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import me.zhengjie.invest.service.InvestTradeAnalysisService;
import me.zhengjie.invest.domain.vo.InvestTradeAnalysisQueryCriteria;
import me.zhengjie.invest.mapper.InvestTradeAnalysisMapper;
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
* @date 2026-06-07
**/
@Service
@RequiredArgsConstructor
public class InvestTradeAnalysisServiceImpl extends ServiceImpl<InvestTradeAnalysisMapper, InvestTradeAnalysis> implements InvestTradeAnalysisService {

    private final InvestTradeAnalysisMapper investTradeAnalysisMapper;

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