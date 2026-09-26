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
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.invest.constants.BinanceEnum;
import me.zhengjie.invest.domain.BinanceAccountInfo;
import me.zhengjie.invest.domain.BinanceCoinFuturesTradeInfo;
import me.zhengjie.invest.domain.dto.BinanceCoinFuturesAccountInfo;
import me.zhengjie.invest.domain.dto.BinanceCoinFuturesClosedSummaryVO;
import me.zhengjie.invest.domain.dto.BinanceCoinFuturesContractInfo;
import me.zhengjie.invest.domain.dto.BinanceCoinFuturesOpenTradeInfo;
import me.zhengjie.invest.domain.dto.BinanceCoinFuturesOrderDto;
import me.zhengjie.invest.domain.dto.BinanceCoinFuturesOrderRequest;
import me.zhengjie.invest.domain.dto.BinanceCoinFuturesPositionInfo;
import me.zhengjie.invest.domain.dto.BinanceCoinFuturesPositionStatsVO;
import me.zhengjie.invest.domain.dto.BinanceCoinFuturesStatsInfoVO;
import me.zhengjie.invest.domain.dto.BinanceCoinFuturesTradeSummary;
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
import me.zhengjie.exception.BadRequestException;
import me.zhengjie.utils.PageResult;
import me.zhengjie.utils.PageUtil;
import me.zhengjie.utils.RedisUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * @author genghui
 * @description 服务实现
 * @date 2025-11-23
 **/
@Service
@RequiredArgsConstructor
@Slf4j
public class BinanceCoinFuturesTradeInfoServiceImpl extends ServiceImpl<BinanceCoinFuturesTradeInfoMapper, BinanceCoinFuturesTradeInfo> implements BinanceCoinFuturesTradeInfoService {

