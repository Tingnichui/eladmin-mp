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

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import me.zhengjie.invest.constants.BinanceEnum;
import me.zhengjie.invest.domain.BinanceAccountInfo;
import me.zhengjie.invest.domain.BinanceTradeInfo;
import me.zhengjie.invest.domain.BinanceTradeInfoExt;
import me.zhengjie.invest.domain.dto.MatchedTradeInfo;
import me.zhengjie.invest.domain.vo.BinanceTradeInfoQueryCriteria;
import me.zhengjie.invest.domain.vo.BinanceTradeStatsInfoVO;
import me.zhengjie.invest.mapper.BinanceTradeInfoMapper;
import me.zhengjie.invest.service.BinanceAccountInfoService;
import me.zhengjie.invest.service.BinanceTradeInfoExtService;
import me.zhengjie.invest.service.BinanceTradeInfoService;
import me.zhengjie.invest.util.BinanceAccountContextHolder;
import me.zhengjie.invest.util.BinanceSpotUtil;
import me.zhengjie.invest.util.BinanceUsdFuturesUtil;
import me.zhengjie.utils.FileUtil;
import me.zhengjie.utils.PageResult;
import me.zhengjie.utils.PageUtil;
import me.zhengjie.utils.enums.OrderDirectionEnum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author genghui
 * @description 服务实现
 * @date 2025-07-05
 **/
@Service
@RequiredArgsConstructor
public class BinanceTradeInfoServiceImpl extends ServiceImpl<BinanceTradeInfoMapper, BinanceTradeInfo> implements BinanceTradeInfoService {

    private final BinanceTradeInfoMapper binanceTradeInfoMapper;
    private final BinanceAccountInfoService binanceAccountInfoService;
    private final BinanceSpotUtil binanceSpotUtil;
    private final BinanceTradeInfoExtService binanceTradeInfoExtService;
    private final BinanceUsdFuturesUtil binanceUsdFuturesUtil;

    @Override
    public PageResult<BinanceTradeInfo> queryAll(BinanceTradeInfoQueryCriteria criteria, Page<Object> page) {
        return PageUtil.toPage(binanceTradeInfoMapper.findAll(criteria, page));
    }

