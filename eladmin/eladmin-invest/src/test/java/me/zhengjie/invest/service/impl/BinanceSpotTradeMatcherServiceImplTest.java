package me.zhengjie.invest.service.impl;

import me.zhengjie.invest.domain.BinanceSpotTradeMatch;
import me.zhengjie.invest.domain.BinanceSpotTradeMatchState;
import me.zhengjie.invest.domain.dto.BinanceSpotTradeMatchResult;
import me.zhengjie.invest.mapper.BinanceSpotTradeMatchMapper;
import me.zhengjie.invest.mapper.BinanceSpotTradeMatchStateMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BinanceSpotTradeMatcherServiceImplTest {

    @Test
    void shouldInitializeAndPersistFifoMatches() {
        BinanceSpotTradeMatchStateMapper stateMapper = mock(BinanceSpotTradeMatchStateMapper.class);
        BinanceSpotTradeMatchMapper matchMapper = mock(BinanceSpotTradeMatchMapper.class);
        BinanceSpotTradeMatcherServiceImpl service = new BinanceSpotTradeMatcherServiceImpl(stateMapper, matchMapper);
        BinanceSpotTradeMatchState sell = state(30L, 3L, 0, "1.5", "150", 3_000L);
        BinanceSpotTradeMatchState firstBuy = state(10L, 1L, 1, "1", "120", 1_000L);
        BinanceSpotTradeMatchState secondBuy = state(20L, 2L, 1, "2", "100", 2_000L);
        when(stateMapper.initializeFromTrades(7, "BTCUSDT")).thenReturn(3);
        when(stateMapper.findPendingSellsForUpdate(7, "BTCUSDT"))
                .thenReturn(Collections.singletonList(sell));
        when(stateMapper.findAvailableBuysForUpdate(7, "BTCUSDT", sell.getTradeTime(), sell.getTradeId()))
                .thenReturn(Arrays.asList(firstBuy, secondBuy));

        BinanceSpotTradeMatchResult result = service.initializeAndMatch(7, "BTCUSDT");

        assertEquals(3, result.getInitializedCount());
        assertEquals(2, result.getMatchCount());
        assertEquals(1, result.getCompletedSellCount());
        assertEquals(0, result.getExceptionSellCount());
        assertEquals(new BigDecimal("1.5"), result.getMatchedQty());

        ArgumentCaptor<BinanceSpotTradeMatch> matchCaptor = ArgumentCaptor.forClass(BinanceSpotTradeMatch.class);
        verify(matchMapper, org.mockito.Mockito.times(2)).insert(matchCaptor.capture());
        List<BinanceSpotTradeMatch> matches = matchCaptor.getAllValues();
        assertEquals(1L, matches.get(0).getBuyTradeId());
        assertEquals(new BigDecimal("1"), matches.get(0).getMatchedQty());
        assertEquals(new BigDecimal("120"), matches.get(0).getBuyAmount());
        assertEquals(new BigDecimal("0.270"), matches.get(0).getFee());
        assertEquals(new BigDecimal("29.730"), matches.get(0).getNetPnl());
        assertEquals(2L, matches.get(1).getBuyTradeId());
        assertEquals(new BigDecimal("0.5"), matches.get(1).getMatchedQty());

        verify(stateMapper).updateMatchProgress(10L, new BigDecimal("1"), BigDecimal.ZERO, "COMPLETED");
        verify(stateMapper).updateMatchProgress(20L, new BigDecimal("0.5"), new BigDecimal("1.5"), "PARTIAL");
        verify(stateMapper).updateMatchProgress(30L, new BigDecimal("1.5"), new BigDecimal("0.0"), "COMPLETED");
    }

    @Test
    void shouldMarkSellAsExceptionWhenNoEarlierBuyExists() {
        BinanceSpotTradeMatchStateMapper stateMapper = mock(BinanceSpotTradeMatchStateMapper.class);
        BinanceSpotTradeMatchMapper matchMapper = mock(BinanceSpotTradeMatchMapper.class);
        BinanceSpotTradeMatcherServiceImpl service = new BinanceSpotTradeMatcherServiceImpl(stateMapper, matchMapper);
        BinanceSpotTradeMatchState sell = state(30L, 3L, 0, "1", "150", 3_000L);
        when(stateMapper.findPendingSellsForUpdate(7, "BTCUSDT"))
                .thenReturn(Collections.singletonList(sell));
        when(stateMapper.findAvailableBuysForUpdate(7, "BTCUSDT", sell.getTradeTime(), sell.getTradeId()))
                .thenReturn(Collections.emptyList());

        BinanceSpotTradeMatchResult result = service.match(7, "BTCUSDT");

        assertEquals(0, result.getMatchCount());
        assertEquals(1, result.getExceptionSellCount());
        verify(matchMapper, never()).insert(any(BinanceSpotTradeMatch.class));
        verify(stateMapper).updateMatchProgress(30L, BigDecimal.ZERO, new BigDecimal("1"), "EXCEPTION");
    }

    @Test
    void shouldBeIdempotentWhenNoPendingSellRemains() {
        BinanceSpotTradeMatchStateMapper stateMapper = mock(BinanceSpotTradeMatchStateMapper.class);
        BinanceSpotTradeMatchMapper matchMapper = mock(BinanceSpotTradeMatchMapper.class);
        BinanceSpotTradeMatcherServiceImpl service = new BinanceSpotTradeMatcherServiceImpl(stateMapper, matchMapper);
        when(stateMapper.findPendingSellsForUpdate(7, "BTCUSDT")).thenReturn(Collections.emptyList());

        BinanceSpotTradeMatchResult result = service.match(7, "BTCUSDT");

        assertEquals(0, result.getMatchCount());
        verify(matchMapper, never()).insert(any(BinanceSpotTradeMatch.class));
        verify(stateMapper, never()).findAvailableBuysForUpdate(eq(7), eq("BTCUSDT"), any(), any());
    }

    @Test
    void shouldRejectMissingScope() {
        BinanceSpotTradeMatchStateMapper stateMapper = mock(BinanceSpotTradeMatchStateMapper.class);
        BinanceSpotTradeMatchMapper matchMapper = mock(BinanceSpotTradeMatchMapper.class);
        BinanceSpotTradeMatcherServiceImpl service = new BinanceSpotTradeMatcherServiceImpl(stateMapper, matchMapper);

        assertThrows(IllegalArgumentException.class, () -> service.match(null, "BTCUSDT"));
        assertThrows(IllegalArgumentException.class, () -> service.match(7, " "));
    }

    private BinanceSpotTradeMatchState state(Long stateId,
                                             Long tradeId,
                                             int buyer,
                                             String qty,
                                             String price,
                                             long time) {
        BinanceSpotTradeMatchState state = new BinanceSpotTradeMatchState();
        state.setId(stateId);
        state.setTradeId(tradeId);
        state.setUid(7);
        state.setSymbol("BTCUSDT");
        state.setIsBuyer(buyer);
        state.setTradeTime(new Timestamp(time));
        state.setOriginalQty(new BigDecimal(qty));
        state.setMatchedQty(BigDecimal.ZERO);
        state.setRemainingQty(new BigDecimal(qty));
        state.setMatchStatus("PENDING");
        state.setPrice(new BigDecimal(price));
        return state;
    }
}
