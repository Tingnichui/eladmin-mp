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

import com.baomidou.dynamic.datasource.annotation.DS;
import me.zhengjie.invest.domain.ResearchStrategyAlertEvents;
import me.zhengjie.utils.FileUtil;
import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import me.zhengjie.invest.service.ResearchStrategyAlertEventsService;
import me.zhengjie.invest.domain.dto.ResearchStrategyAlertEventsQueryCriteria;
import me.zhengjie.invest.mapper.ResearchStrategyAlertEventsMapper;
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
* @date 2026-09-29
**/
@DS("investment_strategy")
@Service
@RequiredArgsConstructor
public class ResearchStrategyAlertEventsServiceImpl extends ServiceImpl<ResearchStrategyAlertEventsMapper, ResearchStrategyAlertEvents> implements ResearchStrategyAlertEventsService {

    private final ResearchStrategyAlertEventsMapper researchStrategyAlertEventsMapper;

    @Override
    public PageResult<ResearchStrategyAlertEvents> queryAll(ResearchStrategyAlertEventsQueryCriteria criteria, Page<Object> page){
        return PageUtil.toPage(researchStrategyAlertEventsMapper.findAll(criteria, page));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(ResearchStrategyAlertEvents resources) {
        researchStrategyAlertEventsMapper.insert(resources);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(ResearchStrategyAlertEvents resources) {
        ResearchStrategyAlertEvents researchStrategyAlertEvents = getById(resources.getId());
        researchStrategyAlertEvents.copy(resources);
        researchStrategyAlertEventsMapper.updateById(researchStrategyAlertEvents);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAll(List<Long> ids) {
        researchStrategyAlertEventsMapper.deleteBatchIds(ids);
    }

    @Override
    public void download(ResearchStrategyAlertEventsQueryCriteria criteria, HttpServletResponse response) throws IOException {
        List<String> headers = new ArrayList<>();
        headers.add("事件唯一键");
        headers.add("告警ID");
        headers.add("结构版本");
        headers.add("策略ID");
        headers.add("交易对");
        headers.add("周期");
        headers.add("策略版本");
        headers.add("参数指纹");
        headers.add("数据源指纹");
        headers.add("K线开盘时间");
        headers.add("决策时间");
        headers.add("操作");
        headers.add("操作前仓位");
        headers.add("操作后仓位");
        headers.add("操作价格");
        headers.add("收盘价格");
        headers.add("止损价格");
        headers.add("触发原因");
        headers.add("标题");
        headers.add("消息");
        headers.add("事件内容");
        headers.add("创建时间");
        FileUtil.downloadExcel(headers, response, writer -> {
            long current = 1L;
            final long pageSize = 10000L;
            while (true) {
                Page<Object> page = new Page<>(current, pageSize, false);
                List<ResearchStrategyAlertEvents> records = researchStrategyAlertEventsMapper.findAll(criteria, page).getRecords();
                if (records.isEmpty()) {
                    break;
                }
                List<Map<String, Object>> list = new ArrayList<>(records.size());
                for (ResearchStrategyAlertEvents researchStrategyAlertEvents : records) {
                    Map<String,Object> map = new LinkedHashMap<>();
                    map.put("事件唯一键", researchStrategyAlertEvents.getEventKey());
                    map.put("告警ID", getExportValue(researchStrategyAlertEvents.getAlertId()));
                    map.put("结构版本", researchStrategyAlertEvents.getSchemaVersion());
                    map.put("策略ID", researchStrategyAlertEvents.getStrategyId());
                    map.put("交易对", researchStrategyAlertEvents.getSymbol());
                    map.put("周期", researchStrategyAlertEvents.getIntervalCode());
                    map.put("策略版本", getExportValue(researchStrategyAlertEvents.getProfileRevision()));
                    map.put("参数指纹", researchStrategyAlertEvents.getParameterFingerprint());
                    map.put("数据源指纹", researchStrategyAlertEvents.getSourceFingerprint());
                    map.put("K线开盘时间", researchStrategyAlertEvents.getBarOpenTime());
                    map.put("决策时间", researchStrategyAlertEvents.getDecisionTime());
                    map.put("操作", researchStrategyAlertEvents.getAction());
                    map.put("操作前仓位", researchStrategyAlertEvents.getPositionBefore());
                    map.put("操作后仓位", researchStrategyAlertEvents.getPositionAfter());
                    map.put("操作价格", researchStrategyAlertEvents.getActionPrice());
                    map.put("收盘价格", researchStrategyAlertEvents.getClosePrice());
                    map.put("止损价格", researchStrategyAlertEvents.getStopLossPrice());
                    map.put("触发原因", researchStrategyAlertEvents.getReason());
                    map.put("标题", researchStrategyAlertEvents.getTitle());
                    map.put("消息", researchStrategyAlertEvents.getMessage());
                    map.put("事件内容", researchStrategyAlertEvents.getPayload());
                    map.put("创建时间", researchStrategyAlertEvents.getCreatedAt());
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
