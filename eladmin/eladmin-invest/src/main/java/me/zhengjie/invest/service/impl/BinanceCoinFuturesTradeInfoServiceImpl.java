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

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import me.zhengjie.invest.constants.BinanceEnum;
import me.zhengjie.invest.domain.BinanceAccountInfo;
import me.zhengjie.invest.domain.BinanceCoinFuturesTradeInfo;
import me.zhengjie.invest.domain.dto.MatchedTradeInfo;
import me.zhengjie.invest.domain.dto.BinanceCoinFuturesTradeInfoQueryCriteria;
import me.zhengjie.invest.domain.dto.BinanceFuturesTradeStatsInfoVO;
import me.zhengjie.invest.mapper.BinanceCoinFuturesTradeInfoMapper;
import me.zhengjie.invest.service.BinanceAccountInfoService;
import me.zhengjie.invest.service.BinanceCoinFuturesTradeInfoService;
import me.zhengjie.invest.service.BinanceTradeInfoService;
import me.zhengjie.invest.service.support.BinanceSpotHedgeContext;
import me.zhengjie.invest.util.BinanceAccountContextHolder;
import me.zhengjie.invest.util.BinanceCoinFuturesUtil;
import me.zhengjie.invest.util.TradeMatcherUtil;
import me.zhengjie.utils.FileUtil;
import me.zhengjie.utils.PageResult;
import me.zhengjie.utils.PageUtil;
import me.zhengjie.utils.RedisUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author genghui
 * @description 服务实现
 * @date 2025-11-23
 **/
@Service
@RequiredArgsConstructor
public class BinanceCoinFuturesTradeInfoServiceImpl extends ServiceImpl<BinanceCoinFuturesTradeInfoMapper, BinanceCoinFuturesTradeInfo> implements BinanceCoinFuturesTradeInfoService {

    private static final int SYNC_PAGE_SIZE = 1000;