    private static final int SYNC_PAGE_SIZE = 1000;
    private static final int INCOME_PAGE_SIZE = 1000;
    private static final long INCOME_MAX_INTERVAL_MILLIS = 365L * 24 * 60 * 60 * 1000;
    private static final long CLOSED_SUMMARY_CACHE_MINUTES = 30L;
    private static final String CLOSED_SUMMARY_CACHE_PREFIX = "BINANCE:COIN_FUTURES:CLOSED_SUMMARY:";
    private static final long POSITION_REQUEST_TIMEOUT_MILLIS = 5_000L;
    private static final long CONTRACT_CACHE_MINUTES = 60L;
    private static final long POSITION_STALE_CACHE_MINUTES = 10L;
    private static final String CONTRACT_CACHE_PREFIX = "BINANCE:COIN_FUTURES:CONTRACT:";
    private static final String POSITION_CACHE_PREFIX = "BINANCE:COIN_FUTURES:POSITION:";
    private static final String MARK_PRICE_CACHE_PREFIX = "BINANCE:COIN_FUTURES:MARK_PRICE:";
    private static final Set<String> ORDER_ACTIONS = new HashSet<>(Arrays.asList("OPEN", "CLOSE"));
    private static final Set<String> ORDER_POSITION_SIDES = new HashSet<>(Arrays.asList("LONG", "SHORT"));
    private static final Set<String> ORDER_TYPES = new HashSet<>(Arrays.asList("MARKET", "LIMIT"));
    private static final Set<String> TIME_IN_FORCE_VALUES = new HashSet<>(Arrays.asList("GTC", "IOC", "FOK", "GTX"));

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
    @Resource
    @Qualifier("binanceStatsExecutor")
    private AsyncTaskExecutor binanceStatsExecutor;

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
        return sync(accountInfo, BinanceEnum.SYMBOL.BTCUSD_PERP.name());
    }

    @Override
    public int sync(BinanceAccountInfo accountInfo, String symbol) {
        int[] syncedCount = {0};
        BinanceAccountContextHolder.runWith(accountInfo, () -> {
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
                        symbol, fromId, SYNC_PAGE_SIZE);
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
    public BinanceCoinFuturesOrderDto placeOrder(BinanceCoinFuturesOrderRequest request) {
        String symbol = normalizeOrderValue(request.getSymbol(), "合约");
        String action = normalizeOrderValue(request.getAction(), "开平仓动作");
        String intendedPositionSide = normalizeOrderValue(request.getPositionSide(), "持仓方向");
        String type = normalizeOrderValue(request.getType(), "订单类型");
        if (!"BTCUSD_PERP".equals(symbol)) {
            throw new BadRequestException("当前仅支持 BTCUSD_PERP");
        }
        if (!ORDER_ACTIONS.contains(action)) {
            throw new BadRequestException("开平仓动作必须是 OPEN 或 CLOSE");
        }
        if (!ORDER_POSITION_SIDES.contains(intendedPositionSide)) {
            throw new BadRequestException("持仓方向必须是 LONG 或 SHORT");
        }
        if (!ORDER_TYPES.contains(type)) {
            throw new BadRequestException("当前仅支持 MARKET 或 LIMIT 订单");
        }
        if (request.getQuantity() == null || request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("下单张数必须大于 0");
        }
        String timeInForce = normalizeTimeInForce(request.getTimeInForce(), type);
        if ("LIMIT".equals(type) && (request.getPrice() == null
                || request.getPrice().compareTo(BigDecimal.ZERO) <= 0)) {
            throw new BadRequestException("限价单必须填写大于 0 的委托价");
        }
        if ("MARKET".equals(type) && request.getPrice() != null) {
            throw new BadRequestException("市价单不能填写委托价");
        }

        JSONObject contract = binanceCoinFuturesUtil.contractInfo(symbol);
        validateContractOrder(contract, request.getQuantity(), request.getPrice(), type);
        boolean hedgeMode = binanceCoinFuturesUtil.isHedgeMode();
        if ("CLOSE".equals(action)) {
            validateClosePosition(symbol, intendedPositionSide, request.getQuantity(), hedgeMode);
        }

        String side = resolveOrderSide(action, intendedPositionSide);
        String clientOrderId = buildClientOrderId(request.getUid());
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("symbol", symbol);
        params.put("side", side);
        params.put("positionSide", hedgeMode ? intendedPositionSide : "BOTH");
        params.put("type", type);
        params.put("quantity", request.getQuantity().stripTrailingZeros().toPlainString());
        params.put("newClientOrderId", clientOrderId);
        params.put("newOrderRespType", "RESULT");
        if ("LIMIT".equals(type)) {
            params.put("price", request.getPrice().stripTrailingZeros().toPlainString());
            params.put("timeInForce", timeInForce);
        }
        if (!hedgeMode && "CLOSE".equals(action)) {
            params.put("reduceOnly", "true");
        }
        try {
            return binanceCoinFuturesUtil.placeOrder(params);
        } catch (RuntimeException placeError) {
            try {
                BinanceCoinFuturesOrderDto reconciled = binanceCoinFuturesUtil.queryOrder(symbol, clientOrderId);
                if (reconciled != null && reconciled.getOrderId() != null) {
                    return reconciled;
                }
            } catch (RuntimeException queryError) {
                placeError.addSuppressed(queryError);
            }
            throw placeError;
        }
    }

    @Override
    public List<BinanceCoinFuturesOrderDto> listOpenOrders(String symbol) {
        return binanceCoinFuturesUtil.listOpenOrders(requireSupportedOrderSymbol(symbol));
    }

    @Override
    public BinanceCoinFuturesOrderDto queryOrder(String symbol, Long orderId) {
        if (orderId == null) {
            throw new BadRequestException("订单 ID 不能为空");
        }
        return binanceCoinFuturesUtil.queryOrder(requireSupportedOrderSymbol(symbol), orderId);
    }

    @Override
    public BinanceCoinFuturesOrderDto cancelOrder(String symbol, Long orderId) {
        if (orderId == null) {
            throw new BadRequestException("订单 ID 不能为空");
        }
        return binanceCoinFuturesUtil.cancelOrder(requireSupportedOrderSymbol(symbol), orderId);
    }

    private String normalizeOrderValue(String value, String fieldName) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new BadRequestException(fieldName + "不能为空");
        }
        return normalized;
    }

    private String requireSupportedOrderSymbol(String symbol) {
        String normalized = normalizeOrderValue(symbol, "合约");
        if (!"BTCUSD_PERP".equals(normalized)) {
            throw new BadRequestException("当前仅支持 BTCUSD_PERP");
        }
        return normalized;
    }

    private String normalizeTimeInForce(String value, String type) {
        if (!"LIMIT".equals(type)) {
            return null;
        }
        String normalized = value == null || value.trim().isEmpty()
                ? "GTC" : value.trim().toUpperCase(Locale.ROOT);
        if (!TIME_IN_FORCE_VALUES.contains(normalized)) {
            throw new BadRequestException("限价单有效方式必须是 GTC、IOC、FOK 或 GTX");
        }
        return normalized;
    }

    private String resolveOrderSide(String action, String positionSide) {
        boolean buy = ("OPEN".equals(action) && "LONG".equals(positionSide))
                || ("CLOSE".equals(action) && "SHORT".equals(positionSide));
        return buy ? "BUY" : "SELL";
    }

    private String buildClientOrderId(Integer uid) {
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return "elc-" + uid + "-" + System.currentTimeMillis() + "-" + random;
    }

    private void validateClosePosition(String symbol, String intendedPositionSide,
                                       BigDecimal quantity, boolean hedgeMode) {
        List<JSONObject> positions = binanceCoinFuturesUtil.positionRisk(symbol);
        JSONObject matched = positions == null ? null : positions.stream()
                .filter(Objects::nonNull)
                .filter(item -> (hedgeMode ? intendedPositionSide : "BOTH")
                        .equalsIgnoreCase(item.getString("positionSide")))
                .findFirst().orElse(null);
        BigDecimal amount = matched == null ? BigDecimal.ZERO : matched.getBigDecimal("positionAmt");
        amount = amount == null ? BigDecimal.ZERO : amount;
        if (!hedgeMode) {
            boolean directionMatches = (amount.signum() > 0 && "LONG".equals(intendedPositionSide))
                    || (amount.signum() < 0 && "SHORT".equals(intendedPositionSide));
            if (!directionMatches) {
                throw new BadRequestException("当前没有可平的" + ("LONG".equals(intendedPositionSide) ? "多仓" : "空仓"));
            }
        } else if (amount.signum() == 0) {
            throw new BadRequestException("当前没有可平的" + ("LONG".equals(intendedPositionSide) ? "多仓" : "空仓"));
        }
        if (quantity.compareTo(amount.abs()) > 0) {
            throw new BadRequestException("平仓张数不能超过当前持仓 " + amount.abs().stripTrailingZeros().toPlainString() + " 张");
        }
    }

    private void validateContractOrder(JSONObject contract, BigDecimal quantity, BigDecimal price, String type) {
        if (contract == null || !"TRADING".equalsIgnoreCase(contract.getString("contractStatus"))) {
            throw new BadRequestException("当前合约不可交易");
        }
        JSONArray filters = contract.getJSONArray("filters");
        validateFilter(filters, "MARKET".equals(type) ? "MARKET_LOT_SIZE" : "LOT_SIZE", quantity, "下单张数");
        if ("LIMIT".equals(type)) {
            validateFilter(filters, "PRICE_FILTER", price, "委托价");
        }
    }

    private void validateFilter(JSONArray filters, String filterType, BigDecimal value, String fieldName) {
        if (filters == null || value == null) {
            return;
        }
        JSONObject filter = filters.stream().map(JSONObject.class::cast)
                .filter(item -> filterType.equals(item.getString("filterType")))
                .findFirst().orElse(null);
        if (filter == null) {
            return;
        }
        String minKey = "PRICE_FILTER".equals(filterType) ? "minPrice" : "minQty";
        String maxKey = "PRICE_FILTER".equals(filterType) ? "maxPrice" : "maxQty";
        String stepKey = "PRICE_FILTER".equals(filterType) ? "tickSize" : "stepSize";
        BigDecimal min = filter.getBigDecimal(minKey);
        BigDecimal max = filter.getBigDecimal(maxKey);
        BigDecimal step = filter.getBigDecimal(stepKey);
        if (min != null && min.signum() > 0 && value.compareTo(min) < 0) {
            throw new BadRequestException(fieldName + "不能小于 " + min.stripTrailingZeros().toPlainString());
        }
        if (max != null && max.signum() > 0 && value.compareTo(max) > 0) {
            throw new BadRequestException(fieldName + "不能大于 " + max.stripTrailingZeros().toPlainString());
        }
        if (step != null && step.signum() > 0 && value.remainder(step).compareTo(BigDecimal.ZERO) != 0) {
            throw new BadRequestException(fieldName + "必须是 " + step.stripTrailingZeros().toPlainString() + " 的整数倍");
        }
    }

    @Override
    public BinanceCoinFuturesStatsInfoVO queryStats(Integer uid, String symbol, String positionSide) {
        BinanceCoinFuturesPositionStatsVO positionStats = queryPositionStats(uid, symbol, positionSide);
        BinanceCoinFuturesClosedSummaryVO closedSummary = queryClosedSummary(uid, symbol, positionSide);
        BinanceCoinFuturesStatsInfoVO result = new BinanceCoinFuturesStatsInfoVO();
        result.setPositionInfo(positionStats.getPositionInfo());
        result.setContractInfo(positionStats.getContractInfo());
        result.setTradeList(positionStats.getTradeList());
        result.setAccountInfo(queryAccountAssets(symbol));
        result.setTradeSummary(closedSummary.getTradeSummary());
        result.getTradeSummary().setOpenTradeCount(positionStats.getTradeList().size());
        result.getWarnings().addAll(positionStats.getWarnings());
        closedSummary.getWarnings().stream()
                .filter(warning -> !result.getWarnings().contains(warning))
                .forEach(result.getWarnings()::add);
        return result;
    }

    @Override
    public BinanceCoinFuturesPositionStatsVO queryPositionStats(Integer uid, String symbol, String positionSide) {
        BinanceCoinFuturesPositionStatsVO result = new BinanceCoinFuturesPositionStatsVO();
        long deadlineNanos = System.nanoTime()
                + TimeUnit.MILLISECONDS.toNanos(POSITION_REQUEST_TIMEOUT_MILLIS);
        BinanceAccountInfo accountInfo = BinanceAccountContextHolder.get();
        String contractCacheKey = CONTRACT_CACHE_PREFIX + symbol;
        String positionCacheKey = POSITION_CACHE_PREFIX + uid + ":" + symbol + ":" + positionSide;
        String markPriceCacheKey = MARK_PRICE_CACHE_PREFIX + symbol;

        BinanceCoinFuturesContractInfo cachedContract = getRealtimeCache(
                contractCacheKey, BinanceCoinFuturesContractInfo.class, "合约规格");
        Future<JSONObject> contractFuture = cachedContract == null
                ? submitRealtime(accountInfo, () -> binanceCoinFuturesUtil.contractInfo(symbol)) : null;
        Future<List<JSONObject>> positionFuture = submitRealtime(
                accountInfo, () -> binanceCoinFuturesUtil.positionRisk(symbol));
        Future<JSONObject> premiumFuture = submitRealtime(
                accountInfo, () -> binanceCoinFuturesUtil.premiumIndex(symbol));

        BinanceCoinFuturesContractInfo contractInfo = cachedContract;
        if (contractInfo == null) {
            RealtimeResult<JSONObject> contractResult = awaitRealtime(
                    contractFuture, deadlineNanos, "合约规格", result.getWarnings());
            if (contractResult.getValue() != null) {
                contractInfo = toContractInfo(contractResult.getValue(), symbol);
                setRealtimeCache(contractCacheKey, contractInfo, CONTRACT_CACHE_MINUTES, "合约规格");
            }
        }
        if (contractInfo == null) {
            contractInfo = toContractInfo(null, symbol);
            result.getWarnings().add("合约规格暂不可用，逐笔数量使用默认规格推算");
        }
        result.setContractInfo(contractInfo);

        RealtimeResult<List<JSONObject>> positionResult = awaitRealtime(
                positionFuture, deadlineNanos, "实时仓位", result.getWarnings());
        JSONObject position = positionResult.getValue() == null ? null : positionResult.getValue().stream()
                .filter(item -> positionSide.equalsIgnoreCase(item.getString("positionSide")))
                .findFirst().orElse(null);
        BinanceCoinFuturesPositionInfo positionInfo = position == null ? null : toPositionInfo(position);
        if (positionInfo != null) {
            setRealtimeCache(positionCacheKey, positionInfo, POSITION_STALE_CACHE_MINUTES, "实时仓位");
        } else {
            positionInfo = getRealtimeCache(positionCacheKey, BinanceCoinFuturesPositionInfo.class, "实时仓位");
            if (positionInfo != null) {
                result.setRealtime(false);
                result.getWarnings().add("实时仓位暂不可用，当前展示最近一次缓存数据，已禁止下单");
            } else {
                positionInfo = emptyPosition(symbol, positionSide);
                result.setRealtime(false);
                result.getWarnings().add("实时仓位暂不可用，已禁止下单");
            }
        }

        RealtimeResult<JSONObject> premiumResult = awaitRealtime(
                premiumFuture, deadlineNanos, "标记价格", result.getWarnings());
        BigDecimal liveMarkPrice = decimal(premiumResult.getValue(), "markPrice");
        if (positive(liveMarkPrice)) {
            positionInfo.setMarkPrice(liveMarkPrice);
            setRealtimeCache(markPriceCacheKey, liveMarkPrice, POSITION_STALE_CACHE_MINUTES, "标记价格");
        } else if (!positive(positionInfo.getMarkPrice())) {
            BigDecimal cachedMarkPrice = getRealtimeCache(markPriceCacheKey, BigDecimal.class, "标记价格");
            if (positive(cachedMarkPrice)) {
                positionInfo.setMarkPrice(cachedMarkPrice);
                result.setRealtime(false);
                result.getWarnings().add("实时标记价格暂不可用，当前展示最近一次缓存数据，已禁止下单");
            } else {
                result.setRealtime(false);
                result.getWarnings().add("实时标记价格暂不可用，已禁止下单");
            }
        }
        result.setPositionInfo(positionInfo);

        List<BinanceCoinFuturesTradeInfo> allTrades = loadStatsTrades(uid, symbol, positionSide);
        PositionCycles positionCycles = splitPositionCycles(allTrades, positionSide);
        List<BinanceCoinFuturesTradeInfo> currentTrades = positionCycles.getCurrentTrades();
        if (!"BOTH".equals(positionSide) && currentTrades.isEmpty()
                && result.getPositionInfo().getPositionAmt().abs().compareTo(BigDecimal.ZERO) > 0) {
            result.getWarnings().add("本地缺少当前持仓周期成交，请先同步数据");
        }
        if ("BOTH".equals(positionSide)) {
            result.getWarnings().add("单向持仓模式暂不提供逐笔未平仓分布");
        } else {
            result.setTradeList(createOpenTradeList(currentTrades, positionSide,
                    result.getPositionInfo().getMarkPrice(), result.getContractInfo().getContractSize()));
            BigDecimal reconstructedQty = result.getTradeList().stream()
                    .map(BinanceCoinFuturesOpenTradeInfo::getContractQty)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (reconstructedQty.compareTo(result.getPositionInfo().getPositionAmt().abs()) != 0) {
                result.getWarnings().add("本地逐笔持仓与币安实时仓位不一致，请同步后复核");
            }
        }
        return result;
    }

    private BinanceCoinFuturesPositionInfo emptyPosition(String symbol, String positionSide) {
        BinanceCoinFuturesPositionInfo positionInfo = new BinanceCoinFuturesPositionInfo();
        positionInfo.setSymbol(symbol);
        positionInfo.setPositionSide(positionSide);
        return positionInfo;
    }

    private <T> Future<T> submitRealtime(BinanceAccountInfo accountInfo, Callable<T> callable) {
        Callable<T> task = () -> {
            try {
                BinanceAccountContextHolder.set(accountInfo);
                return callable.call();
            } finally {
                BinanceAccountContextHolder.clear();
            }
        };
        if (binanceStatsExecutor != null) {
            return binanceStatsExecutor.submit(task);
        }
        CompletableFuture<T> future = new CompletableFuture<>();
        try {
            future.complete(task.call());
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    private <T> RealtimeResult<T> awaitRealtime(Future<T> future, long deadlineNanos,
                                                  String label, List<String> warnings) {
        long remainingNanos = deadlineNanos - System.nanoTime();
        if (remainingNanos <= 0) {
            future.cancel(true);
            warnings.add(label + "请求超时");
            return new RealtimeResult<>(null);
        }
        try {
            return new RealtimeResult<>(future.get(remainingNanos, TimeUnit.NANOSECONDS));
        } catch (TimeoutException e) {
            future.cancel(true);
            warnings.add(label + "请求超时");
        } catch (Exception e) {
            warnings.add(label + "获取失败");
            log.warn("币本位实时统计获取失败: item={}, error={}", label, e.getClass().getSimpleName());
        }
        return new RealtimeResult<>(null);
    }

    private <T> T getRealtimeCache(String key, Class<T> type, String label) {
        if (redisUtils == null) {
            return null;
        }
        try {
            return redisUtils.get(key, type);
        } catch (RuntimeException e) {
            log.warn("读取币本位实时缓存失败: item={}, error={}", label, e.getClass().getSimpleName());
            return null;
        }
    }

    private void setRealtimeCache(String key, Object value, long minutes, String label) {
        if (redisUtils == null || value == null) {
            return;
        }
        try {
            redisUtils.set(key, value, minutes, TimeUnit.MINUTES);
        } catch (RuntimeException e) {
            log.warn("写入币本位实时缓存失败: item={}, error={}", label, e.getClass().getSimpleName());
        }
    }

    private static class RealtimeResult<T> {
        private final T value;

        private RealtimeResult(T value) {
            this.value = value;
        }

        private T getValue() {
            return value;
        }
    }

    @Override
    public BinanceCoinFuturesAccountInfo queryAccountAssets(String symbol) {
        return toAccountInfo(binanceCoinFuturesUtil.account(), marginAsset(symbol));
    }

    @Override
    public BinanceCoinFuturesClosedSummaryVO queryClosedSummary(Integer uid, String symbol, String positionSide) {
        BinanceCoinFuturesClosedSummaryVO result = new BinanceCoinFuturesClosedSummaryVO();
        PositionCycles positionCycles = splitPositionCycles(
                loadStatsTrades(uid, symbol, positionSide), positionSide);
        List<BinanceCoinFuturesTradeInfo> closedTrades = positionCycles.getClosedTrades();
        String cacheKey = closedSummaryCacheKey(uid, symbol, positionSide, closedTrades);
        BinanceCoinFuturesClosedSummaryVO cached = getClosedSummaryCache(cacheKey);
        if (cached != null) {
            return cached;
        }
        BigDecimal fundingFee = loadFundingFee(symbol, closedTrades, result.getWarnings());
        result.setTradeSummary(createTradeSummary(
                closedTrades, positionSide, fundingFee, marginAsset(symbol), result.getWarnings()));
        result.getTradeSummary().setClosedPositionCount(positionCycles.getClosedPositionCount());
        if (!closedTrades.isEmpty()) {
            result.setFirstTradeTime(closedTrades.get(0).getTime());
            result.setLastClosedTradeTime(closedTrades.get(closedTrades.size() - 1).getTime());
        }
        if ("BOTH".equals(positionSide)) {
            result.getWarnings().add("单向持仓模式暂不提供已平仓周期汇总");
        }
        if (result.getWarnings().isEmpty()) {
            setClosedSummaryCache(cacheKey, result);
        }
        return result;
    }

    private String closedSummaryCacheKey(Integer uid, String symbol, String positionSide,
                                         List<BinanceCoinFuturesTradeInfo> closedTrades) {
        Long lastClosedTradeId = closedTrades.isEmpty()
                ? 0L : closedTrades.get(closedTrades.size() - 1).getId();
        return CLOSED_SUMMARY_CACHE_PREFIX + uid + ":" + symbol + ":" + positionSide + ":" + lastClosedTradeId;
    }

    private BinanceCoinFuturesClosedSummaryVO getClosedSummaryCache(String cacheKey) {
        if (redisUtils == null) {
            return null;
        }
        try {
            return redisUtils.get(cacheKey, BinanceCoinFuturesClosedSummaryVO.class);
        } catch (RuntimeException e) {
            log.warn("读取币本位已平仓汇总缓存失败: key={}, error={}", cacheKey, e.getClass().getSimpleName());
            return null;
        }
    }

    private void setClosedSummaryCache(String cacheKey, BinanceCoinFuturesClosedSummaryVO value) {
        if (redisUtils == null) {
            return;
        }
        try {
            redisUtils.set(cacheKey, value, CLOSED_SUMMARY_CACHE_MINUTES, TimeUnit.MINUTES);
        } catch (RuntimeException e) {
            log.warn("写入币本位已平仓汇总缓存失败: key={}, error={}", cacheKey, e.getClass().getSimpleName());
        }
    }

    private List<BinanceCoinFuturesTradeInfo> loadStatsTrades(
            Integer uid, String symbol, String positionSide) {
        return binanceCoinFuturesTradeInfoMapper.selectList(
                Wrappers.lambdaQuery(BinanceCoinFuturesTradeInfo.class)
                        .eq(BinanceCoinFuturesTradeInfo::getUid, uid)
                        .eq(BinanceCoinFuturesTradeInfo::getSymbol, symbol)
                        .eq(BinanceCoinFuturesTradeInfo::getPositionSide, positionSide)
                        .orderByAsc(BinanceCoinFuturesTradeInfo::getTime)
                        .orderByAsc(BinanceCoinFuturesTradeInfo::getId)
        );
    }

    private String marginAsset(String symbol) {
        return symbol != null && symbol.startsWith("BTCUSD") ? "BTC" : null;
    }

    private BinanceCoinFuturesContractInfo toContractInfo(JSONObject source, String symbol) {
        BinanceCoinFuturesContractInfo target = new BinanceCoinFuturesContractInfo();
        target.setSymbol(symbol);
        if (source == null) {
            if (BinanceEnum.SYMBOL.BTCUSD_PERP.name().equals(symbol)) {
                target.setPair("BTCUSD");
                target.setBaseAsset("BTC");
                target.setQuoteAsset("USD");
                target.setMarginAsset("BTC");
            }
            return target;
        }
        target.setPair(source.getString("pair"));
        target.setContractType(source.getString("contractType"));
        target.setContractSize(decimal(source, "contractSize"));
        target.setBaseAsset(source.getString("baseAsset"));
        target.setQuoteAsset(source.getString("quoteAsset"));
        target.setMarginAsset(source.getString("marginAsset"));
        target.setPricePrecision(source.getInteger("pricePrecision"));
        target.setQuantityPrecision(source.getInteger("quantityPrecision"));
        return target;
    }

    private BinanceCoinFuturesPositionInfo toPositionInfo(JSONObject source) {
        BinanceCoinFuturesPositionInfo target = new BinanceCoinFuturesPositionInfo();
        target.setSymbol(source.getString("symbol"));
        target.setPositionSide(source.getString("positionSide"));
        target.setPositionAmt(decimal(source, "positionAmt"));
        target.setEntryPrice(decimal(source, "entryPrice"));
        target.setBreakEvenPrice(decimal(source, "breakEvenPrice"));
        target.setMarkPrice(decimal(source, "markPrice"));
        target.setNotionalValue(decimal(source, "notionalValue"));
        target.setUnrealizedPnl(decimal(source, "unRealizedProfit"));
        target.setLiquidationPrice(decimal(source, "liquidationPrice"));
        target.setLeverage(source.getInteger("leverage"));
        target.setMarginType(source.getString("marginType"));
        target.setAutoAddMargin(source.getBoolean("isAutoAddMargin"));
        target.setIsolatedMargin(decimal(source, "isolatedMargin"));
        target.setIsolatedWallet(decimal(source, "isolatedWallet"));
        target.setMaxQty(decimal(source, "maxQty"));
        target.setUpdateTime(source.getLong("updateTime"));
        return target;
    }

    private BinanceCoinFuturesAccountInfo toAccountInfo(JSONObject source, String marginAsset) {
        BinanceCoinFuturesAccountInfo target = new BinanceCoinFuturesAccountInfo();
        target.setAsset(marginAsset);
        if (source == null || source.getJSONArray("assets") == null) {
            return target;
        }
        JSONObject asset = source.getJSONArray("assets").stream()
                .map(JSONObject.class::cast)
                .filter(item -> marginAsset != null && marginAsset.equalsIgnoreCase(item.getString("asset")))
                .findFirst().orElse(null);
        if (asset == null) {
            return target;
        }
        target.setWalletBalance(decimal(asset, "walletBalance"));
        target.setUnrealizedProfit(decimal(asset, "unrealizedProfit"));
        target.setMarginBalance(decimal(asset, "marginBalance"));
        target.setAvailableBalance(decimal(asset, "availableBalance"));
        target.setMaxWithdrawAmount(decimal(asset, "maxWithdrawAmount"));
        target.setInitialMargin(decimal(asset, "initialMargin"));
        target.setMaintMargin(decimal(asset, "maintMargin"));
        target.setPositionInitialMargin(decimal(asset, "positionInitialMargin"));
        target.setOpenOrderInitialMargin(decimal(asset, "openOrderInitialMargin"));
        return target;
    }

    private PositionCycles splitPositionCycles(List<BinanceCoinFuturesTradeInfo> source, String positionSide) {
        if (source == null || source.isEmpty() || "BOTH".equals(positionSide)) {
            return PositionCycles.empty();
        }
        BigDecimal position = BigDecimal.ZERO;
        int lastFlatIndex = -1;
        int closedPositionCount = 0;
        for (int index = 0; index < source.size(); index++) {
            BinanceCoinFuturesTradeInfo trade = source.get(index);
            boolean increase = "LONG".equals(positionSide)
                    ? "BUY".equals(trade.getSide()) : "SELL".equals(trade.getSide());
            position = increase ? position.add(zeroIfNull(trade.getQty()))
                    : position.subtract(zeroIfNull(trade.getQty()));
            if (position.compareTo(BigDecimal.ZERO) == 0) {
                lastFlatIndex = index;
                closedPositionCount++;
            }
        }
        List<BinanceCoinFuturesTradeInfo> closedTrades = lastFlatIndex < 0
                ? Collections.emptyList() : new ArrayList<>(source.subList(0, lastFlatIndex + 1));
        List<BinanceCoinFuturesTradeInfo> currentTrades = new ArrayList<>(
                source.subList(lastFlatIndex + 1, source.size()));
        return new PositionCycles(closedTrades, currentTrades, closedPositionCount);
    }

    private BigDecimal loadFundingFee(String symbol, List<BinanceCoinFuturesTradeInfo> trades,
                                      List<String> warnings) {
        if (trades.isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            long startTime = trades.get(0).getTime().getTime();
            long endTime = trades.get(trades.size() - 1).getTime().getTime();
            BigDecimal fundingFee = BigDecimal.ZERO;
            Set<Long> transactionIds = new HashSet<>();
            long windowStart = startTime;
            while (windowStart <= endTime) {
                long windowEnd = Math.min(endTime, windowStart + INCOME_MAX_INTERVAL_MILLIS - 1);
                int page = 1;
                while (true) {
                    List<JSONObject> incomes = binanceCoinFuturesUtil.listIncome(
                            symbol, windowStart, windowEnd, "FUNDING_FEE", page, INCOME_PAGE_SIZE);
                    for (JSONObject income : incomes) {
                        Long transactionId = income.getLong("tranId");
                        if (transactionId == null || transactionIds.add(transactionId)) {
                            fundingFee = fundingFee.add(decimal(income, "income"));
                        }
                    }
                    if (incomes.size() < INCOME_PAGE_SIZE) {
                        break;
                    }
                    page++;
                }
                windowStart = windowEnd + 1;
            }
            return fundingFee;
        } catch (RuntimeException e) {
            warnings.add("全部已平仓周期资金费暂不可用");
            return BigDecimal.ZERO;
        }
    }

    private BinanceCoinFuturesTradeSummary createTradeSummary(List<BinanceCoinFuturesTradeInfo> trades,
                                                                String positionSide,
                                                                BigDecimal fundingFee,
                                                                String marginAsset,
                                                                List<String> warnings) {
        BinanceCoinFuturesTradeSummary summary = new BinanceCoinFuturesTradeSummary();
        BigDecimal realizedPnl = trades.stream().map(BinanceCoinFuturesTradeInfo::getRealizedPnl)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal commission = trades.stream()
                .filter(trade -> marginAsset == null || trade.getCommissionAsset() == null
                        || marginAsset.equalsIgnoreCase(trade.getCommissionAsset()))
                .map(BinanceCoinFuturesTradeInfo::getCommission)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add).abs();
        boolean otherCommissionAsset = trades.stream().anyMatch(trade -> trade.getCommissionAsset() != null
                && marginAsset != null && !marginAsset.equalsIgnoreCase(trade.getCommissionAsset()));
        if (otherCommissionAsset) {
            warnings.add("存在非保证金币种手续费，未计入净盈亏");
        }
        summary.setRealizedPnl(realizedPnl);
        summary.setCommission(commission);
        summary.setFundingFee(fundingFee);
        summary.setNetPnl(realizedPnl.add(fundingFee).subtract(commission));
        summary.setTotalTradeCount(trades.size());
        if (!"BOTH".equals(positionSide)) {
            String closeSide = "LONG".equals(positionSide) ? "SELL" : "BUY";
            summary.setClosedTradeCount((int) trades.stream()
                    .filter(trade -> closeSide.equals(trade.getSide())).count());
        }
        return summary;
    }

    private static class PositionCycles {
        private final List<BinanceCoinFuturesTradeInfo> closedTrades;
        private final List<BinanceCoinFuturesTradeInfo> currentTrades;
        private final int closedPositionCount;

        private PositionCycles(List<BinanceCoinFuturesTradeInfo> closedTrades,
                               List<BinanceCoinFuturesTradeInfo> currentTrades,
                               int closedPositionCount) {
            this.closedTrades = closedTrades;
            this.currentTrades = currentTrades;
            this.closedPositionCount = closedPositionCount;
        }

        private static PositionCycles empty() {
            return new PositionCycles(Collections.emptyList(), Collections.emptyList(), 0);
        }

        private List<BinanceCoinFuturesTradeInfo> getClosedTrades() {
            return closedTrades;
        }

        private List<BinanceCoinFuturesTradeInfo> getCurrentTrades() {
            return currentTrades;
        }

        private int getClosedPositionCount() {
            return closedPositionCount;
        }
    }

    private List<BinanceCoinFuturesOpenTradeInfo> createOpenTradeList(
            List<BinanceCoinFuturesTradeInfo> trades, String positionSide,
            BigDecimal markPrice, BigDecimal contractSize) {
        boolean longSide = "LONG".equals(positionSide);
        String openSide = longSide ? "BUY" : "SELL";
        List<BinanceCoinFuturesTradeInfo> openTrades = trades.stream()
                .filter(trade -> openSide.equals(trade.getSide()))
                .map(this::copyTrade).collect(Collectors.toCollection(ArrayList::new));
        List<BinanceCoinFuturesTradeInfo> closeTrades = trades.stream()
                .filter(trade -> !openSide.equals(trade.getSide()))
                .map(this::copyTrade).collect(Collectors.toCollection(ArrayList::new));
        TradeMatcherUtil.matchTradesFifo(
                longSide, "0", openTrades, closeTrades,
                BinanceCoinFuturesTradeInfo::getQty,
                BinanceCoinFuturesTradeInfo::setQty,
                BinanceCoinFuturesTradeInfo::getPrice,
                BinanceCoinFuturesTradeInfo::getTime,
                BinanceCoinFuturesTradeInfo::getId
        );
        List<BinanceCoinFuturesOpenTradeInfo> result = new ArrayList<>();
        for (BinanceCoinFuturesTradeInfo trade : openTrades) {
            if (!positive(trade.getQty()) || !positive(trade.getPrice())) {
                continue;
            }
            BinanceCoinFuturesOpenTradeInfo item = new BinanceCoinFuturesOpenTradeInfo();
            item.setTradeId(trade.getId());
            item.setOrderId(trade.getOrderId());
            item.setContractQty(trade.getQty());
            item.setOpenPrice(trade.getPrice());
            item.setOpenTime(trade.getTime());
            item.setMarkPrice(zeroIfNull(markPrice));
            BigDecimal effectiveContractSize = positive(contractSize)
                    ? contractSize : inferContractSize(trade);
            BigDecimal baseQty = effectiveContractSize.multiply(trade.getQty())
                    .divide(trade.getPrice(), 16, RoundingMode.HALF_UP);
            item.setBaseQty(baseQty);
            if (positive(markPrice) && positive(effectiveContractSize)) {
                BigDecimal openInverse = BigDecimal.ONE.divide(trade.getPrice(), 16, RoundingMode.HALF_UP);
                BigDecimal markInverse = BigDecimal.ONE.divide(markPrice, 16, RoundingMode.HALF_UP);
                BigDecimal pnl = effectiveContractSize.multiply(trade.getQty())
                        .multiply(longSide ? openInverse.subtract(markInverse)
                                : markInverse.subtract(openInverse));
                item.setUnrealizedPnl(pnl);
                item.setRoi(baseQty.compareTo(BigDecimal.ZERO) == 0 ? null
                        : pnl.divide(baseQty, 8, RoundingMode.HALF_UP));
            }
            result.add(item);
        }
        result.sort(Comparator.comparing(BinanceCoinFuturesOpenTradeInfo::getOpenPrice).reversed());
        return result;
    }

    private BigDecimal inferContractSize(BinanceCoinFuturesTradeInfo trade) {
        if (!positive(trade.getBaseQty()) || !positive(trade.getQty()) || !positive(trade.getPrice())) {
            return BigDecimal.ZERO;
        }
        return trade.getBaseQty().multiply(trade.getPrice())
                .divide(trade.getQty(), 8, RoundingMode.HALF_UP);
    }

    private BigDecimal decimal(JSONObject source, String key) {
        BigDecimal value = source == null ? null : source.getBigDecimal(key);
        return zeroIfNull(value);
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private boolean positive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
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
