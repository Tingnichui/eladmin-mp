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

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import me.zhengjie.invest.constants.BinanceEnum;
import me.zhengjie.invest.domain.InvestKlinesRecord;
import me.zhengjie.invest.util.BinanceUtil;
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

import java.util.*;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;

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
    private final BinanceUtil binanceUtil;

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

    @Override
    public void syncKlinesRecord(BinanceEnum.SYMBOL symbol, BinanceEnum.KLINES_INTERVAL interval, long defaultStartTime) {
        // 获取数据库中最新的K线
        InvestKlinesRecord lastOneInDb = this.getOne(
                Wrappers.lambdaQuery(InvestKlinesRecord.class)
                        .eq(InvestKlinesRecord::getSymbol, symbol)
                        .eq(InvestKlinesRecord::getPeriod, interval.getPeriod())
                        .orderByDesc(InvestKlinesRecord::getOpenTime)
                        .last("limit 1")
        );

        // 查询K线时间范围 库中有数据就按照库中数据
        long startTime = defaultStartTime;
        if (null != lastOneInDb) {
            startTime = lastOneInDb.getOpenTime();
            // 因为不确定当前库中最新K线是否已经收盘，所以删除之后在查询
            investKlinesRecordMapper.deleteById(lastOneInDb.getId());
        }

        long now = System.currentTimeMillis();
        while (now > startTime) {
            long endTime = DateUtil.offsetDay(new Date(startTime), 10).getTime() - 1;
            List<InvestKlinesRecord> klines = binanceUtil.getKlines(symbol, interval, startTime, endTime);
            this.saveBatch(klines);
            startTime = endTime;
        }

    }


}