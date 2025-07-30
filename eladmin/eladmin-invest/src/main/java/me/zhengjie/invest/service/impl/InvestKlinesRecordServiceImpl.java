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

import me.zhengjie.invest.domain.InvestKlinesRecord;
import me.zhengjie.utils.FileUtil;
import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import me.zhengjie.invest.service.InvestKlinesRecordService;
import me.zhengjie.invest.domain.vo.InvestKlinesRecordQueryCriteria;
import me.zhengjie.invest.mapper.InvestKlinesRecordMapper;
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
* @date 2025-07-30
**/
@Service
@RequiredArgsConstructor
public class InvestKlinesRecordServiceImpl extends ServiceImpl<InvestKlinesRecordMapper, InvestKlinesRecord> implements InvestKlinesRecordService {

    private final InvestKlinesRecordMapper investKlinesRecordMapper;

    @Override
    public PageResult<InvestKlinesRecord> queryAll(InvestKlinesRecordQueryCriteria criteria, Page<Object> page){
        return PageUtil.toPage(investKlinesRecordMapper.findAll(criteria, page));
    }

    @Override
    public List<InvestKlinesRecord> queryAll(InvestKlinesRecordQueryCriteria criteria){
        return investKlinesRecordMapper.findAll(criteria);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(InvestKlinesRecord resources) {
        save(resources);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(InvestKlinesRecord resources) {
        InvestKlinesRecord investKlinesRecord = getById(resources.getId());
        investKlinesRecord.copy(resources);
        saveOrUpdate(investKlinesRecord);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAll(List<Long> ids) {
        removeBatchByIds(ids);
    }

    @Override
    public void download(List<InvestKlinesRecord> all, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (InvestKlinesRecord investKlinesRecord : all) {
            Map<String,Object> map = new LinkedHashMap<>();
            map.put("交易对", investKlinesRecord.getSymbol());
            map.put("周期，单位分钟", investKlinesRecord.getPeriod());
            map.put("开盘时间", investKlinesRecord.getOpenTime());
            map.put("收盘时间", investKlinesRecord.getCloseTime());
            map.put("开盘价", investKlinesRecord.getOpenPrice());
            map.put("收盘价", investKlinesRecord.getClosePrice());
            map.put("最高价", investKlinesRecord.getHighPrice());
            map.put("最低价", investKlinesRecord.getLowPrice());
            map.put("成交量", investKlinesRecord.getVolume());
            map.put("成交额", investKlinesRecord.getTurnover());
            map.put("成交笔数", investKlinesRecord.getTradeCount());
            map.put("主动买入成交量", investKlinesRecord.getBuyVolume());
            map.put("主动买入成交额", investKlinesRecord.getBuyTurnover());
            list.add(map);
        }
        FileUtil.downloadExcel(list, response);
    }
}