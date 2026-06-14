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

import me.zhengjie.invest.domain.InvestTradeRecord;
import me.zhengjie.utils.FileUtil;
import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import me.zhengjie.invest.service.InvestTradeRecordService;
import me.zhengjie.invest.domain.dto.InvestTradeRecordQueryCriteria;
import me.zhengjie.invest.mapper.InvestTradeRecordMapper;
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
* @date 2025-01-04
**/
@Service
@RequiredArgsConstructor
public class InvestTradeRecordServiceImpl extends ServiceImpl<InvestTradeRecordMapper, InvestTradeRecord> implements InvestTradeRecordService {

    private final InvestTradeRecordMapper investTradeRecordMapper;

    @Override
    public PageResult<InvestTradeRecord> queryAll(InvestTradeRecordQueryCriteria criteria, Page<Object> page){
        return PageUtil.toPage(investTradeRecordMapper.findAll(criteria, page));
    }

    @Override
    public List<InvestTradeRecord> queryAll(InvestTradeRecordQueryCriteria criteria){
        return investTradeRecordMapper.findAll(criteria);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(InvestTradeRecord resources) {
        save(resources);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(InvestTradeRecord resources) {
        InvestTradeRecord investTradeRecord = getById(resources.getId());
        investTradeRecord.copy(resources);
        saveOrUpdate(investTradeRecord);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAll(List<Integer> ids) {
        removeBatchByIds(ids);
    }

    @Override
    public void download(List<InvestTradeRecord> all, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (InvestTradeRecord investTradeRecord : all) {
            Map<String,Object> map = new LinkedHashMap<>();
            map.put("投资产品", investTradeRecord.getProductId());
            map.put("交易类型", investTradeRecord.getTradeType());
            map.put("交易数量", investTradeRecord.getTradeNum());
            map.put("开仓价格", investTradeRecord.getOpenPrice());
            map.put("杠杆", investTradeRecord.getLeverage());
            map.put("开仓成本", investTradeRecord.getCost());
            map.put("止损价格", investTradeRecord.getStopLoss());
            map.put("止盈价格", investTradeRecord.getTakeProfit());
            map.put("平仓价格", investTradeRecord.getClosePrice());
            map.put("交易状态", investTradeRecord.getOperateStatus());
            map.put("创建时间", investTradeRecord.getCreateTime());
            map.put("更新人", investTradeRecord.getUpdateBy());
            map.put("更新时间", investTradeRecord.getUpdateTime());
            map.put("创建人", investTradeRecord.getCreateBy());
            list.add(map);
        }
        FileUtil.downloadExcel(list, response);
    }
}