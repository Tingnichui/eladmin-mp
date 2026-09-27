package me.zhengjie.invest.service.impl;

import me.zhengjie.invest.domain.BinanceSpotTradeMatch;
import me.zhengjie.invest.domain.BinanceSpotTradeMatchState;
import me.zhengjie.invest.domain.dto.BinanceSpotSellSourceDto;
import me.zhengjie.invest.domain.dto.BinanceSpotTradeMatchResult;
import me.zhengjie.invest.domain.dto.SpotActualFeeRebuildResult;
import me.zhengjie.invest.domain.dto.SpotTradeMatchFeeRevaluation;
import me.zhengjie.invest.mapper.BinanceSpotTradeMatchMapper;
import me.zhengjie.invest.mapper.BinanceSpotTradeMatchStateMapper;
import me.zhengjie.invest.service.support.BinanceSpotSellSourceStore;
import me.zhengjie.invest.service.support.SpotCommissionValuationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

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
    void shouldRevalueExistingMatchesWithoutRebuildingFifo() {
        BinanceSpotTradeMatchStateMapper stateMapper = mock(BinanceSpotTradeMatchStateMapper.class);
        BinanceSpotTradeMatchMapper matchMapper = mock(BinanceSpotTradeMatchMapper.class);
        BinanceSpotSellSourceStore sourceStore = mock(BinanceSpotSellSourceStore.class);
        SpotCommissionValuationService valuationService = mock(SpotCommissionValuationService.class);
        BinanceSpotTradeMatcherServiceImpl service = new BinanceSpotTradeMatcherServiceImpl(
                stateMapper, matchMapper, sourceStore);
        ReflectionTestUtils.setField(service, "commissionValuationService", valuationService);
        SpotActualFeeRebuildResult valuationResult = new SpotActualFeeRebuildResult();
        valuationResult.setTradeCount(2);
        valuationResult.setValuedCount(2);
        when(valuationService.valueScope(7, "BTCUSDT")).thenReturn(valuationResult);
        SpotTradeMatchFeeRevaluation row = new SpotTradeMatchFeeRevaluation();
        row.setId(99L);
        row.setBuyTradeId(1L);
        row.setSellTradeId(2L);
        row.setMatchedQty(new BigDecimal("0.5"));
        row.setPnl(new BigDecimal("10"));
        row.setBuyTradeQty(new BigDecimal("1"));
        row.setBuyCommission(new BigDecimal("0.001"));
        row.setBuyCommissionAsset("BTC");
        row.setBuyCommissionQuoteAmount(new BigDecimal("1"));
        row.setBuyCommissionValuationStatus("COMPLETED");
        row.setSellTradeQty(new BigDecimal("0.5"));
        row.setSellCommission(new BigDecimal("0.2"));
        row.setSellCommissionAsset("USDT");
        row.setSellCommissionQuoteAmount(new BigDecimal("0.2"));
        row.setSellCommissionValuationStatus("COMPLETED");
        when(matchMapper.findFeeRevaluationRows(7, "BTCUSDT")).thenReturn(Collections.singletonList(row));
        when(matchMapper.updateFeeValuation(
                eq(99L), eq("BTC"), any(), any(), eq("USDT"), any(), any(),
                eq(1), any(), any())).thenReturn(1);

        SpotActualFeeRebuildResult result = service.rebuildWithActualFees(7, "BTCUSDT");

        assertEquals(1, result.getUpdatedMatchCount());
        assertEquals(true, result.isRebuilt());
        verify(stateMapper, never()).initializeFromTrades(any(), any());
        verify(matchMapper).updateFeeValuation(
                eq(99L), eq("BTC"), eq(new BigDecimal("0.0005000000000000")),
                eq(new BigDecimal("0.5000000000000000")), eq("USDT"),
                eq(new BigDecimal("0.2000000000000000")), eq(new BigDecimal("0.2000000000000000")),
                eq(1), eq(new BigDecimal("0.7000000000000000")),
                eq(new BigDecimal("9.3000000000000000")));
    }

    @Test
    void shouldInitializeAndPersistFifoMatches() {
        BinanceSpotTradeMatchStateMapper stateMapper = mock(BinanceSpotTradeMatchStateMapper.class);
        BinanceSpotTradeMatchMapper matchMapper = mock(BinanceSpotTradeMatchMapper.class);
        BinanceSpotSellSourceStore sourceStore = mock(BinanceSpotSellSourceStore.class);
        BinanceSpotTradeMatcherServiceImpl service = new BinanceSpotTradeMatcherServiceImpl(
                stateMapper, matchMapper, sourceStore);
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
        assertEquals(0, new BigDecimal("0.270").compareTo(matches.get(0).getFee()));
        assertEquals(0, new BigDecimal("29.730").compareTo(matches.get(0).getNetPnl()));
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
        BinanceSpotSellSourceStore sourceStore = mock(BinanceSpotSellSourceStore.class);
        BinanceSpotTradeMatcherServiceImpl service = new BinanceSpotTradeMatcherServiceImpl(
                stateMapper, matchMapper, sourceStore);
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
    void shouldExcludeActiveCoreQtyWithoutChangingTotalRemainingQty() {
        BinanceSpotTradeMatchStateMapper stateMapper = mock(BinanceSpotTradeMatchStateMapper.class);
        BinanceSpotTradeMatchMapper matchMapper = mock(BinanceSpotTradeMatchMapper.class);
        BinanceSpotSellSourceStore sourceStore = mock(BinanceSpotSellSourceStore.class);
        BinanceSpotTradeMatcherServiceImpl service = new BinanceSpotTradeMatcherServiceImpl(
                stateMapper, matchMapper, sourceStore);
        BinanceSpotTradeMatchState sell = state(30L, 3L, 0, "0.5", "150", 3_000L);
        BinanceSpotTradeMatchState firstBuy = state(10L, 1L, 1, "1", "120", 1_000L);
        firstBuy.setActiveCoreQty(new BigDecimal("0.7"));
        BinanceSpotTradeMatchState secondBuy = state(20L, 2L, 1, "2", "100", 2_000L);
        when(stateMapper.findPendingSellsForUpdate(7, "BTCUSDT"))
                .thenReturn(Collections.singletonList(sell));
        when(stateMapper.findAvailableBuysForUpdate(7, "BTCUSDT", sell.getTradeTime(), sell.getTradeId()))
                .thenReturn(Arrays.asList(firstBuy, secondBuy));

        BinanceSpotTradeMatchResult result = service.match(7, "BTCUSDT");

        assertEquals(2, result.getMatchCount());
        assertEquals(new BigDecimal("0.5"), result.getMatchedQty());
        verify(stateMapper).updateMatchProgress(10L, new BigDecimal("0.3"), new BigDecimal("0.7"), "PARTIAL");
        verify(stateMapper).updateMatchProgress(20L, new BigDecimal("0.2"), new BigDecimal("1.8"), "PARTIAL");
        verify(stateMapper).updateMatchProgress(30L, new BigDecimal("0.5"), new BigDecimal("0.0"), "COMPLETED");
    }

    @Test
    void shouldBeIdempotentWhenNoPendingSellRemains() {
        BinanceSpotTradeMatchStateMapper stateMapper = mock(BinanceSpotTradeMatchStateMapper.class);
        BinanceSpotTradeMatchMapper matchMapper = mock(BinanceSpotTradeMatchMapper.class);
        BinanceSpotSellSourceStore sourceStore = mock(BinanceSpotSellSourceStore.class);
        BinanceSpotTradeMatcherServiceImpl service = new BinanceSpotTradeMatcherServiceImpl(
                stateMapper, matchMapper, sourceStore);
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
        BinanceSpotSellSourceStore sourceStore = mock(BinanceSpotSellSourceStore.class);
        BinanceSpotTradeMatcherServiceImpl service = new BinanceSpotTradeMatcherServiceImpl(
                stateMapper, matchMapper, sourceStore);

        assertThrows(IllegalArgumentException.class, () -> service.match(null, "BTCUSDT"));
        assertThrows(IllegalArgumentException.class, () -> service.match(7, " "));
    }

    @Test
    void shouldMatchOnlyTheTradeReferencedBySellSource() {
        BinanceSpotTradeMatchStateMapper stateMapper = mock(BinanceSpotTradeMatchStateMapper.class);
        BinanceSpotTradeMatchMapper matchMapper = mock(BinanceSpotTradeMatchMapper.class);
        BinanceSpotSellSourceStore sourceStore = mock(BinanceSpotSellSourceStore.class);
        BinanceSpotTradeMatcherServiceImpl service = new BinanceSpotTradeMatcherServiceImpl(
                stateMapper, matchMapper, sourceStore);
        BinanceSpotTradeMatchState sell = state(30L, 3L, 0, "0.5", "150", 3_000L);
        sell.setOrderId(300L);
        BinanceSpotTradeMatchState referencedBuy = state(20L, 2L, 1, "1", "100", 2_000L);
        BinanceSpotSellSourceDto source = tradeSource(300L, 2L);
        when(stateMapper.findPendingSellsForUpdate(7, "BTCUSDT"))
                .thenReturn(Collections.singletonList(sell));
        when(sourceStore.find(7, "BTCUSDT", 300L)).thenReturn(source);
        when(stateMapper.findAvailableBuysByTradeIdForUpdate(
                7, "BTCUSDT", 2L, sell.getTradeTime(), sell.getTradeId()))
                .thenReturn(Collections.singletonList(referencedBuy));

        BinanceSpotTradeMatchResult result = service.match(7, "BTCUSDT");

        assertEquals(1, result.getCompletedSellCount());
        ArgumentCaptor<BinanceSpotTradeMatch> matchCaptor = ArgumentCaptor.forClass(BinanceSpotTradeMatch.class);
        verify(matchMapper).insert(matchCaptor.capture());
        assertEquals(2L, matchCaptor.getValue().getBuyTradeId());
        verify(stateMapper, never()).findAvailableBuysForUpdate(
                eq(7), eq("BTCUSDT"), any(), any());
    }

    @Test
    void shouldKeepLinkedSellInsideSourceOrderAndMarkShortage() {
        BinanceSpotTradeMatchStateMapper stateMapper = mock(BinanceSpotTradeMatchStateMapper.class);
        BinanceSpotTradeMatchMapper matchMapper = mock(BinanceSpotTradeMatchMapper.class);
        BinanceSpotSellSourceStore sourceStore = mock(BinanceSpotSellSourceStore.class);
        BinanceSpotTradeMatcherServiceImpl service = new BinanceSpotTradeMatcherServiceImpl(
                stateMapper, matchMapper, sourceStore);
        BinanceSpotTradeMatchState sell = state(30L, 3L, 0, "1", "150", 3_000L);
        sell.setOrderId(300L);
        BinanceSpotTradeMatchState orderBuy = state(20L, 2L, 1, "0.4", "100", 2_000L);
        BinanceSpotSellSourceDto source = orderSource(300L, 200L);
        when(stateMapper.findPendingSellsForUpdate(7, "BTCUSDT"))
                .thenReturn(Collections.singletonList(sell));
        when(sourceStore.find(7, "BTCUSDT", 300L)).thenReturn(source);
        when(stateMapper.findAvailableBuysByOrderIdForUpdate(
                7, "BTCUSDT", 200L, sell.getTradeTime(), sell.getTradeId()))
                .thenReturn(Collections.singletonList(orderBuy));

        BinanceSpotTradeMatchResult result = service.match(7, "BTCUSDT");

        assertEquals(1, result.getExceptionSellCount());
        verify(stateMapper).updateMatchProgress(
                30L, new BigDecimal("0.4"), new BigDecimal("0.6"), "SOURCE_EXCEPTION");
        verify(stateMapper, never()).findAvailableBuysForUpdate(
                eq(7), eq("BTCUSDT"), any(), any());
    }

    @Test
    void shouldRejectInvalidSellSourceWithoutFallingBackToFifo() {
        BinanceSpotTradeMatchStateMapper stateMapper = mock(BinanceSpotTradeMatchStateMapper.class);
        BinanceSpotTradeMatchMapper matchMapper = mock(BinanceSpotTradeMatchMapper.class);
        BinanceSpotSellSourceStore sourceStore = mock(BinanceSpotSellSourceStore.class);
        BinanceSpotTradeMatcherServiceImpl service = new BinanceSpotTradeMatcherServiceImpl(
                stateMapper, matchMapper, sourceStore);
        BinanceSpotTradeMatchState sell = state(30L, 3L, 0, "1", "150", 3_000L);
        sell.setOrderId(300L);
        BinanceSpotSellSourceDto source = tradeSource(999L, 2L);
        when(stateMapper.findPendingSellsForUpdate(7, "BTCUSDT"))
                .thenReturn(Collections.singletonList(sell));
        when(sourceStore.find(7, "BTCUSDT", 300L)).thenReturn(source);

        BinanceSpotTradeMatchResult result = service.match(7, "BTCUSDT");

        assertEquals(1, result.getExceptionSellCount());
        verify(stateMapper).updateMatchProgress(
                30L, BigDecimal.ZERO, new BigDecimal("1"), "SOURCE_EXCEPTION");
        verify(stateMapper, never()).findAvailableBuysForUpdate(
                eq(7), eq("BTCUSDT"), any(), any());
        verify(matchMapper, never()).insert(any(BinanceSpotTradeMatch.class));
    }

    @Test
    void shouldFailInsteadOfUsingFifoWhenRedisReadFails() {
        BinanceSpotTradeMatchStateMapper stateMapper = mock(BinanceSpotTradeMatchStateMapper.class);
        BinanceSpotTradeMatchMapper matchMapper = mock(BinanceSpotTradeMatchMapper.class);
        BinanceSpotSellSourceStore sourceStore = mock(BinanceSpotSellSourceStore.class);
        BinanceSpotTradeMatcherServiceImpl service = new BinanceSpotTradeMatcherServiceImpl(
                stateMapper, matchMapper, sourceStore);
        BinanceSpotTradeMatchState sell = state(30L, 3L, 0, "1", "150", 3_000L);
        sell.setOrderId(300L);
        when(stateMapper.findPendingSellsForUpdate(7, "BTCUSDT"))
                .thenReturn(Collections.singletonList(sell));
        when(sourceStore.find(7, "BTCUSDT", 300L))
                .thenThrow(new IllegalStateException("redis unavailable"));

        assertThrows(IllegalStateException.class, () -> service.match(7, "BTCUSDT"));

        verify(stateMapper, never()).findAvailableBuysForUpdate(
                eq(7), eq("BTCUSDT"), any(), any());
        verify(stateMapper, never()).updateMatchProgress(any(), any(), any(), any());
    }

    private BinanceSpotSellSourceDto tradeSource(Long sellOrderId, Long sourceTradeId) {
        BinanceSpotSellSourceDto source = source(sellOrderId);
        source.setSourceType(BinanceSpotSellSourceDto.SourceType.TRADE);
        source.setSourceTradeId(sourceTradeId);
        return source;
    }

    private BinanceSpotSellSourceDto orderSource(Long sellOrderId, Long sourceOrderId) {
        BinanceSpotSellSourceDto source = source(sellOrderId);
        source.setSourceType(BinanceSpotSellSourceDto.SourceType.ORDER);
        source.setSourceOrderId(sourceOrderId);
        return source;
    }

    private BinanceSpotSellSourceDto source(Long sellOrderId) {
        BinanceSpotSellSourceDto source = new BinanceSpotSellSourceDto();
        source.setUid(7);
        source.setSymbol("BTCUSDT");
        source.setSellOrderId(sellOrderId);
        return source;
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
        BigDecimal commission = new BigDecimal(qty).multiply(new BigDecimal(price))
                .multiply(new BigDecimal("0.001"));
        state.setCommission(commission);
        state.setCommissionAsset("USDT");
        state.setCommissionQuoteAmount(commission);
        state.setCommissionValuationStatus(SpotCommissionValuationService.STATUS_COMPLETED);
        return state;
    }
}
