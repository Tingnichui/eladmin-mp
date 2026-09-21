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

import cn.hutool.core.util.NumberUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.exception.BadRequestException;
import me.zhengjie.invest.constants.BinanceEnum;
import me.zhengjie.invest.domain.BinanceAccountInfo;
import me.zhengjie.invest.domain.BinanceSpotTradeMatchState;
import me.zhengjie.invest.domain.BinanceTradeInfo;
import me.zhengjie.invest.domain.dto.BinanceOrderApiDto;
import me.zhengjie.invest.domain.dto.MatchedTradeInfo;
import me.zhengjie.invest.domain.dto.BinanceOrderVO;
import me.zhengjie.invest.domain.dto.BinanceSpotHedgedTradeStatsInfoVO;
import me.zhengjie.invest.domain.dto.BinanceSpotTradeStatsAggregate;
import me.zhengjie.invest.domain.dto.BinanceSpotOrderRequest;
import me.zhengjie.invest.domain.dto.BinanceSpotOpenOrderDto;
import me.zhengjie.invest.domain.dto.BinanceSpotSellSourceDto;
import me.zhengjie.invest.domain.dto.BinanceSpotSellSourceReconcileResult;
import me.zhengjie.invest.domain.dto.BinanceTradeInfoQueryCriteria;
import me.zhengjie.invest.domain.dto.BinanceTradeStatsInfoVO;
import me.zhengjie.invest.mapper.BinanceTradeInfoMapper;
import me.zhengjie.invest.mapper.BinanceSpotTradeMatchMapper;
import me.zhengjie.invest.mapper.BinanceSpotTradeMatchStateMapper;
import me.zhengjie.invest.service.BinanceAccountInfoService;
import me.zhengjie.invest.service.BinanceSpotTradeMatcherService;
import me.zhengjie.invest.service.BinanceTradeInfoExtService;
import me.zhengjie.invest.service.BinanceTradeInfoService;
import me.zhengjie.invest.service.support.BinanceSpotHedgeContext;
import me.zhengjie.invest.util.BinanceAccountContextHolder;
import me.zhengjie.invest.util.BinanceSpotUtil;
import me.zhengjie.invest.util.BinanceUsdFuturesUtil;
import me.zhengjie.utils.FileUtil;
import me.zhengjie.utils.PageResult;
import me.zhengjie.utils.PageUtil;
import me.zhengjie.utils.RedisUtils;
import me.zhengjie.utils.enums.OrderDirectionEnum;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author genghui
 * @description 服务实现
 * @date 2025-07-05
 **/
@Service
@RequiredArgsConstructor
@Slf4j
public class BinanceTradeInfoServiceImpl extends ServiceImpl<BinanceTradeInfoMapper, BinanceTradeInfo> implements BinanceTradeInfoService {

    private static final int SYNC_PAGE_SIZE = 1000;
    private static final String SPOT_SELL_SOURCE_KEY_PREFIX = "BINANCE:SPOT:SELL_SOURCE:";
    private static final String SPOT_SELL_SOURCE_INDEX_KEY_PREFIX = "BINANCE:SPOT:SELL_SOURCE:INDEX:";
    private static final String SPOT_SELL_SOURCE_INDEXED_KEY_PREFIX = "BINANCE:SPOT:SELL_SOURCE:INDEXED:";
    private static final long SPOT_SELL_SOURCE_TTL_DAYS = 365L;
    private static final long SPOT_SELL_SOURCE_TTL_SECONDS = TimeUnit.DAYS.toSeconds(SPOT_SELL_SOURCE_TTL_DAYS);
    private static final Set<String> SPOT_SELL_SOURCE_REMOVE_STATUSES =
            new HashSet<>(Arrays.asList("CANCELED", "EXPIRED", "EXPIRED_IN_MATCH", "REJECTED"));

