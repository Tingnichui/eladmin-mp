package me.zhengjie.invest.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import me.zhengjie.invest.domain.BinanceAccountInfo;
import me.zhengjie.invest.domain.BinanceCoinFuturesTradeInfo;
import me.zhengjie.invest.domain.BinanceFuturesTradeInfo;
import me.zhengjie.invest.domain.BinanceTradeInfo;
import me.zhengjie.invest.service.BinanceSpotTradeMatcherService;
import me.zhengjie.invest.util.BinanceAccountContextHolder;
import me.zhengjie.invest.util.BinanceCoinFuturesUtil;
import me.zhengjie.invest.util.BinanceSpotUtil;
import me.zhengjie.invest.util.BinanceUsdFuturesUtil;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BinanceTradeIncrementalSyncTest {

    @Test
    @SuppressWarnings("unchecked")
    void shouldPageSpotTradesFromBeginningAndAccumulateInsertedCount() {
        BinanceSpotUtil spotUtil = mock(BinanceSpotUtil.class);
        BinanceSpotTradeMatcherService matcherService = mock(BinanceSpotTradeMatcherService.class);
        BinanceTradeInfoServiceImpl service = spy(new BinanceTradeInfoServiceImpl());
        ReflectionTestUtils.setField(service, "binanceSpotUtil", spotUtil);
        ReflectionTestUtils.setField(service, "binanceSpotTradeMatcherService", matcherService);
        doReturn(Collections.emptyList()).when(service).list(any(Wrapper.class));
        doReturn(true).when(service).saveOrUpdateBatch(anyCollection());
        List<BinanceTradeInfo> firstPage = spotTrades(0L, 1000);
        List<BinanceTradeInfo> secondPage = spotTrades(1000L, 2);
        when(spotUtil.getMyTrades("BTCUSDT", 0L, 1000)).thenReturn(firstPage);
        when(spotUtil.getMyTrades("BTCUSDT", 1000L, 1000)).thenReturn(secondPage);

        int count = service.syncTradeInfo(account(7), "BTCUSDT");

        assertEquals(1002, count);
        verify(spotUtil).getMyTrades("BTCUSDT", 0L, 1000);
        verify(spotUtil).getMyTrades("BTCUSDT", 1000L, 1000);
        verify(service, times(2)).saveOrUpdateBatch(anyCollection());
        verify(matcherService).initializeAndMatch(7, "BTCUSDT");
        assertNull(BinanceAccountContextHolder.get());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldRunSpotMatcherWhenNoIncrementalTradesExist() {
        BinanceSpotUtil spotUtil = mock(BinanceSpotUtil.class);
        BinanceSpotTradeMatcherService matcherService = mock(BinanceSpotTradeMatcherService.class);
        BinanceTradeInfoServiceImpl service = spy(new BinanceTradeInfoServiceImpl());
        ReflectionTestUtils.setField(service, "binanceSpotUtil", spotUtil);
        ReflectionTestUtils.setField(service, "binanceSpotTradeMatcherService", matcherService);
        doReturn(Collections.emptyList()).when(service).list(any(Wrapper.class));
        when(spotUtil.getMyTrades("BNBUSDT", 0L, 1000)).thenReturn(Collections.emptyList());

        int count = service.syncTradeInfo(account(9), "BNBUSDT");

        assertEquals(0, count);
        verify(service, times(0)).saveOrUpdateBatch(anyCollection());
        verify(matcherService).initializeAndMatch(9, "BNBUSDT");
        assertNull(BinanceAccountContextHolder.get());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldStartUsdFuturesAfterLatestStoredTradeId() {
        BinanceUsdFuturesUtil futuresUtil = mock(BinanceUsdFuturesUtil.class);
        BinanceFuturesTradeInfoServiceImpl service = spy(new BinanceFuturesTradeInfoServiceImpl());
        ReflectionTestUtils.setField(service, "binanceUsdFuturesUtil", futuresUtil);
        BinanceFuturesTradeInfo latest = new BinanceFuturesTradeInfo();
        latest.setId(50L);
        doReturn(Collections.singletonList(latest), Collections.emptyList())
                .when(service).list(any(Wrapper.class));
        doReturn(true).when(service).saveOrUpdateBatch(anyCollection());
        BinanceFuturesTradeInfo next = new BinanceFuturesTradeInfo();
        next.setId(51L);
        when(futuresUtil.userTradesFromId("BTCUSDT", 51L, 1000))
                .thenReturn(Collections.singletonList(next));

        int count = service.sync(account(7));

        assertEquals(1, count);
        verify(futuresUtil).userTradesFromId("BTCUSDT", 51L, 1000);
        verify(service).saveOrUpdateBatch(anyCollection());
        assertNull(BinanceAccountContextHolder.get());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldStopCoinFuturesSyncWhenNoIncrementalTradesExist() {
        BinanceCoinFuturesUtil futuresUtil = mock(BinanceCoinFuturesUtil.class);
        BinanceCoinFuturesTradeInfoServiceImpl service = spy(new BinanceCoinFuturesTradeInfoServiceImpl());
        ReflectionTestUtils.setField(service, "binanceCoinFuturesUtil", futuresUtil);
        BinanceCoinFuturesTradeInfo latest = new BinanceCoinFuturesTradeInfo();
        latest.setId(80L);
        doReturn(Collections.singletonList(latest)).when(service).list(any(Wrapper.class));
        when(futuresUtil.userTradesFromId("BTCUSD_PERP", 81L, 1000))
                .thenReturn(Collections.emptyList());

        int count = service.sync(account(7));

        assertEquals(0, count);
        verify(futuresUtil).userTradesFromId("BTCUSD_PERP", 81L, 1000);
        verify(service, times(0)).saveOrUpdateBatch(anyCollection());
        assertNull(BinanceAccountContextHolder.get());
    }

    private BinanceAccountInfo account(int uid) {
        BinanceAccountInfo account = new BinanceAccountInfo();
        account.setUid(uid);
        return account;
    }

    private List<BinanceTradeInfo> spotTrades(long firstId, int count) {
        List<BinanceTradeInfo> trades = new ArrayList<>();
        for (long id = firstId; id < firstId + count; id++) {
            BinanceTradeInfo trade = new BinanceTradeInfo();
            trade.setId(id);
            trades.add(trade);
        }
        return trades;
    }
}