    @Override
    public List<BinanceTradeInfo> queryAll(BinanceTradeInfoQueryCriteria criteria) {
        return binanceTradeInfoMapper.findAll(criteria);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(BinanceTradeInfo resources) {
        save(resources);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(BinanceTradeInfo resources) {
        BinanceTradeInfo binanceTradeInfo = getById(resources.getId());
        binanceTradeInfo.copy(resources);
        saveOrUpdate(binanceTradeInfo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAll(List<Long> ids) {
        removeBatchByIds(ids);
    }

    @Override
    public void download(List<BinanceTradeInfo> all, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (BinanceTradeInfo binanceTradeInfo : all) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("交易对", binanceTradeInfo.getSymbol());
            map.put("成交价格", binanceTradeInfo.getPrice());
            map.put("成交数量", binanceTradeInfo.getQty());
            map.put("手续费", binanceTradeInfo.getCommission());
            map.put("成交时间", binanceTradeInfo.getTime());
            map.put("订单 ID", binanceTradeInfo.getOrderId());
            map.put("成交额", binanceTradeInfo.getQuoteQty());
            map.put("手续费资产", binanceTradeInfo.getCommissionAsset());
            map.put("是否为买方", binanceTradeInfo.getIsBuyer());
            map.put("是否为挂单方", binanceTradeInfo.getIsMaker());
            map.put("是否为最佳匹配", binanceTradeInfo.getIsBestMatch());
            list.add(map);
        }
        FileUtil.downloadExcel(list, response);
    }

    @Override
    public void syncTradeInfo(String symbol) {
        // 查询所有账号
        List<BinanceAccountInfo> accountInfoList = binanceAccountInfoService.listUseApiAccount();
        for (BinanceAccountInfo accountInfo : accountInfoList) {
            // 设置账号信息
            BinanceAccountContextHolder.set(accountInfo);
            // 调用接口获取最近的订单信息
            List<BinanceTradeInfo> orderInfoList = binanceSpotUtil.getMyTrades(symbol);
            // 查询已经在库中的订单
            Set<Long> existOrderIdSet = this.list(
                    Wrappers.lambdaQuery(BinanceTradeInfo.class)
                            .select(BinanceTradeInfo::getOrderId)
                            .in(BinanceTradeInfo::getOrderId, orderInfoList.stream().map(BinanceTradeInfo::getOrderId).collect(Collectors.toSet()))
            ).stream().map(BinanceTradeInfo::getOrderId).collect(Collectors.toSet());

            // 过滤掉已存在的订单
            List<BinanceTradeInfo> newOrders = orderInfoList.stream()
                    .filter(order -> !existOrderIdSet.contains(order.getOrderId()))
                    .peek(order -> order.setUid(accountInfo.getUid()))
                    .collect(Collectors.toList());

            // 保存新订单
            this.saveOrUpdateBatch(newOrders);

            // 清除账号信息
            BinanceAccountContextHolder.clear();
        }

    }

    @Override
    public BinanceTradeStatsInfoVO stats(BinanceTradeInfoQueryCriteria criteria) {
        BinanceTradeStatsInfoVO statsInfoVO = new BinanceTradeStatsInfoVO();

        // 未锁仓的撮合交易
        criteria.setHedgedFlag(0);

        // 撮合交易对
        List<MatchedTradeInfo> matchedList = new ArrayList<>();
        List<BinanceTradeInfo> buyTradeList, sellTradeList;
        {
            // 查询所有买入 价格从低到高
            criteria.setOrderColumn("price");
            criteria.setOrderDirection(OrderDirectionEnum.ASC.getValue());
            criteria.setIsBuyer(1);
            buyTradeList = binanceTradeInfoMapper.findAll(criteria);
            // 查询所有卖出 时间从早到晚
            criteria.setOrderColumn("time");
            criteria.setOrderDirection(OrderDirectionEnum.ASC.getValue());
            criteria.setIsBuyer(0);
            sellTradeList = binanceTradeInfoMapper.findAll(criteria);


            // 匹配逻辑：遍历卖单，有卖肯定有买
            Iterator<BinanceTradeInfo> sellIterator = sellTradeList.iterator();
            while (sellIterator.hasNext()) {
                BinanceTradeInfo sell = sellIterator.next();
                // 按照顺序寻找之前的买单
                Iterator<BinanceTradeInfo> buyIterator = buyTradeList.iterator();
                while (buyIterator.hasNext()) {
                    BinanceTradeInfo buy = buyIterator.next();
                    if (buy.getQty().compareTo(BigDecimal.ZERO) <= 0) {
                        buyIterator.remove();
                        continue;
                    }
                    if (sell.getTime().before(buy.getTime())) {
                        continue;
                    }

                    // 可匹配的仓位数量
                    BigDecimal matchQty = buy.getQty().min(sell.getQty());
                    // 撮合交易记录
                    MatchedTradeInfo matched = new MatchedTradeInfo(true);
                    matched.setQty(matchQty);
                    matched.setOpenPrice(buy.getPrice());
                    matched.setClosePrice(sell.getPrice());
                    matched.setOpenTime(buy.getTime());
                    matched.setCloseTime(sell.getTime());
                    // 最小利润限制
                    if (matched.getPnlRatio().compareTo(criteria.getMinProfitPct()) < 0) {
                        continue;
                    }

                    matchedList.add(matched);
                    // 更新买入剩余量
                    buy.setQty(buy.getQty().subtract(matchQty));
                    // 更新卖出剩余量`
                    sell.setQty(sell.getQty().subtract(matchQty));

                    // 判断是否买入还有剩余 已经平仓掉了就直接去除，并且终止撮合
                    if (sell.getQty().compareTo(BigDecimal.ZERO) <= 0) {
                        sellIterator.remove();
                        break;
                    }

                }

            }
        }


        // 撮合总量
        BigDecimal totalQty = matchedList.stream()
                .map(MatchedTradeInfo::getQty)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 买入总金额
        BigDecimal totalBuyAmount = matchedList.stream()
                .map(MatchedTradeInfo::getOpenAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        statsInfoVO.setTotalBuyAmount(totalBuyAmount);
        // 买入均价
        BigDecimal avgBuyPrice = totalBuyAmount.divide(totalQty, 8, RoundingMode.HALF_UP);
        statsInfoVO.setAvgBuyPrice(avgBuyPrice);

        // 卖出总金额
        BigDecimal totalSellAmount = matchedList.stream()
                .map(MatchedTradeInfo::getCloseAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        statsInfoVO.setTotalSellAmount(totalSellAmount);
        // 卖出均价
        BigDecimal avgSellPrice = totalSellAmount.divide(totalQty, 8, RoundingMode.HALF_UP);
        statsInfoVO.setAvgSellPrice(avgSellPrice);

        // 总的利润
        BigDecimal profit = totalSellAmount.subtract(totalBuyAmount);
        statsInfoVO.setProfit(profit);
        // 收益率
        BigDecimal profitPct = profit.divide(totalBuyAmount, 4, RoundingMode.HALF_UP);
        statsInfoVO.setProfitPct(profitPct);

        // 剩余未平仓总金额
        BigDecimal totalWaitSellAmount = buyTradeList.stream()
                .map(b -> b.getQty().multiply(b.getPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        statsInfoVO.setTotalWaitSellAmount(totalWaitSellAmount);
        // 剩余未平仓总数量
        BigDecimal totalWaitSellQty = buyTradeList.stream()
                .map(BinanceTradeInfo::getQty)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        statsInfoVO.setTotalWaitSellQty(totalWaitSellQty);
        // 剩余未平仓均价
        BigDecimal totalWaitAvgSellPrice = totalWaitSellAmount.divide(totalWaitSellQty, 8, RoundingMode.HALF_UP);
        statsInfoVO.setTotalWaitAvgSellPrice(totalWaitAvgSellPrice);

        // 持仓时间（单位：毫秒）
        List<Long> holdDurations = matchedList.stream()
                .map(MatchedTradeInfo::getHoldMillis)
                .collect(Collectors.toList());

        if (!holdDurations.isEmpty()) {
            // 平均持仓时间（毫秒）
            long avgHoldTimeMs = (long) holdDurations.stream().mapToLong(Long::longValue).average().orElse(0);
            // 最小/最大持仓时间
            long minHoldTimeMs = Collections.min(holdDurations);
            long maxHoldTimeMs = Collections.max(holdDurations);

            // 可选择换算成小时/分钟/秒
            statsInfoVO.setAvgHoldTimeMs(avgHoldTimeMs);
            statsInfoVO.setMinHoldTimeMs(minHoldTimeMs);
            statsInfoVO.setMaxHoldTimeMs(maxHoldTimeMs);
        }

        statsInfoVO.setMatchedTradeInfoList(matchedList);

        // 剩余待平仓交易
        buyTradeList.sort(Comparator.comparing(BinanceTradeInfo::getPrice).reversed());
        statsInfoVO.setWaitSellTradeInfoList(buyTradeList);

        // 手续费计算 按照0.1%
        statsInfoVO.setFee((statsInfoVO.getTotalBuyAmount().add(statsInfoVO.getTotalSellAmount())).multiply(new BigDecimal("0.001")));


        try {
            BigDecimal currentPrice = binanceSpotUtil.getPrice(BinanceEnum.SYMBOL.valueOf(criteria.getSymbol()));
            statsInfoVO.setCurrentSpotPrice(currentPrice);

            // 计算买入总金额与数量
            for (BinanceTradeInfo buy : buyTradeList) {
                // 可匹配的仓位数量
                BigDecimal matchQty = buy.getQty();
                // 撮合交易记录
                MatchedTradeInfo matched = new MatchedTradeInfo(true);
                matched.setQty(matchQty);
                matched.setOpenPrice(buy.getPrice());
                matched.setClosePrice(currentPrice);

                BigDecimal p = matched.getRealizedPnl();
                if (p.compareTo(BigDecimal.ZERO) > 0) {
                    // 持仓盈利
                    statsInfoVO.setHoldingProfit(statsInfoVO.getHoldingProfit().add(p));
                } else {
                    // 持仓亏损
                    statsInfoVO.setHoldingLoss(statsInfoVO.getHoldingLoss().add(p));
                }

                // 持仓盈亏
                statsInfoVO.setHoldingProfitLoss(statsInfoVO.getHoldingProfitLoss().add(p));
            }

        } catch (Exception e) {
        }

        return statsInfoVO;
    }

    @Override
    public void syncAll() {
        for (BinanceEnum.SYMBOL symbol : BinanceEnum.SYMBOL.values()) {
            if (symbol.getType() == 0) {
                this.syncTradeInfo(symbol.toString());
            }
        }

    }

    @Override
    public void syncHedge() {

        BinanceAccountContextHolder.runWith(binanceAccountInfoService.getAccountByIdCardName("耿辉"), () -> {
            JSONObject account = binanceUsdFuturesUtil.account();
            if (null == account) {
                throw new RuntimeException("获取合约信息失败");
            }
            BigDecimal price = account.getBigDecimal("entryPrice");
            BigDecimal qty = account.getBigDecimal("positionAmt").multiply(new BigDecimal("-1"));

            // 对冲仓位的开仓价格 之下，按照创建时间倒序 价格倒序
            List<BinanceTradeInfo> binanceTradeInfos = binanceTradeInfoMapper.list4hedge(price.subtract(new BigDecimal("1000")) ,price, null);

            // 所有都标记未锁仓
            binanceTradeInfoExtService.getBaseMapper().update(null,
                    Wrappers.lambdaUpdate(BinanceTradeInfoExt.class)
                            .set(BinanceTradeInfoExt::getHedgedFlag, 0)
            );

            // 匹配标记锁仓
            for (BinanceTradeInfo tradeInfo : binanceTradeInfos) {
                if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                    break; // 已对冲完毕
                }
                // 可匹配的仓位数量
                if (qty.compareTo(tradeInfo.getQty()) >= 0) {
                    qty = qty.subtract(tradeInfo.getQty());
                    binanceTradeInfoExtService.changeHedgedFlag(tradeInfo.getOrderId());
                }
            }

        });

    }

    @Override
    public List<BinanceTradeInfo> list4hedge(BigDecimal lowPrice, BigDecimal highPrice, BigDecimal qty) {
        return binanceTradeInfoMapper.list4hedge(lowPrice, highPrice, qty);
    }

}