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

import me.zhengjie.invest.domain.BinanceCoinFuturesTradeInfo;
import me.zhengjie.utils.FileUtil;
import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import me.zhengjie.invest.service.BinanceCoinFuturesTradeInfoService;
import me.zhengjie.invest.domain.vo.BinanceCoinFuturesTradeInfoQueryCriteria;
import me.zhengjie.invest.mapper.BinanceCoinFuturesTradeInfoMapper;
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
* @date 2025-11-23
**/
@Service
@RequiredArgsConstructor
public class BinanceCoinFuturesTradeInfoServiceImpl extends ServiceImpl<BinanceCoinFuturesTradeInfoMapper, BinanceCoinFuturesTradeInfo> implements BinanceCoinFuturesTradeInfoService {

    private final BinanceCoinFuturesTradeInfoMapper binanceCoinFuturesTradeInfoMapper;

    @Override
    public PageResult<BinanceCoinFuturesTradeInfo> queryAll(BinanceCoinFuturesTradeInfoQueryCriteria criteria, Page<Object> page){
        return PageUtil.toPage(binanceCoinFuturesTradeInfoMapper.findAll(criteria, page));
    }

    @Override
    public List<BinanceCoinFuturesTradeInfo> queryAll(BinanceCoinFuturesTradeInfoQueryCriteria criteria){
        return binanceCoinFuturesTradeInfoMapper.findAll(criteria);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(BinanceCoinFuturesTradeInfo resources) {
        save(resources);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(BinanceCoinFuturesTradeInfo resources) {
        BinanceCoinFuturesTradeInfo binanceCoinFuturesTradeInfo = getById(resources.getId());
        binanceCoinFuturesTradeInfo.copy(resources);
        saveOrUpdate(binanceCoinFuturesTradeInfo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAll(List<Long> ids) {
        removeBatchByIds(ids);
    }

    @Override
    public void download(List<BinanceCoinFuturesTradeInfo> all, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (BinanceCoinFuturesTradeInfo binanceCoinFuturesTradeInfo : all) {
            Map<String,Object> map = new LinkedHashMap<>();
            map.put("用户编号", binanceCoinFuturesTradeInfo.getUid());
            map.put("交易对", binanceCoinFuturesTradeInfo.getSymbol());
            map.put("订单 ID", binanceCoinFuturesTradeInfo.getOrderId());
            map.put("成交价格", binanceCoinFuturesTradeInfo.getPrice());
            map.put("成交数量", binanceCoinFuturesTradeInfo.getQty());
            map.put("成交额", binanceCoinFuturesTradeInfo.getBaseQty());
            map.put("手续费", binanceCoinFuturesTradeInfo.getCommission());
            map.put("手续费计价单位", binanceCoinFuturesTradeInfo.getCommissionAsset());
            map.put("成交时间", binanceCoinFuturesTradeInfo.getTime());
            map.put("是否为买方", binanceCoinFuturesTradeInfo.getBuyer());
            map.put("是否为挂单方", binanceCoinFuturesTradeInfo.getMaker());
            map.put("实现盈亏", binanceCoinFuturesTradeInfo.getRealizedPnl());
            map.put("买卖方向", binanceCoinFuturesTradeInfo.getSide());
            map.put("持仓方向", binanceCoinFuturesTradeInfo.getPositionSide());
            map.put("标的交易对", binanceCoinFuturesTradeInfo.getPair());
            map.put("保证金币种", binanceCoinFuturesTradeInfo.getMarginAsset());
            list.add(map);
        }
        FileUtil.downloadExcel(list, response);
    }
}