    @Resource
    private BinanceTradeInfoMapper binanceTradeInfoMapper;
    @Resource
    private BinanceSpotTradeMatchMapper binanceSpotTradeMatchMapper;
    @Resource
    private BinanceSpotTradeMatchStateMapper binanceSpotTradeMatchStateMapper;
    @Resource
    private BinanceAccountInfoService binanceAccountInfoService;
    @Resource
    private BinanceSpotUtil binanceSpotUtil;
    @Resource
    private BinanceSpotTradeMatcherService binanceSpotTradeMatcherService;
    @Resource
    private BinanceTradeInfoExtService binanceTradeInfoExtService;
    @Resource
    private BinanceUsdFuturesUtil binanceUsdFuturesUtil;
    @Resource
    private RedisUtils redisUtils;

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
            syncTradeInfo(accountInfo, symbol);
        }
    }

    @Override
    public int syncTradeInfo(BinanceAccountInfo accountInfo, String symbol) {
        int[] syncedCount = {0};
        BinanceAccountContextHolder.runWith(accountInfo, () -> {
            List<BinanceTradeInfo> latestTrades = this.list(
                    Wrappers.<BinanceTradeInfo>query()
                            .select("id")
                            .eq("uid", accountInfo.getUid())
                            .eq("symbol", symbol)
                            .orderByDesc("id")
                            .last("limit 1")
            );
            long fromId = latestTrades.isEmpty() ? 0L : latestTrades.get(0).getId() + 1L;
            while (true) {
                List<BinanceTradeInfo> orderInfoList = binanceSpotUtil.getMyTrades(
                        symbol, fromId, SYNC_PAGE_SIZE);
                if (CollectionUtils.isEmpty(orderInfoList)) {
                    break;
                }
                Set<Long> pageIds = orderInfoList.stream().map(BinanceTradeInfo::getId)
                        .filter(Objects::nonNull).collect(Collectors.toSet());
                if (pageIds.isEmpty()) {
                    throw new IllegalStateException("币安现货成交数据缺少 ID");
                }
                Set<Long> existIdSet = this.list(
                        Wrappers.<BinanceTradeInfo>query()
                                .select("id")
                                .eq("uid", accountInfo.getUid())
                                .eq("symbol", symbol)
                                .in("id", pageIds)
                ).stream().map(BinanceTradeInfo::getId).collect(Collectors.toSet());
                List<BinanceTradeInfo> newOrders = orderInfoList.stream()
                        .filter(order -> order.getId() != null && !existIdSet.contains(order.getId()))
                        .peek(order -> order.setUid(accountInfo.getUid()))
                        .collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(newOrders)) {
                    this.saveOrUpdateBatch(newOrders);
                    syncedCount[0] += newOrders.size();
                }
                long nextFromId = Collections.max(pageIds) + 1L;
                if (nextFromId <= fromId) {
                    throw new IllegalStateException("币安现货成交分页未向前推进");
                }
                fromId = nextFromId;
                if (orderInfoList.size() < SYNC_PAGE_SIZE) {
                    break;
                }
            }
        });
        binanceSpotTradeMatcherService.initializeAndMatch(accountInfo.getUid(), symbol);
        return syncedCount[0];
    }

    @Override
    public BinanceTradeStatsInfoVO stats(BinanceTradeInfoQueryCriteria criteria) {
        return stats(criteria, null, true);
    }

    @Override
    public BinanceTradeStatsInfoVO stats(BinanceTradeInfoQueryCriteria criteria, BigDecimal currentPrice) {
        return stats(criteria, currentPrice, false);
    }

    private BinanceTradeStatsInfoVO stats(BinanceTradeInfoQueryCriteria criteria,
                                          BigDecimal realtimePrice,
                                          boolean loadRealtimePrice) {
        final String key = "SPOT_LAST_NET_PNL:" + criteria.getUid() + ":" + criteria.getSymbol();
        BinanceTradeStatsInfoVO statsInfoVO = new BinanceTradeStatsInfoVO();
        statsInfoVO.setLastNetPnl((BigDecimal) redisUtils.get(key));
        final boolean side = true;
        final String feeRate = "0.001";
        BinanceSpotTradeStatsAggregate aggregate = binanceSpotTradeMatchMapper.aggregateStats(
                criteria.getUid(), criteria.getSymbol());
        if (aggregate == null) {
            aggregate = new BinanceSpotTradeStatsAggregate();
        }
        statsInfoVO.setTotalBuyAmount(zeroIfNull(aggregate.getTotalBuyAmount()));
        statsInfoVO.setTotalSellAmount(zeroIfNull(aggregate.getTotalSellAmount()));
        statsInfoVO.setPnl(zeroIfNull(aggregate.getPnl()));
        statsInfoVO.setFee(zeroIfNull(aggregate.getFee()));
        statsInfoVO.setNetPnl(zeroIfNull(aggregate.getNetPnl()));
        statsInfoVO.setRoi(statsInfoVO.getTotalBuyAmount().compareTo(BigDecimal.ZERO) > 0
                ? statsInfoVO.getNetPnl().divide(statsInfoVO.getTotalBuyAmount(), 8, RoundingMode.HALF_UP)
                : null);
        redisUtils.set(key, statsInfoVO.getNetPnl());

        BigDecimal unmatchedSellQty = zeroIfNull(binanceSpotTradeMatchStateMapper.sumStatsUnmatchedSellQty(
                criteria.getUid(), criteria.getSymbol()));
        statsInfoVO.setUnmatchedSellQty(unmatchedSellQty);
        if (unmatchedSellQty.compareTo(BigDecimal.ZERO) > 0) {
            addWarning(statsInfoVO, "部分卖出成交缺少可匹配的历史买入");
        }

        List<BinanceSpotTradeMatchState> openList = binanceSpotTradeMatchStateMapper.findStatsOpenBuys(
                criteria.getUid(), criteria.getSymbol());
        if (openList == null) {
            openList = Collections.emptyList();
        }
        BigDecimal totalWaitSellAmount = openList.stream()
                .map(b -> zeroIfNull(b.getRemainingQty()).multiply(zeroIfNull(b.getPrice())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        statsInfoVO.setPosAmount(totalWaitSellAmount);
        BigDecimal totalWaitSellQty = openList.stream()
                .map(BinanceSpotTradeMatchState::getRemainingQty)
                .map(this::zeroIfNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        statsInfoVO.setPosQty(totalWaitSellQty);
        statsInfoVO.setPosAvgPrice(totalWaitSellQty.compareTo(BigDecimal.ZERO) > 0
                ? totalWaitSellAmount.divide(totalWaitSellQty, 8, RoundingMode.HALF_UP)
                : null);

        if (openList.isEmpty()) {
            return statsInfoVO;
        }

        try {
            BigDecimal currentPrice = loadRealtimePrice
                    ? binanceSpotUtil.getPrice(BinanceEnum.SYMBOL.valueOf(criteria.getSymbol()))
                    : realtimePrice;
            if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalStateException("现货价格为空或无效");
            }
            statsInfoVO.setCurrentSpotPrice(currentPrice);


            List<MatchedTradeInfo> matchedTradeInfos = openList.stream().map(position -> {
                MatchedTradeInfo trade = new MatchedTradeInfo(side, feeRate);
                BigDecimal coreQty = zeroIfNull(position.getActiveCoreQty());
                trade.setTradeId(position.getTradeId());
                trade.setOrderId(position.getOrderId());
                trade.setCorePositionId(position.getCorePositionId());
                trade.setCoreQty(coreQty);
                trade.setAvailableQty(zeroIfNull(position.getRemainingQty()).subtract(coreQty));
                trade.setCoreLockedAt(position.getCoreLockedAt());
                trade.setQty(position.getRemainingQty());
                trade.setOpenPrice(position.getPrice());
                trade.setOpenTime(position.getTradeTime());
                trade.setClosePrice(currentPrice);
                return trade;
            }).collect(Collectors.toList());
            // 持仓盈利
            statsInfoVO.setHoldingProfit(matchedTradeInfos.stream().map(MatchedTradeInfo::getNetPnl).filter(netPnl -> netPnl.compareTo(BigDecimal.ZERO) > 0).reduce(BigDecimal.ZERO, BigDecimal::add));
            // 持仓亏损
            statsInfoVO.setHoldingLoss(matchedTradeInfos.stream().map(MatchedTradeInfo::getNetPnl).filter(netPnl -> netPnl.compareTo(BigDecimal.ZERO) <= 0).reduce(BigDecimal.ZERO, BigDecimal::add));
            // 持仓盈亏
            statsInfoVO.setHoldingProfitLoss(matchedTradeInfos.stream().map(MatchedTradeInfo::getNetPnl).reduce(BigDecimal.ZERO, BigDecimal::add));
            // 持仓订单
            // 按未平仓买入批次逐笔返回，保留真实成交时间；价格区间聚合由前端按需完成。
            statsInfoVO.setTradeList(matchedTradeInfos.stream()
                    .sorted(Comparator.comparing(MatchedTradeInfo::getOpenTime))
                    .collect(Collectors.toList()));

        } catch (Exception e) {
            statsInfoVO.setCurrentSpotPrice(null);
            statsInfoVO.setTradeList(Collections.emptyList());
            addWarning(statsInfoVO, "现货持仓实时估值暂不可用");
            log.warn("现货持仓实时估值失败: uid={}, symbol={}, error={}",
                    criteria.getUid(), criteria.getSymbol(), e.getClass().getSimpleName());
        }

        return statsInfoVO;
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private void addWarning(BinanceTradeStatsInfoVO statsInfo, String warning) {
        if (!statsInfo.getWarnings().contains(warning)) {
            statsInfo.getWarnings().add(warning);
        }
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
    public BinanceSpotHedgeContext createHedgeContext(Integer uid, String symbol) {
        BinanceTradeInfoQueryCriteria criteria = new BinanceTradeInfoQueryCriteria();
        criteria.setUid(uid);
        criteria.setSymbol(symbol);
        criteria.setIsBuyer(1);
        criteria.setOrderColumn("price");
        criteria.setOrderDirection(OrderDirectionEnum.ASC.getValue());
        return new BinanceSpotHedgeContext(binanceTradeInfoMapper.findAll(criteria));
    }

    @Override
    public BinanceSpotHedgedTradeStatsInfoVO hedgedStats(BinanceSpotHedgeContext hedgeContext) {
        BinanceSpotHedgedTradeStatsInfoVO statsInfo = new BinanceSpotHedgedTradeStatsInfoVO();

        List<BinanceTradeInfo> hedgedTradeInfo = hedgeContext.getHedgedTrades();
        if (CollectionUtils.isNotEmpty(hedgedTradeInfo)) {
            // 锁仓总额
            statsInfo.setPosAmount(hedgedTradeInfo.stream().map(BinanceTradeInfo::getHedgedAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
            // 锁仓数量
            statsInfo.setPosQty(hedgedTradeInfo.stream().map(BinanceTradeInfo::getHedgedQty).reduce(BigDecimal.ZERO, BigDecimal::add));
            // 锁仓均价
            statsInfo.setPosAvgPrice(NumberUtil.div(statsInfo.getPosAmount(), statsInfo.getPosQty()));
        }

        return statsInfo;
    }

    @Override
    public void createPos(BinanceOrderVO posInfo) {
        // 现货请求体
        BinanceOrderApiDto apiDto = new BinanceOrderApiDto();
        apiDto.setSymbol(BinanceEnum.SYMBOL.BTCUSDT.name());
        apiDto.setQuantity(posInfo.getPosQty());
        apiDto.setStopPrice(posInfo.getOpenPrice());

        // 做多
        if (posInfo.getPosDir()) {
            // 现货做多
            apiDto.setType(BinanceEnum.TYPE.STOP_LOSS);
            apiDto.setSide(BinanceEnum.SIDE.BUY);
            Long openOrderId = binanceSpotUtil.order(apiDto, 3);
            System.err.println(openOrderId);
            // 合约止损

        }
        // 做空
        else {
            // 合约做空
            // 现货止损

        }
    }

    @Override
    public Long createSpotOrder(BinanceSpotOrderRequest request) {
        BinanceEnum.SYMBOL symbol = requireSpotSymbol(request.getSymbol());
        if (request.getType() != BinanceEnum.TYPE.STOP_LOSS_LIMIT
                && request.getType() != BinanceEnum.TYPE.TAKE_PROFIT_LIMIT) {
            throw new BadRequestException("仅支持限价止盈或限价止损订单");
        }
        if (request.getPriceMode() == BinanceEnum.PRICE_MODE.FIXED && request.getPrice() == null) {
            throw new BadRequestException("固定委托价模式必须填写委托价");
        }
        if (request.getPriceMode() == BinanceEnum.PRICE_MODE.OPPONENT_FIRST && request.getPrice() != null) {
            throw new BadRequestException("对手价1模式不能填写固定委托价");
        }
        validateSpotSellSource(request);

        BinanceAccountInfo accountInfo = requireAvailableAccount(request.getUid());

        BinanceOrderApiDto apiDto = new BinanceOrderApiDto();
        apiDto.setSymbol(symbol.name());
        apiDto.setSide(request.getSide());
        apiDto.setType(request.getType());
        apiDto.setTimeInForce(BinanceEnum.TIME_IN_FORCE.GTC);
        apiDto.setQuantity(request.getQuantity());
        apiDto.setStopPrice(request.getStopPrice());
        if (request.getPriceMode() == BinanceEnum.PRICE_MODE.FIXED) {
            apiDto.setPrice(request.getPrice());
        } else {
            apiDto.setPegPriceType(BinanceEnum.PEG_PRICE_TYPE.MARKET_PEG);
        }

        Long[] orderId = new Long[1];
        BinanceAccountContextHolder.runWith(accountInfo, () -> {
            validateSpotSellCapacity(request, symbol.name());
            orderId[0] = binanceSpotUtil.order(apiDto);
        });
        recordSpotSellSource(request, symbol.name(), orderId[0]);
        return orderId[0];
    }

    @Override
    public List<BinanceSpotOpenOrderDto> listSpotOpenOrders(Integer uid, String symbolValue) {
        BinanceEnum.SYMBOL symbol = requireSpotSymbol(symbolValue);
        BinanceAccountInfo accountInfo = requireAvailableAccount(uid);
        AtomicReference<List<BinanceSpotOpenOrderDto>> result = new AtomicReference<>();
        BinanceAccountContextHolder.runWith(accountInfo,
                () -> result.set(binanceSpotUtil.listOpenOrders(symbol.name())));
        List<BinanceSpotOpenOrderDto> openOrders = result.get() == null
                ? Collections.emptyList() : result.get();
        openOrders.forEach(order -> enrichSpotSellSource(uid, symbol.name(), order));
        return openOrders;
    }

    @Override
    public Long cancelSpotOrder(Integer uid, String symbolValue, Long orderId) {
        if (orderId == null) {
            throw new BadRequestException("订单 ID 不能为空");
        }
        BinanceEnum.SYMBOL symbol = requireSpotSymbol(symbolValue);
        BinanceAccountInfo accountInfo = requireAvailableAccount(uid);
        Long[] canceledOrderId = new Long[1];
        BinanceAccountContextHolder.runWith(accountInfo,
                () -> canceledOrderId[0] = binanceSpotUtil.cancelOrder(symbol.name(), orderId));
        try {
            redisUtils.del(spotSellSourceKey(uid, symbol.name(), orderId));
            redisUtils.setRemove(spotSellSourceIndexKey(uid, symbol.name()), orderId);
        } catch (Exception e) {
            log.warn("币安现货撤单成功，但来源关联清理失败: uid={}, symbol={}, orderId={}, error={}",
                    uid, symbol.name(), orderId, e.getClass().getSimpleName());
        }
        return canceledOrderId[0];
    }

    @Override
    public BinanceSpotSellSourceReconcileResult reconcileSpotSellSources(Integer uid, String symbolValue) {
        BinanceEnum.SYMBOL symbol = requireSpotSymbol(symbolValue);
        BinanceAccountInfo accountInfo = requireAvailableAccount(uid);
        AtomicReference<List<BinanceSpotOpenOrderDto>> result = new AtomicReference<>();
        BinanceAccountContextHolder.runWith(accountInfo,
                () -> result.set(binanceSpotUtil.listOpenOrders(symbol.name())));
        List<BinanceSpotOpenOrderDto> openOrders = result.get() == null
                ? Collections.emptyList() : result.get();

        ensureSpotSellSourceIndex(uid, symbol.name());
        Set<Long> indexedOrderIds = spotSellSourceOrderIds(uid, symbol.name());
        Set<Long> openOrderIds = openOrders.stream()
                .map(BinanceSpotOpenOrderDto::getOrderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        BinanceSpotSellSourceReconcileResult reconcileResult = new BinanceSpotSellSourceReconcileResult();
        reconcileResult.setOpenCount(openOrderIds.size());

        for (BinanceSpotOpenOrderDto openOrder : openOrders) {
            if (openOrder.getOrderId() != null && indexedOrderIds.contains(openOrder.getOrderId())) {
                updateSpotSellSourceStatus(uid, symbol.name(), openOrder);
            }
        }
        for (Long orderId : indexedOrderIds) {
            if (openOrderIds.contains(orderId)) {
                continue;
            }
            reconcileResult.setCheckedCount(reconcileResult.getCheckedCount() + 1);
            try {
                AtomicReference<BinanceSpotOpenOrderDto> orderResult = new AtomicReference<>();
                BinanceAccountContextHolder.runWith(accountInfo,
                        () -> orderResult.set(binanceSpotUtil.queryOrder(symbol.name(), orderId)));
                reconcileMissingSpotSellSource(uid, symbol.name(), orderId,
                        orderResult.get(), reconcileResult);
            } catch (Exception e) {
                reconcileResult.setFailedCount(reconcileResult.getFailedCount() + 1);
                log.warn("查询币安现货卖出订单状态失败，保留来源关联: uid={}, symbol={}, orderId={}, error={}",
                        uid, symbol.name(), orderId, e.getClass().getSimpleName());
            }
        }
        return reconcileResult;
    }

    private void reconcileMissingSpotSellSource(Integer uid, String symbol, Long orderId,
                                                BinanceSpotOpenOrderDto order,
                                                BinanceSpotSellSourceReconcileResult result) {
        String status = order == null ? null : order.getStatus();
        if (SPOT_SELL_SOURCE_REMOVE_STATUSES.contains(status)) {
            redisUtils.del(spotSellSourceKey(uid, symbol, orderId));
            redisUtils.setRemove(spotSellSourceIndexKey(uid, symbol), orderId);
            result.setRemovedCount(result.getRemovedCount() + 1);
            return;
        }
        if ("FILLED".equals(status)) {
            updateSpotSellSourceStatus(uid, symbol, order);
            redisUtils.setRemove(spotSellSourceIndexKey(uid, symbol), orderId);
            result.setFilledCount(result.getFilledCount() + 1);
            return;
        }
        updateSpotSellSourceStatus(uid, symbol, order);
        result.setRetainedCount(result.getRetainedCount() + 1);
    }

    private void validateSpotSellCapacity(BinanceSpotOrderRequest request, String symbol) {
        if (request.getSourceType() == null) {
            return;
        }
        List<BinanceSpotTradeMatchState> positions =
                binanceSpotTradeMatchStateMapper.findStatsOpenBuys(request.getUid(), symbol);
        if (positions == null) {
            positions = Collections.emptyList();
        }
        BigDecimal sourceAvailable = positions.stream()
                .filter(position -> matchesSellSourcePosition(request, position))
                .map(position -> zeroIfNull(position.getRemainingQty())
                        .subtract(zeroIfNull(position.getActiveCoreQty())))
                .filter(quantity -> quantity.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sourceAvailable.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("该持仓没有可卖数量");
        }

        List<BinanceSpotOpenOrderDto> openOrders = binanceSpotUtil.listOpenOrders(symbol);
        if (openOrders == null) {
            openOrders = Collections.emptyList();
        }
        openOrders.forEach(order -> enrichSpotSellSource(request.getUid(), symbol, order));
        if (request.getSourceType() == BinanceSpotSellSourceDto.SourceType.TRADE
                && hasOrderLevelOpenSell(request.getSourceOrderId(), openOrders)) {
            throw new BadRequestException("该买入订单已有订单级卖出挂单，请先撤单");
        }
        BigDecimal pendingQty = openOrders.stream()
                .filter(order -> matchesSellSourceOrder(request, order))
                .map(this::openOrderRemainingQty)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remainingAvailable = sourceAvailable.subtract(pendingQty).max(BigDecimal.ZERO);
        if (request.getQuantity().compareTo(remainingAvailable) > 0) {
            throw new BadRequestException("卖出数量超过扣除当前挂单后的可卖数量 "
                    + remainingAvailable.stripTrailingZeros().toPlainString());
        }
    }

    private boolean matchesSellSourcePosition(BinanceSpotOrderRequest request,
                                              BinanceSpotTradeMatchState position) {
        if (request.getSourceType() == BinanceSpotSellSourceDto.SourceType.ORDER) {
            return Objects.equals(request.getSourceOrderId(), position.getOrderId());
        }
        return Objects.equals(request.getSourceTradeId(), position.getTradeId());
    }

    private boolean matchesSellSourceOrder(BinanceSpotOrderRequest request,
                                           BinanceSpotOpenOrderDto order) {
        if (request.getSourceType() == BinanceSpotSellSourceDto.SourceType.ORDER) {
            return Objects.equals(request.getSourceOrderId(), order.getSourceOrderId());
        }
        return order.getSourceType() == BinanceSpotSellSourceDto.SourceType.TRADE
                && Objects.equals(request.getSourceTradeId(), order.getSourceTradeId());
    }

    private boolean hasOrderLevelOpenSell(Long sourceOrderId, List<BinanceSpotOpenOrderDto> openOrders) {
        return sourceOrderId != null && openOrders.stream().anyMatch(order ->
                order.getSourceType() == BinanceSpotSellSourceDto.SourceType.ORDER
                        && Objects.equals(sourceOrderId, order.getSourceOrderId()));
    }

    private BigDecimal openOrderRemainingQty(BinanceSpotOpenOrderDto order) {
        return zeroIfNull(order.getOrigQty()).subtract(zeroIfNull(order.getExecutedQty())).max(BigDecimal.ZERO);
    }

    private void validateSpotSellSource(BinanceSpotOrderRequest request) {
        boolean hasSourceId = request.getSourceOrderId() != null || request.getSourceTradeId() != null;
        if (request.getSourceType() == null) {
            if (hasSourceId) {
                throw new BadRequestException("快捷卖出来源类型不能为空");
            }
            return;
        }
        if (request.getSide() != BinanceEnum.SIDE.SELL) {
            throw new BadRequestException("只有卖出订单可以关联持仓来源");
        }
        if (request.getSourceType() == BinanceSpotSellSourceDto.SourceType.ORDER
                && request.getSourceOrderId() == null) {
            throw new BadRequestException("订单来源必须包含原买入订单 ID");
        }
        if (request.getSourceType() == BinanceSpotSellSourceDto.SourceType.TRADE
                && request.getSourceTradeId() == null) {
            throw new BadRequestException("成交来源必须包含原买入成交 ID");
        }
    }

    private void recordSpotSellSource(BinanceSpotOrderRequest request, String symbol, Long sellOrderId) {
        if (request.getSourceType() == null || sellOrderId == null) {
            return;
        }
        BinanceSpotSellSourceDto source = new BinanceSpotSellSourceDto();
        source.setUid(request.getUid());
        source.setSymbol(symbol);
        source.setSellOrderId(sellOrderId);
        source.setSourceType(request.getSourceType());
        source.setSourceOrderId(request.getSourceOrderId());
        source.setSourceTradeId(request.getSourceTradeId());
        source.setQuantity(request.getQuantity());
        source.setCreatedAt(System.currentTimeMillis());
        source.setStatus("NEW");
        source.setExecutedQty(BigDecimal.ZERO);
        source.setUpdatedAt(System.currentTimeMillis());
        boolean recorded = redisUtils.set(spotSellSourceKey(request.getUid(), symbol, sellOrderId),
                source, SPOT_SELL_SOURCE_TTL_DAYS, TimeUnit.DAYS);
        if (!recorded) {
            log.warn("币安现货卖出已下单，但来源关联记录失败: uid={}, symbol={}, sellOrderId={}",
                    request.getUid(), symbol, sellOrderId);
            return;
        }
        addSpotSellSourceIndex(request.getUid(), symbol, sellOrderId);
    }

    private void enrichSpotSellSource(Integer uid, String symbol, BinanceSpotOpenOrderDto order) {
        if (order == null || order.getOrderId() == null) {
            return;
        }
        String key = spotSellSourceKey(uid, symbol, order.getOrderId());
        try {
            BinanceSpotSellSourceDto source = redisUtils.get(key, BinanceSpotSellSourceDto.class);
            if (source == null) {
                return;
            }
            order.setSourceType(source.getSourceType());
            order.setSourceOrderId(source.getSourceOrderId());
            order.setSourceTradeId(source.getSourceTradeId());
            redisUtils.expire(key, SPOT_SELL_SOURCE_TTL_DAYS, TimeUnit.DAYS);
        } catch (Exception e) {
            log.warn("读取币安现货卖出来源关联失败: uid={}, symbol={}, sellOrderId={}, error={}",
                    uid, symbol, order.getOrderId(), e.getClass().getSimpleName());
        }
    }

    private String spotSellSourceKey(Integer uid, String symbol, Long sellOrderId) {
        return SPOT_SELL_SOURCE_KEY_PREFIX + uid + ":" + symbol + ":" + sellOrderId;
    }

    private String spotSellSourceIndexKey(Integer uid, String symbol) {
        return SPOT_SELL_SOURCE_INDEX_KEY_PREFIX + uid + ":" + symbol;
    }

    private String spotSellSourceIndexedKey(Integer uid, String symbol) {
        return SPOT_SELL_SOURCE_INDEXED_KEY_PREFIX + uid + ":" + symbol;
    }

    private void ensureSpotSellSourceIndex(Integer uid, String symbol) {
        String indexedKey = spotSellSourceIndexedKey(uid, symbol);
        if (redisUtils.hasKey(indexedKey)) {
            return;
        }
        String prefix = SPOT_SELL_SOURCE_KEY_PREFIX + uid + ":" + symbol + ":";
        List<String> sourceKeys = redisUtils.scan(prefix + "*");
        boolean indexReady = true;
        if (sourceKeys != null) {
            for (String sourceKey : sourceKeys) {
                Long orderId = parseSpotSellSourceOrderId(prefix, sourceKey);
                if (orderId == null) {
                    continue;
                }
                BinanceSpotSellSourceDto source = redisUtils.get(sourceKey, BinanceSpotSellSourceDto.class);
                if (source == null || "FILLED".equals(source.getStatus())) {
                    continue;
                }
                String indexKey = spotSellSourceIndexKey(uid, symbol);
                redisUtils.sSetAndTime(indexKey, SPOT_SELL_SOURCE_TTL_SECONDS, orderId);
                if (!redisUtils.sHasKey(indexKey, orderId)) {
                    indexReady = false;
                    log.warn("币安现货卖出来源活动索引写入失败: uid={}, symbol={}, sellOrderId={}",
                            uid, symbol, orderId);
                }
            }
        }
        if (indexReady) {
            redisUtils.set(indexedKey, true, SPOT_SELL_SOURCE_TTL_DAYS, TimeUnit.DAYS);
        }
    }

    private void addSpotSellSourceIndex(Integer uid, String symbol, Long sellOrderId) {
        String indexKey = spotSellSourceIndexKey(uid, symbol);
        try {
            redisUtils.sSetAndTime(indexKey, SPOT_SELL_SOURCE_TTL_SECONDS, sellOrderId);
            if (!redisUtils.sHasKey(indexKey, sellOrderId)) {
                redisUtils.del(spotSellSourceIndexedKey(uid, symbol));
                log.warn("币安现货卖出来源已记录，但活动索引写入失败: uid={}, symbol={}, sellOrderId={}",
                        uid, symbol, sellOrderId);
            }
        } catch (Exception e) {
            log.warn("币安现货卖出来源已记录，但活动索引维护失败: uid={}, symbol={}, sellOrderId={}, error={}",
                    uid, symbol, sellOrderId, e.getClass().getSimpleName());
        }
    }

    private Set<Long> spotSellSourceOrderIds(Integer uid, String symbol) {
        Set<Object> values = redisUtils.sGet(spotSellSourceIndexKey(uid, symbol));
        if (values == null || values.isEmpty()) {
            return Collections.emptySet();
        }
        return values.stream()
                .map(this::toLong)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Long parseSpotSellSourceOrderId(String prefix, String key) {
        if (key == null || !key.startsWith(prefix)) {
            return null;
        }
        return toLong(key.substring(prefix.length()));
    }

    private Long toLong(Object value) {
        try {
            return value == null ? null : Long.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void updateSpotSellSourceStatus(Integer uid, String symbol, BinanceSpotOpenOrderDto order) {
        if (order == null || order.getOrderId() == null) {
            return;
        }
        String key = spotSellSourceKey(uid, symbol, order.getOrderId());
        BinanceSpotSellSourceDto source = redisUtils.get(key, BinanceSpotSellSourceDto.class);
        if (source == null) {
            return;
        }
        source.setStatus(order.getStatus());
        source.setExecutedQty(order.getExecutedQty());
        source.setUpdatedAt(System.currentTimeMillis());
        redisUtils.set(key, source, SPOT_SELL_SOURCE_TTL_DAYS, TimeUnit.DAYS);
    }

    private BinanceEnum.SYMBOL requireSpotSymbol(String symbolValue) {
        BinanceEnum.SYMBOL symbol;
        try {
            symbol = BinanceEnum.SYMBOL.valueOf(symbolValue.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BadRequestException("不支持的交易对");
        }
        if (!Integer.valueOf(0).equals(symbol.getType())) {
            throw new BadRequestException("请选择现货交易对");
        }
        return symbol;
    }

    private BinanceAccountInfo requireAvailableAccount(Integer uid) {
        BinanceAccountInfo accountInfo = binanceAccountInfoService.getAccountByUid(uid);
        if (accountInfo == null || !Integer.valueOf(1).equals(accountInfo.getApiValidFlag())) {
            throw new BadRequestException("账户API不可用");
        }
        return accountInfo;
    }

}