    @Resource
    private BinanceCoinFuturesTradeInfoMapper binanceCoinFuturesTradeInfoMapper;
    @Resource
    private BinanceAccountInfoService binanceAccountInfoService;
    @Resource
    private BinanceCoinFuturesUtil binanceCoinFuturesUtil;
    @Resource
    private RedisUtils redisUtils;
    @Resource
    private BinanceTradeInfoService binanceTradeInfoService;

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
            sync(accountInfo);
        }
    }

    @Override
    public int sync(BinanceAccountInfo accountInfo) {
        int[] syncedCount = {0};
        BinanceAccountContextHolder.runWith(accountInfo, () -> {
            String symbol = BinanceEnum.SYMBOL.BTCUSD_PERP.name();
            List<BinanceCoinFuturesTradeInfo> latestTrades = this.list(
                    Wrappers.<BinanceCoinFuturesTradeInfo>query()
                            .select("id")
                            .eq("uid", accountInfo.getUid())
                            .eq("symbol", symbol)
                            .orderByDesc("id")
                            .last("limit 1")
            );
            long fromId = latestTrades.isEmpty() ? 0L : latestTrades.get(0).getId() + 1L;
            while (true) {
                List<BinanceCoinFuturesTradeInfo> orderInfoList = binanceCoinFuturesUtil.userTradesFromId(
                        BinanceEnum.SYMBOL.BTCUSD_PERP, fromId, SYNC_PAGE_SIZE);
                if (CollectionUtils.isEmpty(orderInfoList)) {
                    break;
                }
                Set<Long> pageIds = orderInfoList.stream().map(BinanceCoinFuturesTradeInfo::getId)
                        .filter(Objects::nonNull).collect(Collectors.toSet());
                if (pageIds.isEmpty()) {
                    throw new IllegalStateException("币安币本位成交数据缺少 ID");
                }
                Set<Long> existIdSet = this.list(
                        Wrappers.<BinanceCoinFuturesTradeInfo>query()
                                .select("id")
                                .eq("uid", accountInfo.getUid())
                                .eq("symbol", symbol)
                                .in("id", pageIds)
                ).stream().map(BinanceCoinFuturesTradeInfo::getId).collect(Collectors.toSet());
                List<BinanceCoinFuturesTradeInfo> newOrders = orderInfoList.stream()
                        .filter(order -> order.getId() != null && !existIdSet.contains(order.getId()))
                        .peek(order -> order.setUid(accountInfo.getUid()))
                        .collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(newOrders)) {
                    this.saveOrUpdateBatch(newOrders);
                    syncedCount[0] += newOrders.size();
                }
                long nextFromId = Collections.max(pageIds) + 1L;
                if (nextFromId <= fromId) {
                    throw new IllegalStateException("币安币本位成交分页未向前推进");
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
    public Date getLastPosCloseTime(Integer uid) {
        final String symbol = BinanceEnum.SYMBOL.BTCUSD_PERP.name();
        final String key = "BINANCE:COIN_FUTURES:LAST_POS_CLOSE_TIME:" + uid + ":" + symbol;
        Date lastPosCloseTime = redisUtils.get(key, Date.class);
        List<BinanceCoinFuturesTradeInfo> binanceFuturesTradeInfoList = baseMapper.selectList(
                Wrappers.lambdaQuery(BinanceCoinFuturesTradeInfo.class)
                        .eq(BinanceCoinFuturesTradeInfo::getUid, uid)
                        .eq(BinanceCoinFuturesTradeInfo::getSymbol, symbol)
                        .eq(BinanceCoinFuturesTradeInfo::getPositionSide, "SHORT")
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
        BigDecimal currentPrice = binanceCoinFuturesUtil.price(BinanceEnum.SYMBOL.BTCUSD_PERP);
        BigDecimal fundingFee = this.calculatePositionFundingFee(uid, BinanceEnum.SYMBOL.BTCUSD_PERP);
        return stats(uid, hedgeContext, currentPrice, fundingFee);
    }

    @Override
    public BinanceFuturesTradeStatsInfoVO stats(Integer uid, BinanceSpotHedgeContext hedgeContext,
                                                BigDecimal currentPrice, BigDecimal fundingFee) {
        return stats(uid, hedgeContext, currentPrice, fundingFee, null);
    }

    @Override
    public BinanceFuturesTradeStatsInfoVO stats(Integer uid, BinanceSpotHedgeContext hedgeContext,
                                                BigDecimal currentPrice, BigDecimal fundingFee,
                                                List<BinanceCoinFuturesTradeInfo> snapshotTrades) {
        BinanceFuturesTradeStatsInfoVO statsInfoVO = new BinanceFuturesTradeStatsInfoVO();

        final boolean side = false;

        List<BinanceCoinFuturesTradeInfo> openList;
        List<BinanceCoinFuturesTradeInfo> closeList;
        if (snapshotTrades == null) {
            Date lastPosCloseTime = this.getLastPosCloseTime(uid);
            openList = this.list(
                    Wrappers.lambdaQuery(BinanceCoinFuturesTradeInfo.class)
                            .eq(BinanceCoinFuturesTradeInfo::getUid, uid)
                            .eq(BinanceCoinFuturesTradeInfo::getSymbol, BinanceEnum.SYMBOL.BTCUSD_PERP.name())
                            .gt(BinanceCoinFuturesTradeInfo::getTime, lastPosCloseTime)
                            .eq(BinanceCoinFuturesTradeInfo::getSide, "SELL")
                            .eq(BinanceCoinFuturesTradeInfo::getPositionSide, "SHORT")
                            .orderByAsc(BinanceCoinFuturesTradeInfo::getTime)
            );
            closeList = this.list(
                    Wrappers.lambdaQuery(BinanceCoinFuturesTradeInfo.class)
                            .eq(BinanceCoinFuturesTradeInfo::getUid, uid)
                            .eq(BinanceCoinFuturesTradeInfo::getSymbol, BinanceEnum.SYMBOL.BTCUSD_PERP.name())
                            .gt(BinanceCoinFuturesTradeInfo::getTime, lastPosCloseTime)
                            .eq(BinanceCoinFuturesTradeInfo::getSide, "BUY")
                            .eq(BinanceCoinFuturesTradeInfo::getPositionSide, "SHORT")
                            .orderByAsc(BinanceCoinFuturesTradeInfo::getTime)
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
                    BinanceCoinFuturesTradeInfo::getBaseQty,
                    BinanceCoinFuturesTradeInfo::setBaseQty,
                    BinanceCoinFuturesTradeInfo::getPrice,
                    BinanceCoinFuturesTradeInfo::getTime,
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
                statsInfoVO.setPosAmount(openList.stream().map(v -> v.getBaseQty().multiply(v.getPrice())).reduce(BigDecimal.ZERO, BigDecimal::add));
                // 持仓数量
                statsInfoVO.setPosQty(openList.stream().map(BinanceCoinFuturesTradeInfo::getBaseQty).reduce(BigDecimal.ZERO, BigDecimal::add));
                // 持仓均价
                statsInfoVO.setPosAvgPrice(statsInfoVO.getPosAmount().divide(statsInfoVO.getPosQty(), 8, RoundingMode.HALF_UP));
            }

        }


        // 现货止损交易匹配依赖实时价格；价格不可用时保留本地历史统计
        if (currentPrice != null) {
            List<MatchedTradeInfo> matchedList = TradeMatcherUtil.matchTrades(side, "0.001", openList, BinanceCoinFuturesTradeInfo::getBaseQty, BinanceCoinFuturesTradeInfo::getPrice, currentPrice,
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
        statsInfoVO.setFundingFee(currentPrice == null || fundingFee == null ? null : fundingFee.multiply(currentPrice));

        return statsInfoVO;
    }

    private BinanceCoinFuturesTradeInfo copyTrade(BinanceCoinFuturesTradeInfo source) {
        BinanceCoinFuturesTradeInfo target = new BinanceCoinFuturesTradeInfo();
        target.copy(source);
        return target;
    }

    @Override
    public BigDecimal calculatePositionFundingFee(Integer uid, BinanceEnum.SYMBOL symbol) {
        return calculatePositionFundingFee(uid, symbol, getLastPosCloseTime(uid));
    }

    @Override
    public BigDecimal calculatePositionFundingFee(Integer uid, BinanceEnum.SYMBOL symbol, Date startTime) {
        String incomeType = "FUNDING_FEE";
        List<JSONObject> list = binanceCoinFuturesUtil.listIncome(symbol, startTime.getTime(), incomeType);
        return list.stream().map(v -> v.getBigDecimal("income")).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

}
