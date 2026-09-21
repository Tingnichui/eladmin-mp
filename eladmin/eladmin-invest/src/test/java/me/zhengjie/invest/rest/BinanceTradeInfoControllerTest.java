package me.zhengjie.invest.rest;

import me.zhengjie.exception.BadRequestException;
import me.zhengjie.invest.domain.BinanceAccountInfo;
import me.zhengjie.invest.domain.dto.BinanceSpotTradeMatchResult;
import me.zhengjie.invest.domain.dto.BinanceSpotOrderRequest;
import me.zhengjie.invest.domain.dto.BinanceSpotOpenOrderDto;
import me.zhengjie.invest.domain.dto.BinanceTradeInfoQueryCriteria;
import me.zhengjie.invest.domain.dto.BinanceTradeStatsInfoVO;
import me.zhengjie.invest.service.BinanceAccountInfoService;
import me.zhengjie.invest.service.BinanceCoinFuturesTradeInfoService;
import me.zhengjie.invest.service.BinanceC2cOrderService;
import me.zhengjie.invest.service.BinanceFuturesTradeInfoService;
import me.zhengjie.invest.service.BinanceSpotTradeMatcherService;
import me.zhengjie.invest.service.BinanceTradeInfoService;
import me.zhengjie.invest.service.support.BinanceStatsRealtimeService;
import me.zhengjie.invest.service.support.BinanceStatsRealtimeSnapshot;
import me.zhengjie.invest.util.BinanceAccountContextHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BinanceTradeInfoControllerTest {

    private final BinanceTradeInfoService spotService = mock(BinanceTradeInfoService.class);
    private final BinanceFuturesTradeInfoService usdFuturesService = mock(BinanceFuturesTradeInfoService.class);
    private final BinanceCoinFuturesTradeInfoService coinFuturesService =
            mock(BinanceCoinFuturesTradeInfoService.class);
    private final BinanceAccountInfoService accountService = mock(BinanceAccountInfoService.class);
    private final BinanceC2cOrderService c2cOrderService = mock(BinanceC2cOrderService.class);
    private final BinanceSpotTradeMatcherService matcherService = mock(BinanceSpotTradeMatcherService.class);
    private final BinanceStatsRealtimeService realtimeService = mock(BinanceStatsRealtimeService.class);
    private final BinanceTradeInfoController controller = new BinanceTradeInfoController();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "binanceTradeInfoService", spotService);
        ReflectionTestUtils.setField(controller, "binanceFuturesTradeInfoService", usdFuturesService);
        ReflectionTestUtils.setField(controller, "binanceCoinFuturesTradeInfoService", coinFuturesService);
        ReflectionTestUtils.setField(controller, "binanceAccountInfoService", accountService);
        ReflectionTestUtils.setField(controller, "binanceC2cOrderService", c2cOrderService);
        ReflectionTestUtils.setField(controller, "binanceSpotTradeMatcherService", matcherService);
        ReflectionTestUtils.setField(controller, "binanceStatsRealtimeService", realtimeService);
    }

    @Test
    void shouldQueryOnlySpotStats() {
        BinanceAccountInfo account = new BinanceAccountInfo();
        account.setUid(7);
        BinanceTradeInfoQueryCriteria criteria = new BinanceTradeInfoQueryCriteria();
        criteria.setUid(7);
        criteria.setSymbol("BTCUSDT");
        BinanceStatsRealtimeSnapshot snapshot = new BinanceStatsRealtimeSnapshot();
        snapshot.setCurrentSpotPrice(new BigDecimal("63000"));
        BinanceTradeStatsInfoVO spotStats = new BinanceTradeStatsInfoVO();
        when(accountService.getAccountByUid(7)).thenReturn(account);
        when(realtimeService.loadSpot(7, "BTCUSDT")).thenReturn(snapshot);
        when(spotService.stats(criteria, new BigDecimal("63000"))).thenReturn(spotStats);

        ResponseEntity<Object> response = controller.queryBinanceTradeStats(criteria);
        Map<?, ?> body = (Map<?, ?>) response.getBody();

        assertEquals(spotStats, body.get("spotFuturesStatsInfo"));
        assertFalse(body.containsKey("accountInfo"));
        assertFalse(body.containsKey("usdFuturesStatsInfo"));
        assertFalse(body.containsKey("coinFuturesStatsInfo"));
        verify(realtimeService).loadSpot(7, "BTCUSDT");
        verify(spotService).stats(criteria, new BigDecimal("63000"));
        verifyNoInteractions(usdFuturesService, coinFuturesService);
        assertNull(BinanceAccountContextHolder.get());
    }

    @Test
    void shouldSyncOnlySelectedAccountAndSpotSymbol() {
        BinanceAccountInfo account = new BinanceAccountInfo();
        account.setUid(7);
        account.setApiValidFlag(1);
        when(accountService.getAccountByUid(7)).thenReturn(account);
        when(spotService.syncTradeInfo(account, "BTCUSDT")).thenReturn(2);
        when(usdFuturesService.sync(account)).thenReturn(1);
        when(coinFuturesService.sync(account)).thenReturn(0);

        ResponseEntity<Map<String, Object>> response = controller.syncSelected(7, "BTCUSDT");

        assertEquals(2, response.getBody().get("spotCount"));
        assertEquals(1, response.getBody().get("usdFuturesCount"));
        assertEquals(0, response.getBody().get("coinFuturesCount"));
        assertFalse(response.getBody().containsKey("c2cOrderCount"));
        verify(spotService).syncTradeInfo(account, "BTCUSDT");
        verify(usdFuturesService).sync(account);
        verify(coinFuturesService).sync(account);
        verifyNoInteractions(c2cOrderService);
        assertNull(BinanceAccountContextHolder.get());
    }

    @Test
    void shouldRejectNonSpotSymbolBeforeLoadingAccount() {
        assertThrows(BadRequestException.class,
                () -> controller.syncSelected(7, "BTCUSD_PERP"));

        verify(accountService, never()).getAccountByUid(7);
        verifyNoInteractions(spotService);
    }

    @Test
    void shouldInitializeAndMatchSelectedSpotSymbol() {
        BinanceSpotTradeMatchResult result = new BinanceSpotTradeMatchResult();
        result.setInitializedCount(23);
        result.setMatchCount(18);
        when(matcherService.initializeAndMatch(7, "BNBUSDT")).thenReturn(result);

        ResponseEntity<BinanceSpotTradeMatchResult> response =
                controller.matchSpotTradeInfo(7, "BNBUSDT");

        assertEquals(23, response.getBody().getInitializedCount());
        assertEquals(18, response.getBody().getMatchCount());
        verify(matcherService).initializeAndMatch(7, "BNBUSDT");
    }

    @Test
    void shouldRejectFuturesSymbolForSpotMatching() {
        assertThrows(BadRequestException.class,
                () -> controller.matchSpotTradeInfo(7, "BTCUSD_PERP"));

        verifyNoInteractions(matcherService);
    }

    @Test
    void shouldReturnCreatedSpotOrderId() {
        BinanceSpotOrderRequest request = new BinanceSpotOrderRequest();
        when(spotService.createSpotOrder(request)).thenReturn(123L);

        ResponseEntity<Map<String, Long>> response = controller.createSpotOrder(request);

        assertEquals(201, response.getStatusCodeValue());
        assertEquals(123L, response.getBody().get("orderId"));
        verify(spotService).createSpotOrder(request);
    }

    @Test
    void shouldReturnSpotOpenOrders() {
        BinanceSpotOpenOrderDto openOrder = new BinanceSpotOpenOrderDto();
        openOrder.setOrderId(789L);
        List<BinanceSpotOpenOrderDto> openOrders = Collections.singletonList(openOrder);
        when(spotService.listSpotOpenOrders(7, "BTCUSDT")).thenReturn(openOrders);

        ResponseEntity<List<BinanceSpotOpenOrderDto>> response =
                controller.listSpotOpenOrders(7, "BTCUSDT");

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(openOrders, response.getBody());
        verify(spotService).listSpotOpenOrders(7, "BTCUSDT");
    }

    @Test
    void shouldCancelSpotOrder() {
        when(spotService.cancelSpotOrder(7, "BTCUSDT", 789L)).thenReturn(789L);

        ResponseEntity<Map<String, Long>> response =
                controller.cancelSpotOrder(7, "BTCUSDT", 789L);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(Long.valueOf(789L), response.getBody().get("orderId"));
        verify(spotService).cancelSpotOrder(7, "BTCUSDT", 789L);
    }
}
