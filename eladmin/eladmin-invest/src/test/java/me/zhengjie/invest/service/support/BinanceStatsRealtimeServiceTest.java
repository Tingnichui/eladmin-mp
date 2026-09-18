package me.zhengjie.invest.service.support;

import me.zhengjie.invest.constants.BinanceEnum;
import me.zhengjie.invest.service.BinanceCoinFuturesTradeInfoService;
import me.zhengjie.invest.util.BinanceCoinFuturesUtil;
import me.zhengjie.invest.util.BinanceSpotUtil;
import me.zhengjie.invest.util.BinanceUsdFuturesUtil;
import me.zhengjie.utils.RedisUtils;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BinanceStatsRealtimeServiceTest {

    private final RedisUtils redisUtils = mock(RedisUtils.class);
    private final BinanceUsdFuturesUtil usdFuturesUtil = mock(BinanceUsdFuturesUtil.class);
    private final BinanceCoinFuturesUtil coinFuturesUtil = mock(BinanceCoinFuturesUtil.class);
    private final BinanceSpotUtil spotUtil = mock(BinanceSpotUtil.class);
    private final BinanceCoinFuturesTradeInfoService coinFuturesService = mock(BinanceCoinFuturesTradeInfoService.class);
    private final BinanceStatsRealtimeService service = new BinanceStatsRealtimeService(
            redisUtils, usdFuturesUtil, coinFuturesUtil, spotUtil, coinFuturesService
    );

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
}
