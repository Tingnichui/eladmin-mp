package me.zhengjie.invest.service.impl;

import me.zhengjie.exception.BadRequestException;
import me.zhengjie.invest.constants.BinanceEnum;
import me.zhengjie.invest.domain.BinanceAccountInfo;
import me.zhengjie.invest.domain.BinanceSpotTradeMatchState;
import me.zhengjie.invest.domain.dto.BinanceOrderApiDto;
import me.zhengjie.invest.domain.dto.BinanceSpotOrderRequest;
import me.zhengjie.invest.domain.dto.BinanceSpotOpenOrderDto;
import me.zhengjie.invest.domain.dto.BinanceSpotSellSourceDto;
import me.zhengjie.invest.domain.dto.BinanceSpotSellSourceReconcileResult;
import me.zhengjie.invest.domain.dto.BinanceSpotTradeStatsAggregate;
import me.zhengjie.invest.domain.dto.BinanceTradeInfoQueryCriteria;
import me.zhengjie.invest.domain.dto.BinanceTradeStatsInfoVO;
import me.zhengjie.invest.domain.dto.MatchedTradeInfo;
import me.zhengjie.invest.mapper.BinanceSpotTradeMatchMapper;
import me.zhengjie.invest.mapper.BinanceSpotTradeMatchStateMapper;
import me.zhengjie.invest.mapper.BinanceTradeInfoMapper;
import me.zhengjie.invest.service.BinanceAccountInfoService;
import me.zhengjie.invest.util.BinanceAccountContextHolder;
import me.zhengjie.invest.util.BinanceSpotUtil;
import me.zhengjie.utils.RedisUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BinanceTradeInfoServiceImplTest {

    private final BinanceTradeInfoMapper mapper = mock(BinanceTradeInfoMapper.class);
    private final BinanceSpotTradeMatchMapper matchMapper = mock(BinanceSpotTradeMatchMapper.class);
    private final BinanceSpotTradeMatchStateMapper stateMapper = mock(BinanceSpotTradeMatchStateMapper.class);
    private final BinanceSpotUtil spotUtil = mock(BinanceSpotUtil.class);
    private final BinanceAccountInfoService accountService = mock(BinanceAccountInfoService.class);
    private final RedisUtils redisUtils = mock(RedisUtils.class);
    private final BinanceTradeInfoServiceImpl service = new BinanceTradeInfoServiceImpl();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "binanceTradeInfoMapper", mapper);
        ReflectionTestUtils.setField(service, "binanceSpotTradeMatchMapper", matchMapper);
        ReflectionTestUtils.setField(service, "binanceSpotTradeMatchStateMapper", stateMapper);
        ReflectionTestUtils.setField(service, "binanceSpotUtil", spotUtil);
        ReflectionTestUtils.setField(service, "binanceAccountInfoService", accountService);
        ReflectionTestUtils.setField(service, "redisUtils", redisUtils);
        when(matchMapper.aggregateStats(any(), any())).thenReturn(new BinanceSpotTradeStatsAggregate());
        when(stateMapper.sumStatsUnmatchedSellQty(any(), any())).thenReturn(BigDecimal.ZERO);
        when(stateMapper.findStatsOpenBuys(any(), any())).thenReturn(Collections.emptyList());
    }

    @Test
    void shouldCreateOpponentFirstStopLimitOrder() {
        BinanceAccountInfo account = validAccount();
        BinanceSpotOrderRequest request = spotOrderRequest();
        request.setPriceMode(BinanceEnum.PRICE_MODE.OPPONENT_FIRST);
        when(accountService.getAccountByUid(7)).thenReturn(account);
        when(spotUtil.order(any(BinanceOrderApiDto.class))).thenReturn(123L);

        Long orderId = service.createSpotOrder(request);

        ArgumentCaptor<BinanceOrderApiDto> captor = ArgumentCaptor.forClass(BinanceOrderApiDto.class);
        verify(spotUtil).order(captor.capture());
        BinanceOrderApiDto apiDto = captor.getValue();
        assertEquals(123L, orderId);
        assertEquals("BTCUSDT", apiDto.getSymbol());
        assertEquals(BinanceEnum.SIDE.SELL, apiDto.getSide());
        assertEquals(BinanceEnum.TYPE.STOP_LOSS_LIMIT, apiDto.getType());
        assertEquals(BinanceEnum.TIME_IN_FORCE.GTC, apiDto.getTimeInForce());
        assertEquals(new BigDecimal("0.001"), apiDto.getQuantity());
        assertEquals(new BigDecimal("80000"), apiDto.getStopPrice());
        assertEquals(BinanceEnum.PEG_PRICE_TYPE.MARKET_PEG, apiDto.getPegPriceType());
        assertNull(apiDto.getPrice());
        assertNull(BinanceAccountContextHolder.get());
    }

    @Test
    void shouldRecordOrderSourceAfterQuickSellSucceeds() {
        BinanceAccountInfo account = validAccount();
        BinanceSpotOrderRequest request = spotOrderRequest();
        request.setPriceMode(BinanceEnum.PRICE_MODE.OPPONENT_FIRST);
        request.setSourceType(BinanceSpotSellSourceDto.SourceType.ORDER);
        request.setSourceOrderId(456L);
        BinanceSpotTradeMatchState position = sourcePosition(654L, 456L, "0.006", "0");
        when(accountService.getAccountByUid(7)).thenReturn(account);
        when(stateMapper.findStatsOpenBuys(7, "BTCUSDT"))
                .thenReturn(Collections.singletonList(position));
        when(spotUtil.listOpenOrders("BTCUSDT")).thenReturn(Collections.emptyList());
        when(spotUtil.order(any(BinanceOrderApiDto.class))).thenReturn(123L);
        when(redisUtils.set(eq("BINANCE:SPOT:SELL_SOURCE:7:BTCUSDT:123"),
                any(BinanceSpotSellSourceDto.class), eq(365L), eq(TimeUnit.DAYS))).thenReturn(true);
        when(redisUtils.sHasKey("BINANCE:SPOT:SELL_SOURCE:INDEX:7:BTCUSDT", 123L)).thenReturn(true);

        assertEquals(123L, service.createSpotOrder(request));

        ArgumentCaptor<BinanceSpotSellSourceDto> captor =
                ArgumentCaptor.forClass(BinanceSpotSellSourceDto.class);
        verify(redisUtils).set(eq("BINANCE:SPOT:SELL_SOURCE:7:BTCUSDT:123"),
                captor.capture(), eq(365L), eq(TimeUnit.DAYS));
        assertEquals(BinanceSpotSellSourceDto.SourceType.ORDER, captor.getValue().getSourceType());
        assertEquals(Long.valueOf(456L), captor.getValue().getSourceOrderId());
        assertEquals(new BigDecimal("0.001"), captor.getValue().getQuantity());
    }

    @Test
    void shouldRejectQuickSellQuantityAlreadyOccupiedByOpenOrder() {
        BinanceAccountInfo account = validAccount();
        BinanceSpotOrderRequest request = spotOrderRequest();
        request.setPriceMode(BinanceEnum.PRICE_MODE.OPPONENT_FIRST);
        request.setQuantity(new BigDecimal("0.006"));
        request.setSourceType(BinanceSpotSellSourceDto.SourceType.ORDER);
        request.setSourceOrderId(456L);
        BinanceSpotOpenOrderDto openOrder = new BinanceSpotOpenOrderDto();
        openOrder.setOrderId(789L);
        openOrder.setOrigQty(new BigDecimal("0.002"));
        openOrder.setExecutedQty(BigDecimal.ZERO);
        BinanceSpotSellSourceDto source = new BinanceSpotSellSourceDto();
        source.setSourceType(BinanceSpotSellSourceDto.SourceType.ORDER);
        source.setSourceOrderId(456L);
        when(accountService.getAccountByUid(7)).thenReturn(account);
        when(stateMapper.findStatsOpenBuys(7, "BTCUSDT"))
                .thenReturn(Collections.singletonList(sourcePosition(654L, 456L, "0.006", "0")));
        when(spotUtil.listOpenOrders("BTCUSDT")).thenReturn(Collections.singletonList(openOrder));
        when(redisUtils.get("BINANCE:SPOT:SELL_SOURCE:7:BTCUSDT:789", BinanceSpotSellSourceDto.class))
                .thenReturn(source);

        assertThrows(BadRequestException.class, () -> service.createSpotOrder(request));

        verify(spotUtil, never()).order(any(BinanceOrderApiDto.class));
        assertNull(BinanceAccountContextHolder.get());
    }

    @Test
    void shouldCreateFixedPriceTakeProfitLimitOrder() {
        BinanceAccountInfo account = validAccount();
        BinanceSpotOrderRequest request = spotOrderRequest();
        request.setType(BinanceEnum.TYPE.TAKE_PROFIT_LIMIT);
        request.setPriceMode(BinanceEnum.PRICE_MODE.FIXED);
        request.setPrice(new BigDecimal("89990"));
        when(accountService.getAccountByUid(7)).thenReturn(account);
        when(spotUtil.order(any(BinanceOrderApiDto.class))).thenReturn(456L);

        assertEquals(456L, service.createSpotOrder(request));

        ArgumentCaptor<BinanceOrderApiDto> captor = ArgumentCaptor.forClass(BinanceOrderApiDto.class);
        verify(spotUtil).order(captor.capture());
        assertEquals(new BigDecimal("89990"), captor.getValue().getPrice());
        assertNull(captor.getValue().getPegPriceType());
    }

    @Test
    void shouldRejectUnsupportedSpotOrderBeforeLoadingAccount() {
        BinanceSpotOrderRequest request = spotOrderRequest();
        request.setType(BinanceEnum.TYPE.LIMIT);
        request.setPriceMode(BinanceEnum.PRICE_MODE.OPPONENT_FIRST);

        assertThrows(BadRequestException.class, () -> service.createSpotOrder(request));

        verify(accountService, never()).getAccountByUid(7);
        verify(spotUtil, never()).order(any(BinanceOrderApiDto.class));
    }

    @Test
    void shouldListOpenOrdersForSelectedSpotAccount() {
        BinanceAccountInfo account = validAccount();
        BinanceSpotOpenOrderDto openOrder = new BinanceSpotOpenOrderDto();
        openOrder.setOrderId(789L);
        when(accountService.getAccountByUid(7)).thenReturn(account);
        when(spotUtil.listOpenOrders("BTCUSDT")).thenReturn(Collections.singletonList(openOrder));

        assertEquals(Collections.singletonList(openOrder), service.listSpotOpenOrders(7, "btcusdt"));

        verify(spotUtil).listOpenOrders("BTCUSDT");
        assertNull(BinanceAccountContextHolder.get());
    }

    @Test
    void shouldReconcileSellSourcesWhenListingOpenOrdersWithoutDuplicateOpenOrderRequest() {
        BinanceAccountInfo account = validAccount();
        BinanceSpotOpenOrderDto canceledOrder = spotOrder(789L, "CANCELED", "0");
        when(accountService.getAccountByUid(7)).thenReturn(account);
        when(spotUtil.listOpenOrders("BTCUSDT")).thenReturn(Collections.emptyList());
        when(redisUtils.hasKey("BINANCE:SPOT:SELL_SOURCE:INDEXED:7:BTCUSDT")).thenReturn(true);
        when(redisUtils.sGet("BINANCE:SPOT:SELL_SOURCE:INDEX:7:BTCUSDT"))
                .thenReturn(Collections.singleton((Object) 789L));
        when(spotUtil.queryOrder("BTCUSDT", 789L)).thenReturn(canceledOrder);

        assertEquals(Collections.emptyList(), service.listSpotOpenOrders(7, "BTCUSDT"));

        verify(spotUtil, times(1)).listOpenOrders("BTCUSDT");
        verify(spotUtil).queryOrder("BTCUSDT", 789L);
        verify(redisUtils).del("BINANCE:SPOT:SELL_SOURCE:7:BTCUSDT:789");
    }

    @Test
    void shouldEnrichOpenOrderWithTradeSource() {
        BinanceAccountInfo account = validAccount();
        BinanceSpotOpenOrderDto openOrder = new BinanceSpotOpenOrderDto();
        openOrder.setOrderId(789L);
        BinanceSpotSellSourceDto source = new BinanceSpotSellSourceDto();
        source.setSourceType(BinanceSpotSellSourceDto.SourceType.TRADE);
        source.setSourceOrderId(456L);
        source.setSourceTradeId(654L);
        when(accountService.getAccountByUid(7)).thenReturn(account);
        when(spotUtil.listOpenOrders("BTCUSDT")).thenReturn(Collections.singletonList(openOrder));
        when(redisUtils.get("BINANCE:SPOT:SELL_SOURCE:7:BTCUSDT:789", BinanceSpotSellSourceDto.class))
                .thenReturn(source);

        List<BinanceSpotOpenOrderDto> result = service.listSpotOpenOrders(7, "BTCUSDT");

        assertEquals(BinanceSpotSellSourceDto.SourceType.TRADE, result.get(0).getSourceType());
        assertEquals(Long.valueOf(456L), result.get(0).getSourceOrderId());
        assertEquals(Long.valueOf(654L), result.get(0).getSourceTradeId());
        verify(redisUtils).expire("BINANCE:SPOT:SELL_SOURCE:7:BTCUSDT:789", 365L, TimeUnit.DAYS);
    }

    @Test
    void shouldCancelSpotOrderAndRemoveSourceAssociation() {
        BinanceAccountInfo account = validAccount();
        when(accountService.getAccountByUid(7)).thenReturn(account);
        when(spotUtil.cancelOrder("BTCUSDT", 789L)).thenReturn(789L);

        assertEquals(789L, service.cancelSpotOrder(7, "BTCUSDT", 789L));

        verify(spotUtil).cancelOrder("BTCUSDT", 789L);
        verify(redisUtils).del("BINANCE:SPOT:SELL_SOURCE:7:BTCUSDT:789");
        assertNull(BinanceAccountContextHolder.get());
    }

    @Test
    void shouldRemoveCanceledSellSourceDuringReconciliation() {
        BinanceAccountInfo account = validAccount();
        BinanceSpotOpenOrderDto canceledOrder = spotOrder(789L, "CANCELED", "0");
        when(accountService.getAccountByUid(7)).thenReturn(account);
        when(spotUtil.listOpenOrders("BTCUSDT")).thenReturn(Collections.emptyList());
        when(redisUtils.hasKey("BINANCE:SPOT:SELL_SOURCE:INDEXED:7:BTCUSDT")).thenReturn(true);
        when(redisUtils.sGet("BINANCE:SPOT:SELL_SOURCE:INDEX:7:BTCUSDT"))
                .thenReturn(Collections.singleton((Object) 789L));
        when(spotUtil.queryOrder("BTCUSDT", 789L)).thenReturn(canceledOrder);

        BinanceSpotSellSourceReconcileResult result =
                service.reconcileSpotSellSources(7, "BTCUSDT");

        assertEquals(1, result.getCheckedCount());
        assertEquals(1, result.getRemovedCount());
        verify(redisUtils).del("BINANCE:SPOT:SELL_SOURCE:7:BTCUSDT:789");
        verify(redisUtils).setRemove("BINANCE:SPOT:SELL_SOURCE:INDEX:7:BTCUSDT", 789L);
        assertNull(BinanceAccountContextHolder.get());
    }

    @Test
    void shouldKeepFilledSellSourceAndRemoveItFromActiveIndex() {
        BinanceAccountInfo account = validAccount();
        BinanceSpotOpenOrderDto filledOrder = spotOrder(789L, "FILLED", "0.006");
        BinanceSpotSellSourceDto source = new BinanceSpotSellSourceDto();
        source.setSellOrderId(789L);
        when(accountService.getAccountByUid(7)).thenReturn(account);
        when(spotUtil.listOpenOrders("BTCUSDT")).thenReturn(Collections.emptyList());
        when(redisUtils.hasKey("BINANCE:SPOT:SELL_SOURCE:INDEXED:7:BTCUSDT")).thenReturn(true);
        when(redisUtils.sGet("BINANCE:SPOT:SELL_SOURCE:INDEX:7:BTCUSDT"))
                .thenReturn(Collections.singleton((Object) 789L));
        when(spotUtil.queryOrder("BTCUSDT", 789L)).thenReturn(filledOrder);
        when(redisUtils.get("BINANCE:SPOT:SELL_SOURCE:7:BTCUSDT:789", BinanceSpotSellSourceDto.class))
                .thenReturn(source);

        BinanceSpotSellSourceReconcileResult result =
                service.reconcileSpotSellSources(7, "BTCUSDT");

        assertEquals(1, result.getFilledCount());
        assertEquals("FILLED", source.getStatus());
        assertEquals(new BigDecimal("0.006"), source.getExecutedQty());
        verify(redisUtils, never()).del("BINANCE:SPOT:SELL_SOURCE:7:BTCUSDT:789");
        verify(redisUtils).set(eq("BINANCE:SPOT:SELL_SOURCE:7:BTCUSDT:789"), eq(source),
                eq(365L), eq(TimeUnit.DAYS));
        verify(redisUtils).setRemove("BINANCE:SPOT:SELL_SOURCE:INDEX:7:BTCUSDT", 789L);
    }

    @Test
    void shouldRetainSellSourceWhenOrderStatusQueryFails() {
        BinanceAccountInfo account = validAccount();
        when(accountService.getAccountByUid(7)).thenReturn(account);
        when(spotUtil.listOpenOrders("BTCUSDT")).thenReturn(Collections.emptyList());
        when(redisUtils.hasKey("BINANCE:SPOT:SELL_SOURCE:INDEXED:7:BTCUSDT")).thenReturn(true);
        when(redisUtils.sGet("BINANCE:SPOT:SELL_SOURCE:INDEX:7:BTCUSDT"))
                .thenReturn(Collections.singleton((Object) 789L));
        when(spotUtil.queryOrder("BTCUSDT", 789L)).thenThrow(new RuntimeException("temporary failure"));

        BinanceSpotSellSourceReconcileResult result =
                service.reconcileSpotSellSources(7, "BTCUSDT");

        assertEquals(1, result.getFailedCount());
        verify(redisUtils, never()).del("BINANCE:SPOT:SELL_SOURCE:7:BTCUSDT:789");
        verify(redisUtils, never()).setRemove("BINANCE:SPOT:SELL_SOURCE:INDEX:7:BTCUSDT", 789L);
        assertNull(BinanceAccountContextHolder.get());
    }

    @Test
    void shouldRetainPartiallyFilledSellSourceDuringReconciliation() {
        BinanceAccountInfo account = validAccount();
        BinanceSpotOpenOrderDto partialOrder = spotOrder(789L, "PARTIALLY_FILLED", "0.002");
        BinanceSpotSellSourceDto source = new BinanceSpotSellSourceDto();
        source.setSellOrderId(789L);
        when(accountService.getAccountByUid(7)).thenReturn(account);
        when(spotUtil.listOpenOrders("BTCUSDT")).thenReturn(Collections.emptyList());
        when(redisUtils.hasKey("BINANCE:SPOT:SELL_SOURCE:INDEXED:7:BTCUSDT")).thenReturn(true);
        when(redisUtils.sGet("BINANCE:SPOT:SELL_SOURCE:INDEX:7:BTCUSDT"))
                .thenReturn(Collections.singleton((Object) 789L));
        when(spotUtil.queryOrder("BTCUSDT", 789L)).thenReturn(partialOrder);
        when(redisUtils.get("BINANCE:SPOT:SELL_SOURCE:7:BTCUSDT:789", BinanceSpotSellSourceDto.class))
                .thenReturn(source);

        BinanceSpotSellSourceReconcileResult result =
                service.reconcileSpotSellSources(7, "BTCUSDT");

        assertEquals(1, result.getRetainedCount());
        assertEquals("PARTIALLY_FILLED", source.getStatus());
        assertEquals(new BigDecimal("0.002"), source.getExecutedQty());
        verify(redisUtils, never()).del("BINANCE:SPOT:SELL_SOURCE:7:BTCUSDT:789");
        verify(redisUtils, never()).setRemove("BINANCE:SPOT:SELL_SOURCE:INDEX:7:BTCUSDT", 789L);
    }

    @Test
    void shouldBootstrapExistingSellSourceIntoIndex() {
        BinanceAccountInfo account = validAccount();
        BinanceSpotSellSourceDto source = new BinanceSpotSellSourceDto();
        source.setSellOrderId(789L);
        when(accountService.getAccountByUid(7)).thenReturn(account);
        when(spotUtil.listOpenOrders("BTCUSDT")).thenReturn(Collections.singletonList(
                spotOrder(789L, "NEW", "0")));
        when(redisUtils.scan("BINANCE:SPOT:SELL_SOURCE:7:BTCUSDT:*")).thenReturn(
                Collections.singletonList("BINANCE:SPOT:SELL_SOURCE:7:BTCUSDT:789"));
        when(redisUtils.get("BINANCE:SPOT:SELL_SOURCE:7:BTCUSDT:789", BinanceSpotSellSourceDto.class))
                .thenReturn(source);
        when(redisUtils.sHasKey("BINANCE:SPOT:SELL_SOURCE:INDEX:7:BTCUSDT", 789L)).thenReturn(true);
        when(redisUtils.sGet("BINANCE:SPOT:SELL_SOURCE:INDEX:7:BTCUSDT"))
                .thenReturn(Collections.singleton((Object) 789L));

        BinanceSpotSellSourceReconcileResult result =
                service.reconcileSpotSellSources(7, "BTCUSDT");

        assertEquals(1, result.getOpenCount());
        verify(redisUtils).sSetAndTime("BINANCE:SPOT:SELL_SOURCE:INDEX:7:BTCUSDT",
                TimeUnit.DAYS.toSeconds(365L), 789L);
        verify(redisUtils).set("BINANCE:SPOT:SELL_SOURCE:INDEXED:7:BTCUSDT", true,
                365L, TimeUnit.DAYS);
        verify(spotUtil, never()).queryOrder("BTCUSDT", 789L);
    }

    private BinanceSpotOpenOrderDto spotOrder(Long orderId, String status, String executedQty) {
        BinanceSpotOpenOrderDto order = new BinanceSpotOpenOrderDto();
        order.setOrderId(orderId);
        order.setStatus(status);
        order.setExecutedQty(new BigDecimal(executedQty));
        return order;
    }

    private BinanceAccountInfo validAccount() {
        BinanceAccountInfo account = new BinanceAccountInfo();
        account.setUid(7);
        account.setApiValidFlag(1);
        account.setApiKey("api-key");
        account.setApiSecret("api-secret");
        return account;
    }

    private BinanceSpotOrderRequest spotOrderRequest() {
        BinanceSpotOrderRequest request = new BinanceSpotOrderRequest();
        request.setUid(7);
        request.setSymbol("BTCUSDT");
        request.setSide(BinanceEnum.SIDE.SELL);
        request.setType(BinanceEnum.TYPE.STOP_LOSS_LIMIT);
        request.setQuantity(new BigDecimal("0.001"));
        request.setStopPrice(new BigDecimal("80000"));
        return request;
    }

    private BinanceSpotTradeMatchState sourcePosition(Long tradeId, Long orderId,
                                                      String remainingQty, String coreQty) {
        BinanceSpotTradeMatchState position = new BinanceSpotTradeMatchState();
        position.setTradeId(tradeId);
        position.setOrderId(orderId);
        position.setRemainingQty(new BigDecimal(remainingQty));
        position.setActiveCoreQty(new BigDecimal(coreQty));
        return position;
    }

    @Test
    void shouldReturnStableEmptyStatsWithoutMutatingCriteria() {
        BinanceTradeInfoQueryCriteria criteria = criteria();
        when(spotUtil.getPrice(BinanceEnum.SYMBOL.BTCUSDT)).thenReturn(new BigDecimal("140"));

        BinanceTradeStatsInfoVO result = service.stats(criteria);

        assertEquals(BigDecimal.ZERO, result.getPosQty());
        assertEquals(BigDecimal.ZERO, result.getPosAmount());
        assertNull(result.getPosAvgPrice());
        assertNull(result.getRoi());
        assertEquals(new BigDecimal("140"), result.getCurrentSpotPrice());
        assertTrue(result.getTradeList().isEmpty());
        assertTrue(result.getWarnings().isEmpty());
        assertNull(criteria.getIsBuyer());
        assertNull(criteria.getHedgedFlag());
        assertEquals("time", criteria.getOrderColumn());
        assertEquals("desc", criteria.getOrderDirection());
        verify(spotUtil).getPrice(BinanceEnum.SYMBOL.BTCUSDT);
    }

    @Test
    void shouldReturnPersistedClosedTradeStats() {
        String cacheKey = "SPOT_LAST_NET_PNL:1:BTCUSDT";
        when(redisUtils.get(cacheKey)).thenReturn(new BigDecimal("8.000"));
        when(matchMapper.aggregateStats(eq(1), eq("BTCUSDT")))
                .thenReturn(aggregate("100", "110", "10", "0.210", "9.790"));
        when(spotUtil.getPrice(BinanceEnum.SYMBOL.BTCUSDT)).thenReturn(new BigDecimal("140"));

        BinanceTradeStatsInfoVO result = service.stats(criteria());

        assertEquals(BigDecimal.ZERO, result.getPosQty());
        assertEquals(BigDecimal.ZERO, result.getPosAmount());
        assertNull(result.getPosAvgPrice());
        assertEquals(new BigDecimal("100"), result.getTotalBuyAmount());
        assertEquals(new BigDecimal("110"), result.getTotalSellAmount());
        assertEquals(new BigDecimal("9.790"), result.getNetPnl());
        assertEquals(new BigDecimal("0.09790000"), result.getRoi());
        assertEquals(new BigDecimal("8.000"), result.getLastNetPnl());
        assertEquals(new BigDecimal("140"), result.getCurrentSpotPrice());
        assertTrue(result.getTradeList().isEmpty());
        verify(redisUtils).set(cacheKey, new BigDecimal("9.790"));
        verify(spotUtil).getPrice(BinanceEnum.SYMBOL.BTCUSDT);
    }

    @Test
    void shouldUseProvidedRealtimePriceWithoutPositionOrAnotherHttpRequest() {
        BinanceTradeStatsInfoVO result = service.stats(criteria(), new BigDecimal("140"));

        assertEquals(BigDecimal.ZERO, result.getPosQty());
        assertEquals(new BigDecimal("140"), result.getCurrentSpotPrice());
        assertTrue(result.getTradeList().isEmpty());
        assertTrue(result.getWarnings().isEmpty());
        verify(spotUtil, never()).getPrice(any(BinanceEnum.SYMBOL.class));
    }

    @Test
    void shouldWarnWhenCurrentPriceFailsWithoutPosition() {
        when(spotUtil.getPrice(BinanceEnum.SYMBOL.BTCUSDT))
                .thenThrow(new RuntimeException("proxy unavailable"));

        BinanceTradeStatsInfoVO result = service.stats(criteria());

        assertEquals(BigDecimal.ZERO, result.getPosQty());
        assertNull(result.getCurrentSpotPrice());
        assertTrue(result.getTradeList().isEmpty());
        assertTrue(result.getWarnings().contains("现货当前价格暂不可用"));
    }

    @Test
    void shouldKeepPositionStatsWhenRealtimePriceFails() {
        when(stateMapper.findStatsOpenBuys(eq(1), eq("BTCUSDT")))
                .thenReturn(Collections.singletonList(position(1L, "100", "2", 1_000L)));
        when(spotUtil.getPrice(BinanceEnum.SYMBOL.BTCUSDT))
                .thenThrow(new RuntimeException("proxy unavailable"));

        BinanceTradeStatsInfoVO result = service.stats(criteria());

        assertEquals(new BigDecimal("2"), result.getPosQty());
        assertEquals(new BigDecimal("200"), result.getPosAmount());
        assertEquals(new BigDecimal("100.00000000"), result.getPosAvgPrice());
        assertNull(result.getCurrentSpotPrice());
        assertTrue(result.getTradeList().isEmpty());
        assertTrue(result.getWarnings().contains("现货持仓实时估值暂不可用"));
    }

    @Test
    void shouldUsePersistedFifoResultAndRemainingPosition() {
        when(matchMapper.aggregateStats(eq(1), eq("BTCUSDT")))
                .thenReturn(aggregate("120", "130", "10", "0.250", "9.750"));
        when(stateMapper.findStatsOpenBuys(eq(1), eq("BTCUSDT")))
                .thenReturn(Collections.singletonList(position(2L, "100", "1", 2_000L)));
        when(spotUtil.getPrice(BinanceEnum.SYMBOL.BTCUSDT)).thenReturn(new BigDecimal("140"));

        BinanceTradeStatsInfoVO result = service.stats(criteria());

        assertEquals(new BigDecimal("120"), result.getTotalBuyAmount());
        assertEquals(new BigDecimal("130"), result.getTotalSellAmount());
        assertEquals(new BigDecimal("100.00000000"), result.getPosAvgPrice());
        assertEquals(new BigDecimal("1"), result.getPosQty());
        assertEquals(BigDecimal.ZERO, result.getUnmatchedSellQty());
    }

    @Test
    void shouldUseProvidedRealtimePriceWithoutAnotherHttpRequest() {
        when(stateMapper.findStatsOpenBuys(eq(1), eq("BTCUSDT")))
                .thenReturn(Collections.singletonList(position(2L, "100", "1", 2_000L)));

        BinanceTradeStatsInfoVO result = service.stats(criteria(), new BigDecimal("140"));

        assertEquals(new BigDecimal("140"), result.getCurrentSpotPrice());
        assertEquals(1, result.getTradeList().size());
        verify(spotUtil, never()).getPrice(any(BinanceEnum.SYMBOL.class));
    }

    @Test
    void shouldKeepEachOpenBuyAndItsOriginalTradeTime() {
        when(stateMapper.findStatsOpenBuys(eq(1), eq("BTCUSDT")))
                .thenReturn(Arrays.asList(
                        position(2L, "100", "1", 2_000L),
                        position(1L, "100", "0.5", 1_000L)));
        when(spotUtil.getPrice(BinanceEnum.SYMBOL.BTCUSDT)).thenReturn(new BigDecimal("140"));

        BinanceTradeStatsInfoVO result = service.stats(criteria());

        assertEquals(2, result.getTradeList().size());
        assertEquals(new Timestamp(1_000L), result.getTradeList().get(0).getOpenTime());
        assertEquals(new Timestamp(2_000L), result.getTradeList().get(1).getOpenTime());
        assertEquals(new BigDecimal("0.5"), result.getTradeList().get(0).getQty());
        assertEquals(new BigDecimal("1"), result.getTradeList().get(1).getQty());
    }

    @Test
    void shouldIncludeCorePositionStatusInOpenTrades() {
        BinanceSpotTradeMatchState position = position(2L, "100", "1", 2_000L);
        position.setOrderId(88L);
        position.setCorePositionId(9L);
        position.setActiveCoreQty(new BigDecimal("0.4"));
        position.setCoreLockedAt(new Timestamp(1_500L));
        when(stateMapper.findStatsOpenBuys(eq(1), eq("BTCUSDT")))
                .thenReturn(Collections.singletonList(position));
        when(spotUtil.getPrice(BinanceEnum.SYMBOL.BTCUSDT)).thenReturn(new BigDecimal("140"));

        BinanceTradeStatsInfoVO result = service.stats(criteria());

        MatchedTradeInfo trade = result.getTradeList().get(0);
        assertEquals(Long.valueOf(2L), trade.getTradeId());
        assertEquals(Long.valueOf(88L), trade.getOrderId());
        assertEquals(Long.valueOf(9L), trade.getCorePositionId());
        assertEquals(new BigDecimal("0.4"), trade.getCoreQty());
        assertEquals(new BigDecimal("0.6"), trade.getAvailableQty());
        assertEquals(new Timestamp(1_500L), trade.getCoreLockedAt());
    }

    @Test
    void shouldReportPersistedUnmatchedSell() {
        when(stateMapper.sumStatsUnmatchedSellQty(eq(1), eq("BTCUSDT")))
                .thenReturn(BigDecimal.ONE);
        when(stateMapper.findStatsOpenBuys(eq(1), eq("BTCUSDT")))
                .thenReturn(Collections.singletonList(position(2L, "100", "1", 2_000L)));
        when(spotUtil.getPrice(BinanceEnum.SYMBOL.BTCUSDT)).thenReturn(new BigDecimal("120"));

        BinanceTradeStatsInfoVO result = service.stats(criteria());

        assertEquals(BigDecimal.ZERO, result.getTotalBuyAmount());
        assertEquals(BigDecimal.ZERO, result.getTotalSellAmount());
        assertEquals(BigDecimal.ONE, result.getUnmatchedSellQty());
        assertEquals(BigDecimal.ONE, result.getPosQty());
        assertTrue(result.getWarnings().contains("部分卖出成交缺少可匹配的历史买入"));
    }

    private BinanceTradeInfoQueryCriteria criteria() {
        BinanceTradeInfoQueryCriteria criteria = new BinanceTradeInfoQueryCriteria();
        criteria.setUid(1);
        criteria.setSymbol(BinanceEnum.SYMBOL.BTCUSDT.name());
        return criteria;
    }

    private BinanceSpotTradeStatsAggregate aggregate(String buyAmount, String sellAmount,
                                                      String pnl, String fee, String netPnl) {
        BinanceSpotTradeStatsAggregate aggregate = new BinanceSpotTradeStatsAggregate();
        aggregate.setTotalBuyAmount(new BigDecimal(buyAmount));
        aggregate.setTotalSellAmount(new BigDecimal(sellAmount));
        aggregate.setPnl(new BigDecimal(pnl));
        aggregate.setFee(new BigDecimal(fee));
        aggregate.setNetPnl(new BigDecimal(netPnl));
        return aggregate;
    }

    private BinanceSpotTradeMatchState position(Long tradeId, String price, String qty, long time) {
        BinanceSpotTradeMatchState position = new BinanceSpotTradeMatchState();
        position.setTradeId(tradeId);
        position.setPrice(new BigDecimal(price));
        position.setRemainingQty(new BigDecimal(qty));
        position.setTradeTime(new Timestamp(time));
        return position;
    }
}
