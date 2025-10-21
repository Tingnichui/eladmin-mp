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

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.NumberUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import me.zhengjie.invest.constants.BinanceEnum;
import me.zhengjie.invest.domain.BinanceAccountInfo;
import me.zhengjie.invest.domain.BinanceFuturesTradeInfo;
import me.zhengjie.invest.domain.BinanceTradeInfo;
import me.zhengjie.invest.domain.BinanceTradeInfoExt;
import me.zhengjie.invest.domain.dto.MatchedTradeInfo;
import me.zhengjie.invest.domain.vo.BinanceFuturesTradeStatsInfoVO;
import me.zhengjie.invest.domain.vo.BinanceTradeInfoQueryCriteria;
import me.zhengjie.invest.service.BinanceAccountInfoService;
import me.zhengjie.invest.service.BinanceTradeInfoExtService;
import me.zhengjie.invest.service.BinanceTradeInfoService;
import me.zhengjie.invest.util.BinanceAccountContextHolder;
import me.zhengjie.invest.util.BinanceSpotUtil;
import me.zhengjie.invest.util.BinanceUsdFuturesUtil;
import me.zhengjie.utils.FileUtil;
import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import me.zhengjie.invest.service.BinanceFuturesTradeInfoService;
import me.zhengjie.invest.domain.vo.BinanceFuturesTradeInfoQueryCriteria;
import me.zhengjie.invest.mapper.BinanceFuturesTradeInfoMapper;
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
* @description 服务实现
* @author genghui
* @date 2025-10-17
**/
@Service
@RequiredArgsConstructor
public class BinanceFuturesTradeInfoServiceImpl extends ServiceImpl<BinanceFuturesTradeInfoMapper, BinanceFuturesTradeInfo> implements BinanceFuturesTradeInfoService {

    private final BinanceFuturesTradeInfoMapper binanceFuturesTradeInfoMapper;
    private final BinanceUsdFuturesUtil binanceUsdFuturesUtil;
    private final BinanceSpotUtil binanceSpotUtil;
    private final BinanceAccountInfoService binanceAccountInfoService;
    private final RedisUtils redisUtils;
    private final BinanceTradeInfoExtService binanceTradeInfoExtService;
    private final BinanceTradeInfoService binanceTradeInfoService;

    @Override
    public PageResult<BinanceFuturesTradeInfo> queryAll(BinanceFuturesTradeInfoQueryCriteria criteria, Page<Object> page){
        return PageUtil.toPage(binanceFuturesTradeInfoMapper.findAll(criteria, page));
    }

