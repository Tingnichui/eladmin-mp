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

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import me.zhengjie.invest.constants.BinanceEnum;
import me.zhengjie.invest.domain.BinanceAccountInfo;
import me.zhengjie.invest.domain.BinanceCoinFuturesTradeInfo;
import me.zhengjie.invest.domain.BinanceFuturesTradeInfo;
import me.zhengjie.invest.service.BinanceAccountInfoService;
import me.zhengjie.invest.util.BinanceAccountContextHolder;
import me.zhengjie.invest.util.BinanceCoinFuturesUtil;
import me.zhengjie.utils.FileUtil;
import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import me.zhengjie.invest.service.BinanceCoinFuturesTradeInfoService;
import me.zhengjie.invest.domain.vo.BinanceCoinFuturesTradeInfoQueryCriteria;
import me.zhengjie.invest.mapper.BinanceCoinFuturesTradeInfoMapper;
import me.zhengjie.utils.RedisUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import me.zhengjie.utils.PageUtil;

import java.math.BigDecimal;
import java.util.*;
import java.io.IOException;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletResponse;

import me.zhengjie.utils.PageResult;

/**
 * @author genghui
 * @description 服务实现
 * @date 2025-11-23
 **/
@Service
@RequiredArgsConstructor
public class BinanceCoinFuturesTradeInfoServiceImpl extends ServiceImpl<BinanceCoinFuturesTradeInfoMapper, BinanceCoinFuturesTradeInfo> implements BinanceCoinFuturesTradeInfoService {

    private final BinanceCoinFuturesTradeInfoMapper binanceCoinFuturesTradeInfoMapper;
    private final BinanceAccountInfoService binanceAccountInfoService;
    private final BinanceCoinFuturesUtil binanceCoinFuturesUtil;
    private final RedisUtils redisUtils;

    @Override
    public PageResult<BinanceCoinFuturesTradeInfo> queryAll(BinanceCoinFuturesTradeInfoQueryCriteria criteria, Page<Object> page) {
        return PageUtil.toPage(binanceCoinFuturesTradeInfoMapper.findAll(criteria, page));
    }

    @Override
    public List<BinanceCoinFuturesTradeInfo> queryAll(BinanceCoinFuturesTradeInfoQueryCriteria criteria) {
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
            Map<String, Object> map = new LinkedHashMap<>();
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

    @Override
    public void sync() {
        // 查询所有账号
        List<BinanceAccountInfo> accountInfoList = binanceAccountInfoService.listUseApiAccount();
        for (BinanceAccountInfo accountInfo : accountInfoList) {
            BinanceAccountContextHolder.runWith(accountInfo, () -> {
                List<BinanceCoinFuturesTradeInfo> orderInfoList = binanceCoinFuturesUtil.userTrades(BinanceEnum.SYMBOL.BTCUSD_PERP, null, null);
                if (CollectionUtils.isEmpty(orderInfoList)) {
                    return;
                }

                // 查询已经在库中的订单
                Set<Long> existIdSet = this.list(
                        Wrappers.lambdaQuery(BinanceCoinFuturesTradeInfo.class)
                                .select(BinanceCoinFuturesTradeInfo::getId)
                                .in(BinanceCoinFuturesTradeInfo::getId, orderInfoList.stream().map(BinanceCoinFuturesTradeInfo::getId).collect(Collectors.toSet()))
                ).stream().map(BinanceCoinFuturesTradeInfo::getId).collect(Collectors.toSet());

                // 过滤掉已存在的订单
                List<BinanceCoinFuturesTradeInfo> newOrders = orderInfoList.stream()
                        .filter(order -> !existIdSet.contains(order.getId()))
                        .peek(order -> order.setUid(accountInfo.getUid()))
                        .collect(Collectors.toList());

                // 保存新订单
                this.saveOrUpdateBatch(newOrders);

            });
        }
    }

    @Override
    public Date getLastPosCloseTime() {
        final String key = "BINANCE:COIN_FUTURES:LAST_POS_CLOSE_TIME";
        Date lastPosCloseTime = (Date) redisUtils.get(key);
        List<BinanceCoinFuturesTradeInfo> binanceFuturesTradeInfoList = baseMapper.selectList(
                Wrappers.lambdaQuery(BinanceCoinFuturesTradeInfo.class)
                        .gt(null != lastPosCloseTime, BinanceCoinFuturesTradeInfo::getTime, lastPosCloseTime)
                        .orderByAsc(BinanceCoinFuturesTradeInfo::getTime)
        );

        BigDecimal pos = BigDecimal.ZERO;
        for (BinanceCoinFuturesTradeInfo tradeInfo : binanceFuturesTradeInfoList) {
            // 卖开仓 买减仓
            if (tradeInfo.getBuyer().equals(1)) {
                pos = pos.subtract(tradeInfo.getQty());
            } else {
                pos = pos.add(tradeInfo.getQty());
            }
            if (pos.compareTo(BigDecimal.ZERO) == 0) {
                redisUtils.set(key, tradeInfo.getTime());
            }
        }

        return (Date) redisUtils.get(key);
    }

}