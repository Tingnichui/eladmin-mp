package me.zhengjie.invest.service.impl;

import me.zhengjie.exception.BadRequestException;
import me.zhengjie.invest.domain.BinanceSpotCorePosition;
import me.zhengjie.invest.domain.BinanceSpotTradeMatchState;
import me.zhengjie.invest.domain.dto.BinanceSpotCorePositionAdjustRequest;
import me.zhengjie.invest.domain.dto.BinanceSpotCorePositionBatchLockRequest;
import me.zhengjie.invest.domain.dto.BinanceSpotCorePositionLockRequest;
import me.zhengjie.invest.mapper.BinanceSpotCorePositionMapper;
import me.zhengjie.invest.mapper.BinanceSpotTradeMatchStateMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BinanceSpotCorePositionServiceImplTest {

    @Test
    void shouldLockPartOfCurrentRemainingPosition() {
        BinanceSpotCorePositionMapper coreMapper = mock(BinanceSpotCorePositionMapper.class);
        BinanceSpotTradeMatchStateMapper stateMapper = mock(BinanceSpotTradeMatchStateMapper.class);
        BinanceSpotCorePositionServiceImpl service = new BinanceSpotCorePositionServiceImpl(coreMapper, stateMapper);
        when(stateMapper.findBuyStateForUpdate(7, "BTCUSDT", 11L)).thenReturn(state("0.8"));

        BinanceSpotCorePosition result = service.lock(lockRequest("0.3"));

        ArgumentCaptor<BinanceSpotCorePosition> captor = ArgumentCaptor.forClass(BinanceSpotCorePosition.class);
        verify(coreMapper).insert(captor.capture());
        assertEquals(new BigDecimal("0.3"), captor.getValue().getCoreQty());
        assertEquals(11L, captor.getValue().getTradeId());
        assertNotNull(captor.getValue().getLockedAt());
        assertEquals(captor.getValue(), result);
    }

    @Test
    void shouldRejectCoreQtyGreaterThanRemainingPosition() {
        BinanceSpotCorePositionMapper coreMapper = mock(BinanceSpotCorePositionMapper.class);
        BinanceSpotTradeMatchStateMapper stateMapper = mock(BinanceSpotTradeMatchStateMapper.class);
        BinanceSpotCorePositionServiceImpl service = new BinanceSpotCorePositionServiceImpl(coreMapper, stateMapper);
        when(stateMapper.findBuyStateForUpdate(7, "BTCUSDT", 11L)).thenReturn(state("0.2"));

        assertThrows(BadRequestException.class, () -> service.lock(lockRequest("0.3")));

        verify(coreMapper, never()).insert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldLockEveryRemainingPositionOnceAndKeepExistingLocks() {
        BinanceSpotCorePositionMapper coreMapper = mock(BinanceSpotCorePositionMapper.class);
        BinanceSpotTradeMatchStateMapper stateMapper = mock(BinanceSpotTradeMatchStateMapper.class);
        BinanceSpotCorePositionServiceImpl service = new BinanceSpotCorePositionServiceImpl(coreMapper, stateMapper);
        BinanceSpotTradeMatchState firstState = state("0.8");
        BinanceSpotTradeMatchState secondState = state("0.4");
        secondState.setTradeId(12L);
        BinanceSpotCorePosition existing = position(6L, 12L);
        when(stateMapper.findBuyStateForUpdate(7, "BTCUSDT", 11L)).thenReturn(firstState);
        when(stateMapper.findBuyStateForUpdate(7, "BTCUSDT", 12L)).thenReturn(secondState);
        when(coreMapper.findActiveByTradeForUpdate(7, "BTCUSDT", 12L)).thenReturn(existing);
        BinanceSpotCorePositionBatchLockRequest request = new BinanceSpotCorePositionBatchLockRequest();
        request.setUid(7);
        request.setSymbol("BTCUSDT");
        request.setTradeIds(Arrays.asList(11L, 12L, 11L));

        List<BinanceSpotCorePosition> results = service.lockAll(request);

        ArgumentCaptor<BinanceSpotCorePosition> captor = ArgumentCaptor.forClass(BinanceSpotCorePosition.class);
        verify(coreMapper).insert(captor.capture());
        assertEquals(new BigDecimal("0.8"), captor.getValue().getCoreQty());
        assertEquals(11L, captor.getValue().getTradeId());
        assertEquals(2, results.size());
        assertEquals(existing, results.get(1));
        verify(stateMapper).initializeFromTrades(7, "BTCUSDT");
    }

    @Test
    void shouldAdjustOnlyActiveCorePosition() {
        BinanceSpotCorePositionMapper coreMapper = mock(BinanceSpotCorePositionMapper.class);
        BinanceSpotTradeMatchStateMapper stateMapper = mock(BinanceSpotTradeMatchStateMapper.class);
        BinanceSpotCorePositionServiceImpl service = new BinanceSpotCorePositionServiceImpl(coreMapper, stateMapper);
        BinanceSpotCorePosition position = position();
        when(coreMapper.selectById(5L)).thenReturn(position);
        when(coreMapper.findByIdForUpdate(5L)).thenReturn(position);
        when(stateMapper.findBuyStateForUpdate(7, "BTCUSDT", 11L)).thenReturn(state("0.8"));
        BinanceSpotCorePositionAdjustRequest request = new BinanceSpotCorePositionAdjustRequest();
        request.setCoreQty(new BigDecimal("0.6"));
        request.setRemark("长期持仓");

        BinanceSpotCorePosition result = service.adjust(5L, request);

        assertEquals(new BigDecimal("0.6"), result.getCoreQty());
        assertEquals("长期持仓", result.getRemark());
        verify(coreMapper).updateById(position);
    }

    @Test
    void shouldReleaseWithoutDeletingHistory() {
        BinanceSpotCorePositionMapper coreMapper = mock(BinanceSpotCorePositionMapper.class);
        BinanceSpotTradeMatchStateMapper stateMapper = mock(BinanceSpotTradeMatchStateMapper.class);
        BinanceSpotCorePositionServiceImpl service = new BinanceSpotCorePositionServiceImpl(coreMapper, stateMapper);
        BinanceSpotCorePosition position = position();
        when(coreMapper.selectById(5L)).thenReturn(position);
        when(coreMapper.findByIdForUpdate(5L)).thenReturn(position);
        when(stateMapper.findBuyStateForUpdate(7, "BTCUSDT", 11L)).thenReturn(state("0.8"));

        BinanceSpotCorePosition result = service.release(5L);

        assertNotNull(result.getReleasedAt());
        verify(coreMapper).updateById(position);
        verify(coreMapper, never()).deleteById(5L);
    }

    @Test
    void shouldReleaseDistinctCorePositionsInOneBatch() {
        BinanceSpotCorePositionMapper coreMapper = mock(BinanceSpotCorePositionMapper.class);
        BinanceSpotTradeMatchStateMapper stateMapper = mock(BinanceSpotTradeMatchStateMapper.class);
        BinanceSpotCorePositionServiceImpl service = new BinanceSpotCorePositionServiceImpl(coreMapper, stateMapper);
        BinanceSpotCorePosition first = position(5L, 11L);
        BinanceSpotCorePosition second = position(6L, 12L);
        when(coreMapper.selectById(5L)).thenReturn(first);
        when(coreMapper.selectById(6L)).thenReturn(second);
        when(coreMapper.findByIdForUpdate(5L)).thenReturn(first);
        when(coreMapper.findByIdForUpdate(6L)).thenReturn(second);
        when(stateMapper.findBuyStateForUpdate(7, "BTCUSDT", 11L)).thenReturn(state("0.8"));
        BinanceSpotTradeMatchState secondState = state("0.4");
        secondState.setTradeId(12L);
        when(stateMapper.findBuyStateForUpdate(7, "BTCUSDT", 12L)).thenReturn(secondState);

        List<BinanceSpotCorePosition> results = service.releaseAll(Arrays.asList(5L, 6L, 5L));

        assertEquals(2, results.size());
        assertNotNull(first.getReleasedAt());
        assertNotNull(second.getReleasedAt());
        verify(coreMapper).updateById(first);
        verify(coreMapper).updateById(second);
    }

    private BinanceSpotCorePositionLockRequest lockRequest(String qty) {
        BinanceSpotCorePositionLockRequest request = new BinanceSpotCorePositionLockRequest();
        request.setUid(7);
        request.setSymbol("BTCUSDT");
        request.setTradeId(11L);
        request.setCoreQty(new BigDecimal(qty));
        return request;
    }

    private BinanceSpotTradeMatchState state(String remainingQty) {
        BinanceSpotTradeMatchState state = new BinanceSpotTradeMatchState();
        state.setId(3L);
        state.setTradeId(11L);
        state.setUid(7);
        state.setSymbol("BTCUSDT");
        state.setIsBuyer(1);
        state.setRemainingQty(new BigDecimal(remainingQty));
        return state;
    }

    private BinanceSpotCorePosition position() {
        return position(5L, 11L);
    }

    private BinanceSpotCorePosition position(Long id, Long tradeId) {
        BinanceSpotCorePosition position = new BinanceSpotCorePosition();
        position.setId(id);
        position.setUid(7);
        position.setSymbol("BTCUSDT");
        position.setTradeId(tradeId);
        position.setCoreQty(new BigDecimal("0.3"));
        return position;
    }
}