    @Override
    public List<BinanceFuturesTradeInfo> queryAll(BinanceFuturesTradeInfoQueryCriteria criteria){
        return binanceFuturesTradeInfoMapper.findAll(criteria);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(BinanceFuturesTradeInfo resources) {
        save(resources);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(BinanceFuturesTradeInfo resources) {
        BinanceFuturesTradeInfo binanceFuturesTradeInfo = getById(resources.getId());
        binanceFuturesTradeInfo.copy(resources);
        saveOrUpdate(binanceFuturesTradeInfo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAll(List<Long> ids) {
        removeBatchByIds(ids);
    }

    @Override
    public void download(List<BinanceFuturesTradeInfo> all, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (BinanceFuturesTradeInfo binanceFuturesTradeInfo : all) {
            Map<String,Object> map = new LinkedHashMap<>();
            map.put("用户编号", binanceFuturesTradeInfo.getUid());
            map.put("交易对", binanceFuturesTradeInfo.getSymbol());
            map.put("订单 ID", binanceFuturesTradeInfo.getOrderId());
            map.put("成交价格", binanceFuturesTradeInfo.getPrice());
            map.put("成交数量", binanceFuturesTradeInfo.getQty());
            map.put("成交额", binanceFuturesTradeInfo.getQuoteQty());
            map.put("手续费", binanceFuturesTradeInfo.getCommission());
            map.put("手续费计价单位", binanceFuturesTradeInfo.getCommissionAsset());
            map.put("成交时间", binanceFuturesTradeInfo.getTime());
            map.put("是否为买方", binanceFuturesTradeInfo.getBuyer());
            map.put("是否为挂单方", binanceFuturesTradeInfo.getMaker());
            map.put("实现盈亏", binanceFuturesTradeInfo.getRealizedPnl());
            map.put("买卖方向", binanceFuturesTradeInfo.getSide());
            map.put("持仓方向", binanceFuturesTradeInfo.getPositionSide());
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
                List<BinanceFuturesTradeInfo> orderInfoList = binanceUsdFuturesUtil.userTrades(BinanceEnum.SYMBOL.BTCUSDT, null, null);
                if (CollectionUtils.isEmpty(orderInfoList)) {
                    return;
                }

                // 查询已经在库中的订单
                Set<Long> existOrderIdSet = this.list(
                        Wrappers.lambdaQuery(BinanceFuturesTradeInfo.class)
                                .select(BinanceFuturesTradeInfo::getOrderId)
                                .in(BinanceFuturesTradeInfo::getOrderId, orderInfoList.stream().map(BinanceFuturesTradeInfo::getOrderId).collect(Collectors.toSet()))
                ).stream().map(BinanceFuturesTradeInfo::getOrderId).collect(Collectors.toSet());

                // 过滤掉已存在的订单
                List<BinanceFuturesTradeInfo> newOrders = orderInfoList.stream()
                        .filter(order -> !existOrderIdSet.contains(order.getOrderId()))
                        .peek(order -> order.setUid(accountInfo.getUid()))
                        .collect(Collectors.toList());

                // 保存新订单
                this.saveOrUpdateBatch(newOrders);

            });
        }
    }

    @Override
    public Date getLastPosCloseTime() {
        final String key = "BINANCE:FUTURES:LAST_POS_CLOSE_TIME";
        Date lastPosCloseTime = (Date) redisUtils.get(key);
        List<BinanceFuturesTradeInfo> binanceFuturesTradeInfoList = binanceFuturesTradeInfoMapper.selectList(
                Wrappers.lambdaQuery(BinanceFuturesTradeInfo.class)
                        .gt(null != lastPosCloseTime, BinanceFuturesTradeInfo::getTime, lastPosCloseTime)
                        .orderByAsc(BinanceFuturesTradeInfo::getTime)
        );

        BigDecimal pos = BigDecimal.ZERO;
        for (BinanceFuturesTradeInfo tradeInfo : binanceFuturesTradeInfoList) {
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

    @Override
    public BinanceFuturesTradeStatsInfoVO syncFuturesHedge() {
        BinanceFuturesTradeStatsInfoVO statsInfoVO = new BinanceFuturesTradeStatsInfoVO();

        // 当前仓位
        Date lastPosCloseTime = this.getLastPosCloseTime();
        List<BinanceFuturesTradeInfo> sellTradeInfoList = binanceFuturesTradeInfoMapper.selectList(
                Wrappers.lambdaQuery(BinanceFuturesTradeInfo.class)
                        .gt(BinanceFuturesTradeInfo::getTime, lastPosCloseTime)
                        .eq(BinanceFuturesTradeInfo::getBuyer, 0)
                        .orderByAsc(BinanceFuturesTradeInfo::getTime)
        );

        List<BinanceFuturesTradeInfo> buyTradeInfoList = binanceFuturesTradeInfoMapper.selectList(
                Wrappers.lambdaQuery(BinanceFuturesTradeInfo.class)
                        .gt(BinanceFuturesTradeInfo::getTime, lastPosCloseTime)
                        .eq(BinanceFuturesTradeInfo::getBuyer, 1)
                        .orderByAsc(BinanceFuturesTradeInfo::getTime)
        );

        // 获取当前合约价格
        BigDecimal currentPrice = binanceUsdFuturesUtil.price(BinanceEnum.SYMBOL.BTCUSDT);


        List<MatchedTradeInfo> matchedList = new ArrayList<>();
        Iterator<BinanceFuturesTradeInfo> sellIterator = sellTradeInfoList.iterator();
        while (sellIterator.hasNext()) {
            BinanceFuturesTradeInfo sell = sellIterator.next();

            // 遍历平仓交易
            Iterator<BinanceFuturesTradeInfo> buyItrator = buyTradeInfoList.iterator();
            while (buyItrator.hasNext()) {
                BinanceFuturesTradeInfo buy = buyItrator.next();
                // 移除平仓完毕的
                if (buy.getQty().compareTo(BigDecimal.ZERO) <= 0) {
                    buyItrator.remove();
                    continue;
                }
                // 忽略 平仓交易大于开仓交易的，及亏损单，亏损单由现货止损
                if (buy.getPrice().compareTo(sell.getPrice()) > 0) {
                    continue;
                }
                // 更新交易数量
                BigDecimal matchQty = sell.getQty().min(buy.getQty());
                buy.setQty(buy.getQty().subtract(matchQty));
                sell.setQty(sell.getQty().subtract(matchQty));

                // 撮合交易记录
                MatchedTradeInfo matched = new MatchedTradeInfo();
                matched.setQty(matchQty);
                matched.setBuyPrice(buy.getPrice());
                matched.setSellPrice(sell.getPrice());
                matched.computeDerivedFields();
                matchedList.add(matched);

                // 移除平仓完毕的做空单
                if (sell.getQty().compareTo(BigDecimal.ZERO) <= 0) {
                    sellIterator.remove();
                    break;
                }
            }

        }

        // 计算盈利
        statsInfoVO.setProfit(matchedList.stream().map(MatchedTradeInfo::getProfit).reduce(BigDecimal.ZERO, BigDecimal::add));
        // 计算手续费
        BigDecimal feeRate = new BigDecimal("0.0005");
        statsInfoVO.setFee(matchedList.stream().map(v -> (v.getBuyAmount().add(v.getSellAmount())).multiply(feeRate)).reduce(BigDecimal.ZERO, BigDecimal::add));

        // 所有都标记未锁仓
        binanceTradeInfoExtService.getBaseMapper().update(null,
                Wrappers.lambdaUpdate(BinanceTradeInfoExt.class)
                        .set(BinanceTradeInfoExt::getHedgedFlag, 0)
        );

        // 剩下没有平仓的做空单判断是否需要锁仓
        for (BinanceFuturesTradeInfo sellInfo : sellTradeInfoList) {
            // 当前价格小于开仓价格，说明是盈利的，不需要锁仓
            if (currentPrice.compareTo(sellInfo.getPrice()) <= 0) {
                sellInfo.setQty(BigDecimal.ZERO);
                continue;
            }

            // 查询现货止损单
            List<BinanceTradeInfo> binanceTradeInfos = binanceTradeInfoService.list4hedge(sellInfo.getPrice(), sellInfo.getPrice().add(new BigDecimal("1000")));

            // 标记锁仓
            BigDecimal qty = sellInfo.getQty();
            for (BinanceTradeInfo tradeInfo : binanceTradeInfos) {
                if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                    break; // 已对冲完毕
                }
                // 可匹配的仓位数量
                if (qty.compareTo(tradeInfo.getQty()) >= 0) {
                    qty = qty.subtract(tradeInfo.getQty());
                    sellInfo.setQty(qty);
                    binanceTradeInfoExtService.changeHedgedFlag(tradeInfo.getOrderId());
                }
            }


        }

        // 锁仓统计
        {
            BinanceTradeInfoQueryCriteria criteria = new BinanceTradeInfoQueryCriteria();
            criteria.setHedgedFlag(1);
            List<BinanceTradeInfo> hedgedTradeInfo = binanceTradeInfoService.queryAll(criteria);
            if (CollectionUtils.isNotEmpty(hedgedTradeInfo)) {
                // 锁仓总额
                statsInfoVO.setHedgedAmount(hedgedTradeInfo.stream().map(BinanceTradeInfo::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
                // 锁仓数量
                statsInfoVO.setHedgedQty(hedgedTradeInfo.stream().map(BinanceTradeInfo::getQty).reduce(BigDecimal.ZERO, BigDecimal::add));
                // 锁仓均价
                statsInfoVO.setHedgedAvgPrice(NumberUtil.div(statsInfoVO.getHedgedAmount(), statsInfoVO.getHedgedQty()));
            }
        }


        // 未匹配到现货止损的交易
        statsInfoVO.setNoStopLossTradeInfoList(sellTradeInfoList.stream().filter(v -> v.getQty().compareTo(BigDecimal.ZERO) > 0).collect(Collectors.toList()));


        return statsInfoVO;
    }


}
