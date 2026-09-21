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

import com.alibaba.fastjson2.JSONObject;
import cn.hutool.core.util.NumberUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import me.zhengjie.invest.constants.BinanceEnum;
import me.zhengjie.invest.domain.BinanceAccountInfo;
import me.zhengjie.invest.domain.BinanceFuturesTradeInfo;
import me.zhengjie.invest.domain.dto.MatchedTradeInfo;
import me.zhengjie.invest.domain.dto.BinanceFuturesTradeStatsInfoVO;
import me.zhengjie.invest.domain.dto.BinanceUsdFuturesAccountInfo;
import me.zhengjie.invest.domain.dto.BinanceUsdFuturesPositionInfo;
import me.zhengjie.invest.domain.dto.BinanceUsdFuturesStatsInfoVO;
import me.zhengjie.invest.domain.dto.BinanceUsdFuturesTradeSummary;
import me.zhengjie.invest.service.BinanceAccountInfoService;
import me.zhengjie.invest.service.BinanceTradeInfoService;
import me.zhengjie.invest.service.support.BinanceSpotHedgeContext;
import me.zhengjie.invest.util.BinanceAccountContextHolder;
import me.zhengjie.invest.util.BinanceSpotUtil;
import me.zhengjie.invest.util.BinanceUsdFuturesUtil;
import me.zhengjie.invest.util.TradeMatcherUtil;
import me.zhengjie.utils.FileUtil;
import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import me.zhengjie.invest.service.BinanceFuturesTradeInfoService;
import me.zhengjie.invest.domain.dto.BinanceFuturesTradeInfoQueryCriteria;
import me.zhengjie.invest.mapper.BinanceFuturesTradeInfoMapper;
import me.zhengjie.utils.RedisUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import me.zhengjie.utils.PageUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.io.IOException;
import java.util.stream.Collectors;
import javax.annotation.Resource;
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

    private static final int SYNC_PAGE_SIZE = 1000;

    @Resource
    private BinanceFuturesTradeInfoMapper binanceFuturesTradeInfoMapper;
    @Resource
    private BinanceUsdFuturesUtil binanceUsdFuturesUtil;
    @Resource
    private BinanceSpotUtil binanceSpotUtil;
    @Resource
    private BinanceAccountInfoService binanceAccountInfoService;
    @Resource
    private RedisUtils redisUtils;
    @Resource
    private BinanceTradeInfoService binanceTradeInfoService;

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
            sync(accountInfo);
        }
    }

    @Override
    public int sync(BinanceAccountInfo accountInfo) {
        return sync(accountInfo, BinanceEnum.SYMBOL.BTCUSDT.name());
    }

    @Override
    public int sync(BinanceAccountInfo accountInfo, String symbol) {
        int[] syncedCount = {0};
        BinanceAccountContextHolder.runWith(accountInfo, () -> {
            List<BinanceFuturesTradeInfo> latestTrades = this.list(
                    Wrappers.<BinanceFuturesTradeInfo>query()
                            .select("id")
                            .eq("uid", accountInfo.getUid())
                            .eq("symbol", symbol)
                            .orderByDesc("id")
                            .last("limit 1")
            );
            long fromId = latestTrades.isEmpty() ? 0L : latestTrades.get(0).getId() + 1L;
            while (true) {
                List<BinanceFuturesTradeInfo> orderInfoList = binanceUsdFuturesUtil.userTradesFromId(
                        symbol, fromId, SYNC_PAGE_SIZE);
                if (CollectionUtils.isEmpty(orderInfoList)) {
                    break;
                }
                Set<Long> pageIds = orderInfoList.stream().map(BinanceFuturesTradeInfo::getId)
                        .filter(Objects::nonNull).collect(Collectors.toSet());
                if (pageIds.isEmpty()) {
                    throw new IllegalStateException("币安 U 本位成交数据缺少 ID");
                }
                Set<Long> existIdSet = this.list(
                        Wrappers.<BinanceFuturesTradeInfo>query()
                                .select("id")
                                .eq("uid", accountInfo.getUid())
                                .eq("symbol", symbol)
                                .in("id", pageIds)
                ).stream().map(BinanceFuturesTradeInfo::getId).collect(Collectors.toSet());
                List<BinanceFuturesTradeInfo> newOrders = orderInfoList.stream()
                        .filter(order -> order.getId() != null && !existIdSet.contains(order.getId()))
                        .peek(order -> order.setUid(accountInfo.getUid()))
                        .collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(newOrders)) {
                    this.saveOrUpdateBatch(newOrders);
                    syncedCount[0] += newOrders.size();
                }
                long nextFromId = Collections.max(pageIds) + 1L;
                if (nextFromId <= fromId) {
                    throw new IllegalStateException("币安 U 本位成交分页未向前推进");
                }
                fromId = nextFromId;
                if (orderInfoList.size() < SYNC_PAGE_SIZE) {
                    break;
                }
            }
        });
        return syncedCount[0];
    }

    @Override
    public BinanceUsdFuturesStatsInfoVO queryStats(Integer uid, String symbol, String positionSide) {
        BinanceUsdFuturesStatsInfoVO result = new BinanceUsdFuturesStatsInfoVO();
        List<JSONObject> positions = binanceUsdFuturesUtil.positionRisk(symbol);
        JSONObject position = positions.stream()
                .filter(item -> positionSide.equalsIgnoreCase(item.getString("positionSide")))
                .findFirst().orElse(null);
        if (position == null) {
            BinanceUsdFuturesPositionInfo emptyPosition = new BinanceUsdFuturesPositionInfo();
            emptyPosition.setSymbol(symbol);
            emptyPosition.setPositionSide(positionSide);
            result.setPositionInfo(emptyPosition);
            result.getWarnings().add("币安未返回当前交易对和持仓方向的持仓信息");
        } else {
            result.setPositionInfo(toPositionInfo(position));
        }
        applySymbolConfig(result.getPositionInfo(), binanceUsdFuturesUtil.symbolConfig(symbol));
        result.setAccountInfo(toAccountInfo(binanceUsdFuturesUtil.account()));

        List<BinanceFuturesTradeInfo> trades = binanceFuturesTradeInfoMapper.selectList(
                Wrappers.lambdaQuery(BinanceFuturesTradeInfo.class)
                        .eq(BinanceFuturesTradeInfo::getUid, uid)
                        .eq(BinanceFuturesTradeInfo::getSymbol, symbol)
                        .eq(BinanceFuturesTradeInfo::getPositionSide, positionSide)
                        .orderByAsc(BinanceFuturesTradeInfo::getTime)
                        .orderByAsc(BinanceFuturesTradeInfo::getId)
        );
        result.setTradeSummary(createTradeSummary(trades, positionSide));
        if ("BOTH".equals(positionSide)) {
            result.getWarnings().add("单向持仓模式暂不提供逐笔未平仓分布");
        } else {
            result.setTradeList(createOpenTradeList(trades, positionSide,
                    result.getPositionInfo().getMarkPrice()));
            result.getTradeSummary().setOpenTradeCount(result.getTradeList().size());
        }
        return result;
    }

    private BinanceUsdFuturesPositionInfo toPositionInfo(JSONObject source) {
        BinanceUsdFuturesPositionInfo target = new BinanceUsdFuturesPositionInfo();
        target.setSymbol(source.getString("symbol"));
        target.setPositionSide(source.getString("positionSide"));
        target.setPositionAmt(decimal(source, "positionAmt"));
        target.setEntryPrice(decimal(source, "entryPrice"));
        target.setBreakEvenPrice(decimal(source, "breakEvenPrice"));
        target.setMarkPrice(decimal(source, "markPrice"));
        target.setNotional(decimal(source, "notional"));
        target.setUnrealizedPnl(decimal(source, "unRealizedProfit"));
        target.setLiquidationPrice(decimal(source, "liquidationPrice"));
        target.setIsolatedMargin(decimal(source, "isolatedMargin"));
        target.setInitialMargin(decimal(source, "initialMargin"));
        target.setMaintMargin(decimal(source, "maintMargin"));
        target.setPositionInitialMargin(decimal(source, "positionInitialMargin"));
        target.setOpenOrderInitialMargin(decimal(source, "openOrderInitialMargin"));
        target.setMarginAsset(source.getString("marginAsset"));
        target.setAdl(source.getInteger("adl"));
        target.setUpdateTime(source.getLong("updateTime"));
        return target;
    }

    private void applySymbolConfig(BinanceUsdFuturesPositionInfo target, JSONObject config) {
        if (config == null) {
            return;
        }
        target.setLeverage(config.getInteger("leverage"));
        target.setMarginType(config.getString("marginType"));
        target.setAutoAddMargin(config.getBoolean("isAutoAddMargin"));
        target.setMaxNotionalValue(decimal(config, "maxNotionalValue"));
    }

    private BinanceUsdFuturesAccountInfo toAccountInfo(JSONObject source) {
        BinanceUsdFuturesAccountInfo target = new BinanceUsdFuturesAccountInfo();
        target.setWalletBalance(decimal(source, "totalWalletBalance"));
        target.setUnrealizedProfit(decimal(source, "totalUnrealizedProfit"));
        target.setMarginBalance(decimal(source, "totalMarginBalance"));
        target.setAvailableBalance(decimal(source, "availableBalance"));
        target.setInitialMargin(decimal(source, "totalInitialMargin"));
        target.setMaintMargin(decimal(source, "totalMaintMargin"));
        return target;
    }

    private BinanceUsdFuturesTradeSummary createTradeSummary(List<BinanceFuturesTradeInfo> trades,
                                                              String positionSide) {
        BinanceUsdFuturesTradeSummary summary = new BinanceUsdFuturesTradeSummary();
        BigDecimal realizedPnl = trades.stream().map(BinanceFuturesTradeInfo::getRealizedPnl)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal commission = trades.stream().map(BinanceFuturesTradeInfo::getCommission)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add).abs();
        summary.setRealizedPnl(realizedPnl);
        summary.setCommission(commission);
        summary.setNetPnl(realizedPnl.subtract(commission));
        summary.setTotalTradeCount(trades.size());
        if (!"BOTH".equals(positionSide)) {
            String closeSide = "LONG".equals(positionSide) ? "SELL" : "BUY";
            summary.setClosedTradeCount((int) trades.stream()
                    .filter(trade -> closeSide.equals(trade.getSide())).count());
        }
        return summary;
    }

    private List<MatchedTradeInfo> createOpenTradeList(List<BinanceFuturesTradeInfo> trades,
                                                        String positionSide,
                                                        BigDecimal markPrice) {
        boolean longSide = "LONG".equals(positionSide);
        String openSide = longSide ? "BUY" : "SELL";
        List<BinanceFuturesTradeInfo> openTrades = trades.stream()
                .filter(trade -> openSide.equals(trade.getSide()))
                .map(this::copyTrade).collect(Collectors.toCollection(ArrayList::new));
        List<BinanceFuturesTradeInfo> closeTrades = trades.stream()
                .filter(trade -> !openSide.equals(trade.getSide()))
                .map(this::copyTrade).collect(Collectors.toCollection(ArrayList::new));
        TradeMatcherUtil.matchTradesFifo(
                longSide, "0.0005", openTrades, closeTrades,
                BinanceFuturesTradeInfo::getQty,
                BinanceFuturesTradeInfo::setQty,
                BinanceFuturesTradeInfo::getPrice,
                BinanceFuturesTradeInfo::getTime,
                BinanceFuturesTradeInfo::getId
        );
        List<MatchedTradeInfo> result = new ArrayList<>();
        for (BinanceFuturesTradeInfo trade : openTrades) {
            if (trade.getQty() == null || trade.getQty().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            MatchedTradeInfo item = new MatchedTradeInfo(longSide, "0.0005");
            item.setTradeId(trade.getId());
            item.setOrderId(trade.getOrderId());
            item.setQty(trade.getQty());
            item.setOpenPrice(trade.getPrice());
            item.setOpenTime(trade.getTime());
            item.setClosePrice(markPrice == null ? BigDecimal.ZERO : markPrice);
            result.add(item);
        }
        result.sort(Comparator.comparing(MatchedTradeInfo::getOpenPrice).reversed());
        return result;
    }

    private BigDecimal decimal(JSONObject source, String key) {
        BigDecimal value = source == null ? null : source.getBigDecimal(key);
        return value == null ? BigDecimal.ZERO : value;
    }

    @Override
    public Date getLastPosCloseTime(Integer uid) {
        final String symbol = BinanceEnum.SYMBOL.BTCUSDT.name();
        final String key = "BINANCE:FUTURES:LAST_POS_CLOSE_TIME:" + uid + ":" + symbol;
        Date lastPosCloseTime = redisUtils.get(key, Date.class);
        List<BinanceFuturesTradeInfo> binanceFuturesTradeInfoList = binanceFuturesTradeInfoMapper.selectList(
                Wrappers.lambdaQuery(BinanceFuturesTradeInfo.class)
                        .eq(BinanceFuturesTradeInfo::getUid, uid)
                        .eq(BinanceFuturesTradeInfo::getSymbol, symbol)
                        .eq(BinanceFuturesTradeInfo::getPositionSide, "SHORT")
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
                lastPosCloseTime = tradeInfo.getTime();
                redisUtils.set(key, tradeInfo.getTime());
            }
        }

        return lastPosCloseTime == null ? new Date(0L) : lastPosCloseTime;
    }

    @Override
    public BinanceFuturesTradeStatsInfoVO stats(Integer uid) {
        return stats(uid, binanceTradeInfoService.createHedgeContext(uid, BinanceEnum.SYMBOL.BTCUSDT.name()));
    }

    @Override
    public BinanceFuturesTradeStatsInfoVO stats(Integer uid, BinanceSpotHedgeContext hedgeContext) {
        BigDecimal currentPrice = binanceUsdFuturesUtil.price(BinanceEnum.SYMBOL.BTCUSDT);
        return stats(uid, hedgeContext, currentPrice);
    }

    @Override
    public BinanceFuturesTradeStatsInfoVO stats(Integer uid, BinanceSpotHedgeContext hedgeContext, BigDecimal currentPrice) {
        return stats(uid, hedgeContext, currentPrice, null);
    }

    @Override
    public BinanceFuturesTradeStatsInfoVO stats(Integer uid, BinanceSpotHedgeContext hedgeContext,
                                                BigDecimal currentPrice,
                                                List<BinanceFuturesTradeInfo> snapshotTrades) {
        BinanceFuturesTradeStatsInfoVO statsInfoVO = new BinanceFuturesTradeStatsInfoVO();

        final boolean side = false;

        List<BinanceFuturesTradeInfo> openList;
        List<BinanceFuturesTradeInfo> closeList;
        if (snapshotTrades == null) {
            Date lastPosCloseTime = this.getLastPosCloseTime(uid);
            openList = binanceFuturesTradeInfoMapper.selectList(
                    Wrappers.lambdaQuery(BinanceFuturesTradeInfo.class)
                            .eq(BinanceFuturesTradeInfo::getUid, uid)
                            .eq(BinanceFuturesTradeInfo::getSymbol, BinanceEnum.SYMBOL.BTCUSDT.name())
                            .gt(BinanceFuturesTradeInfo::getTime, lastPosCloseTime)
                            .eq(BinanceFuturesTradeInfo::getSide, "SELL")
                            .eq(BinanceFuturesTradeInfo::getPositionSide, "SHORT")
                            .orderByAsc(BinanceFuturesTradeInfo::getTime)
            );
            closeList = binanceFuturesTradeInfoMapper.selectList(
                    Wrappers.lambdaQuery(BinanceFuturesTradeInfo.class)
                            .eq(BinanceFuturesTradeInfo::getUid, uid)
                            .eq(BinanceFuturesTradeInfo::getSymbol, BinanceEnum.SYMBOL.BTCUSDT.name())
                            .gt(BinanceFuturesTradeInfo::getTime, lastPosCloseTime)
                            .eq(BinanceFuturesTradeInfo::getSide, "BUY")
                            .eq(BinanceFuturesTradeInfo::getPositionSide, "SHORT")
                            .orderByAsc(BinanceFuturesTradeInfo::getTime)
            );
        } else {
            openList = snapshotTrades.stream().filter(trade -> "SELL".equals(trade.getSide()))
                    .map(this::copyTrade).collect(Collectors.toCollection(ArrayList::new));
            closeList = snapshotTrades.stream().filter(trade -> "BUY".equals(trade.getSide()))
                    .map(this::copyTrade).collect(Collectors.toCollection(ArrayList::new));
        }

        // 盈利交易匹配
        {

            List<MatchedTradeInfo> matchedList = TradeMatcherUtil.matchTrades(
                    side,
                    "0.0005",
                    openList,
                    closeList,
                    BinanceFuturesTradeInfo::getQty,
                    BinanceFuturesTradeInfo::setQty,
                    BinanceFuturesTradeInfo::getPrice,
                    BinanceFuturesTradeInfo::getTime,
                    match -> {
                        return match.getNetPnl().compareTo(BigDecimal.ZERO) >= 0;
                    }
            );

            // 盈亏
            statsInfoVO.setPnl(matchedList.stream().map(MatchedTradeInfo::getPnl).reduce(BigDecimal.ZERO, BigDecimal::add));
            // 手续费
            statsInfoVO.setFee(matchedList.stream().map(MatchedTradeInfo::getFee).reduce(BigDecimal.ZERO, BigDecimal::add));
            // 净盈亏
            statsInfoVO.setNetPnl(matchedList.stream().map(MatchedTradeInfo::getNetPnl).reduce(BigDecimal.ZERO, BigDecimal::add));

            if (CollectionUtils.isNotEmpty(openList)) {
                // 持仓金额
                statsInfoVO.setPosAmount(openList.stream().map(v -> v.getQty().multiply(v.getPrice())).reduce(BigDecimal.ZERO, BigDecimal::add));
                // 持仓数量
                statsInfoVO.setPosQty(openList.stream().map(BinanceFuturesTradeInfo::getQty).reduce(BigDecimal.ZERO, BigDecimal::add));
                // 持仓均价
                statsInfoVO.setPosAvgPrice(statsInfoVO.getPosAmount().divide(statsInfoVO.getPosQty(), 8, RoundingMode.HALF_UP));
            }

        }


        // 现货止损交易匹配依赖实时价格；价格不可用时保留本地历史统计
        if (currentPrice != null) {
            List<MatchedTradeInfo> matchedList = TradeMatcherUtil.matchTrades(side, "0.001", openList, BinanceFuturesTradeInfo::getQty, BinanceFuturesTradeInfo::getPrice, currentPrice,
                    matched -> {
                        // 亏损单找现货做对冲止损
                        if (matched.getNetPnl().compareTo(BigDecimal.ZERO) <= 0) {
                            BinanceSpotHedgeContext.HedgeResult hedgeResult = hedgeContext.allocate(
                                    matched.getOpenPrice(),
                                    matched.getOpenPrice().add(new BigDecimal("1000")),
                                    matched.getQty(),
                                    100
                            );
                            matched.setClosePrice(hedgeResult.getAveragePrice());

                        }
                    });

            // 计算止损金额
            statsInfoVO.setStopLossAmount(matchedList.stream().map(MatchedTradeInfo::getPnl).filter(v -> v.compareTo(BigDecimal.ZERO) <= 0).reduce(BigDecimal.ZERO, BigDecimal::add));
            // 未平仓的交易
            statsInfoVO.setTradeList(matchedList.stream().sorted(Comparator.comparing(MatchedTradeInfo::getOpenPrice).reversed()).collect(Collectors.toList()));
        } else {
            statsInfoVO.setStopLossAmount(null);
            statsInfoVO.setTradeList(Collections.emptyList());
        }

        return statsInfoVO;
    }

    private BinanceFuturesTradeInfo copyTrade(BinanceFuturesTradeInfo source) {
        BinanceFuturesTradeInfo target = new BinanceFuturesTradeInfo();
        target.copy(source);
        return target;
    }


}
