package me.zhengjie.invest.service.support;

import me.zhengjie.invest.constants.BinanceEnum;
import me.zhengjie.invest.service.BinanceCoinFuturesTradeInfoService;
import me.zhengjie.invest.util.BinanceCoinFuturesUtil;
import me.zhengjie.invest.util.BinanceSpotUtil;
import me.zhengjie.invest.util.BinanceUsdFuturesUtil;
import me.zhengjie.utils.RedisUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BinanceStatsRealtimeServiceTest {

    private final RedisUtils redisUtils = mock(RedisUtils.class);
    private final BinanceUsdFuturesUtil usdFuturesUtil = mock(BinanceUsdFuturesUtil.class);
    private final BinanceCoinFuturesUtil coinFuturesUtil = mock(BinanceCoinFuturesUtil.class);
    private final BinanceSpotUtil spotUtil = mock(BinanceSpotUtil.class);
    private final BinanceCoinFuturesTradeInfoService coinFuturesService = mock(BinanceCoinFuturesTradeInfoService.class);
    private final ThreadPoolTaskExecutor executor = createExecutor();
    private final BinanceStatsRealtimeService service = new BinanceStatsRealtimeService(
            redisUtils, usdFuturesUtil, coinFuturesUtil, spotUtil, coinFuturesService, executor
    );

    @AfterEach
    void shutdownExecutor() {
        executor.shutdown();
    }

    @Test
    void shouldReturnLiveSnapshotWhenRealtimeRequestsSucceed() {
        when(usdFuturesUtil.price(BinanceEnum.SYMBOL.BTCUSDT)).thenReturn(new BigDecimal("62000"));
        when(coinFuturesUtil.price(BinanceEnum.SYMBOL.BTCUSD_PERP)).thenReturn(new BigDecimal("62100"));
        when(coinFuturesService.calculatePositionFundingFee(1, BinanceEnum.SYMBOL.BTCUSD_PERP))
                .thenReturn(new BigDecimal("0.001"));
        when(spotUtil.usdStats(true)).thenReturn(Collections.singletonMap("usdAmount", new BigDecimal("100")));

        BinanceStatsRealtimeSnapshot snapshot = service.load(1);

        assertEquals(new BigDecimal("62000"), snapshot.getUsdFuturesPrice());
        assertEquals(new BigDecimal("62100"), snapshot.getCoinFuturesPrice());
        assertEquals("LIVE", snapshot.getStatuses().get("usdFuturesPrice").getStatus());
        assertTrue(snapshot.getWarnings().isEmpty());
    }

    @Test
    void shouldReuseSnapshotPositionStartForFundingFee() {
        Date positionStart = new Date(1_000L);

        service.load(1, positionStart);

        verify(coinFuturesService).calculatePositionFundingFee(
                1, BinanceEnum.SYMBOL.BTCUSD_PERP, positionStart);
        verify(coinFuturesService, never()).calculatePositionFundingFee(
                1, BinanceEnum.SYMBOL.BTCUSD_PERP);
    }

    @Test
    void shouldUseStaleCacheAndKeepOtherUnavailableItemsIsolated() {
        BinanceStatsRealtimeService.CachedValue staleUsdPrice = new BinanceStatsRealtimeService.CachedValue(
                "61000", new Date(System.currentTimeMillis() - 60_000L)
        );
        when(redisUtils.get(anyString(), eq(BinanceStatsRealtimeService.CachedValue.class)))
                .thenAnswer(invocation -> invocation.<String>getArgument(0).contains("PRICE:USD") ? staleUsdPrice : null);
        when(usdFuturesUtil.price(BinanceEnum.SYMBOL.BTCUSDT)).thenThrow(new RuntimeException("proxy unavailable"));
        when(coinFuturesUtil.price(BinanceEnum.SYMBOL.BTCUSD_PERP)).thenThrow(new RuntimeException("proxy unavailable"));
        when(coinFuturesService.calculatePositionFundingFee(1, BinanceEnum.SYMBOL.BTCUSD_PERP))
                .thenThrow(new RuntimeException("proxy unavailable"));
        when(spotUtil.usdStats(true)).thenThrow(new RuntimeException("proxy unavailable"));

        BinanceStatsRealtimeSnapshot snapshot = service.load(1);

        assertEquals(new BigDecimal("61000"), snapshot.getUsdFuturesPrice());
        assertEquals("CACHE", snapshot.getStatuses().get("usdFuturesPrice").getStatus());
        assertNull(snapshot.getCoinFuturesPrice());
        assertEquals("UNAVAILABLE", snapshot.getStatuses().get("coinFuturesPrice").getStatus());
        assertEquals(4, snapshot.getWarnings().size());
    }

    @Test
    void shouldRunRealtimeRequestsInParallel() throws Exception {
        CountDownLatch started = new CountDownLatch(4);
        when(usdFuturesUtil.price(BinanceEnum.SYMBOL.BTCUSDT))
                .thenAnswer(invocation -> awaitParallel(started, new BigDecimal("62000")));
        when(coinFuturesUtil.price(BinanceEnum.SYMBOL.BTCUSD_PERP))
                .thenAnswer(invocation -> awaitParallel(started, new BigDecimal("62100")));
        when(coinFuturesService.calculatePositionFundingFee(1, BinanceEnum.SYMBOL.BTCUSD_PERP))
                .thenAnswer(invocation -> awaitParallel(started, new BigDecimal("0.001")));
        when(spotUtil.usdStats(true))
                .thenAnswer(invocation -> awaitParallel(started, Collections.singletonMap("usdAmount", 100)));

        BinanceStatsRealtimeSnapshot snapshot = service.load(1);

        assertEquals(0L, started.getCount());
        assertEquals("LIVE", snapshot.getStatuses().get("accountInfo").getStatus());
    }

    @Test
    void shouldOpenCircuitAfterThreeConsecutiveFailures() {
        when(usdFuturesUtil.price(BinanceEnum.SYMBOL.BTCUSDT)).thenThrow(new RuntimeException("proxy unavailable"));
        when(coinFuturesUtil.price(BinanceEnum.SYMBOL.BTCUSD_PERP)).thenThrow(new RuntimeException("proxy unavailable"));
        when(coinFuturesService.calculatePositionFundingFee(1, BinanceEnum.SYMBOL.BTCUSD_PERP))
                .thenThrow(new RuntimeException("proxy unavailable"));
        when(spotUtil.usdStats(true)).thenThrow(new RuntimeException("proxy unavailable"));

        service.load(1);
        service.load(1);
        service.load(1);
        BinanceStatsRealtimeSnapshot snapshot = service.load(1);

        verify(usdFuturesUtil, times(3)).price(BinanceEnum.SYMBOL.BTCUSDT);
        assertTrue(snapshot.getStatuses().get("usdFuturesPrice").getMessage().contains("熔断"));
    }

    @Test
    void shouldIsolateCircuitByAccount() {
        when(usdFuturesUtil.price(BinanceEnum.SYMBOL.BTCUSDT))
                .thenThrow(new RuntimeException("proxy unavailable"))
                .thenThrow(new RuntimeException("proxy unavailable"))
                .thenThrow(new RuntimeException("proxy unavailable"))
                .thenReturn(new BigDecimal("62000"));

        service.load(1);
        service.load(1);
        service.load(1);
        BinanceStatsRealtimeSnapshot snapshot = service.load(2);

        verify(usdFuturesUtil, times(4)).price(BinanceEnum.SYMBOL.BTCUSDT);
        assertEquals("LIVE", snapshot.getStatuses().get("usdFuturesPrice").getStatus());
    }

    @Test
    void shouldApplyOneSharedTimeoutToAllParallelRequests() {
        when(usdFuturesUtil.price(BinanceEnum.SYMBOL.BTCUSDT)).thenAnswer(invocation -> slowValue(new BigDecimal("62000")));
        when(coinFuturesUtil.price(BinanceEnum.SYMBOL.BTCUSD_PERP)).thenAnswer(invocation -> slowValue(new BigDecimal("62100")));
        when(coinFuturesService.calculatePositionFundingFee(1, BinanceEnum.SYMBOL.BTCUSD_PERP))
                .thenAnswer(invocation -> slowValue(new BigDecimal("0.001")));
        when(spotUtil.usdStats(true)).thenAnswer(invocation -> slowValue(Collections.singletonMap("usdAmount", 100)));

        long start = System.currentTimeMillis();
        BinanceStatsRealtimeSnapshot snapshot = service.load(1);
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(elapsed < 4_500L);
        assertEquals("UNAVAILABLE", snapshot.getStatuses().get("usdFuturesPrice").getStatus());
        assertEquals("UNAVAILABLE", snapshot.getStatuses().get("accountInfo").getStatus());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldDegradeWhenExecutorIsSaturated() {
        AsyncTaskExecutor rejectedExecutor = mock(AsyncTaskExecutor.class);
        when(rejectedExecutor.submit(any(Callable.class))).thenThrow(new TaskRejectedException("busy"));
        BinanceStatsRealtimeService rejectedService = new BinanceStatsRealtimeService(
                redisUtils, usdFuturesUtil, coinFuturesUtil, spotUtil, coinFuturesService, rejectedExecutor
        );

        BinanceStatsRealtimeSnapshot snapshot = rejectedService.load(1);

        assertEquals("UNAVAILABLE", snapshot.getStatuses().get("usdFuturesPrice").getStatus());
        assertTrue(snapshot.getStatuses().get("usdFuturesPrice").getMessage().contains("繁忙"));
    }

    private <T> T awaitParallel(CountDownLatch started, T value) throws InterruptedException {
        started.countDown();
        started.await(1, TimeUnit.SECONDS);
        return value;
    }

    private <T> T slowValue(T value) throws InterruptedException {
        Thread.sleep(5_000L);
        return value;
    }

    private static ThreadPoolTaskExecutor createExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(10);
        executor.initialize();
        return executor;
    }
}
