package me.zhengjie.invest.service.impl;

import me.zhengjie.invest.constants.BinanceEnum;
import me.zhengjie.invest.domain.BinanceSpotTradeMatchState;
import me.zhengjie.invest.domain.dto.BinanceSpotTradeStatsAggregate;
import me.zhengjie.invest.domain.dto.BinanceTradeInfoQueryCriteria;
import me.zhengjie.invest.domain.dto.BinanceTradeStatsInfoVO;
import me.zhengjie.invest.mapper.BinanceSpotTradeMatchMapper;
import me.zhengjie.invest.mapper.BinanceSpotTradeMatchStateMapper;
import me.zhengjie.invest.mapper.BinanceTradeInfoMapper;
import me.zhengjie.invest.util.BinanceSpotUtil;
import me.zhengjie.utils.RedisUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BinanceTradeInfoServiceImplTest {

    private final BinanceTradeInfoMapper mapper = mock(BinanceTradeInfoMapper.class);
    private final BinanceSpotTradeMatchMapper matchMapper = mock(BinanceSpotTradeMatchMapper.class);
    private final BinanceSpotTradeMatchStateMapper stateMapper = mock(BinanceSpotTradeMatchStateMapper.class);
    private final BinanceSpotUtil spotUtil = mock(BinanceSpotUtil.class);
    private final RedisUtils redisUtils = mock(RedisUtils.class);
    private final BinanceTradeInfoServiceImpl service = new BinanceTradeInfoServiceImpl();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "binanceTradeInfoMapper", mapper);
        ReflectionTestUtils.setField(service, "binanceSpotTradeMatchMapper", matchMapper);
        ReflectionTestUtils.setField(service, "binanceSpotTradeMatchStateMapper", stateMapper);
        ReflectionTestUtils.setField(service, "binanceSpotUtil", spotUtil);
        ReflectionTestUtils.setField(service, "redisUtils", redisUtils);
        when(matchMapper.aggregateStats(any(), any())).thenReturn(new BinanceSpotTradeStatsAggregate());
        when(stateMapper.sumStatsUnmatchedSellQty(any(), any())).thenReturn(BigDecimal.ZERO);
        when(stateMapper.findStatsOpenBuys(any(), any())).thenReturn(Collections.emptyList());
    }

    @Test
    void shouldReturnStableEmptyStatsWithoutMutatingCriteria() {
        BinanceTradeInfoQueryCriteria criteria = criteria();

        BinanceTradeStatsInfoVO result = service.stats(criteria);

        assertEquals(BigDecimal.ZERO, result.getPosQty());
        assertEquals(BigDecimal.ZERO, result.getPosAmount());
        assertNull(result.getPosAvgPrice());
        assertNull(result.getRoi());
        assertTrue(result.getTradeList().isEmpty());
        assertTrue(result.getWarnings().isEmpty());
        assertNull(criteria.getIsBuyer());
        assertNull(criteria.getHedgedFlag());
        assertEquals("time", criteria.getOrderColumn());
        assertEquals("desc", criteria.getOrderDirection());
        verify(spotUtil, never()).getPrice(any(BinanceEnum.SYMBOL.class));
    }

    @Test
    void shouldReturnPersistedClosedTradeStats() {
        String cacheKey = "SPOT_LAST_NET_PNL:1:BTCUSDT";
        when(redisUtils.get(cacheKey)).thenReturn(new BigDecimal("8.000"));
        when(matchMapper.aggregateStats(eq(1), eq("BTCUSDT")))
                .thenReturn(aggregate("100", "110", "10", "0.210", "9.790"));

        BinanceTradeStatsInfoVO result = service.stats(criteria());

        assertEquals(BigDecimal.ZERO, result.getPosQty());
        assertEquals(BigDecimal.ZERO, result.getPosAmount());
        assertNull(result.getPosAvgPrice());
        assertEquals(new BigDecimal("100"), result.getTotalBuyAmount());
        assertEquals(new BigDecimal("110"), result.getTotalSellAmount());
        assertEquals(new BigDecimal("9.790"), result.getNetPnl());
        assertEquals(new BigDecimal("0.09790000"), result.getRoi());
        assertEquals(new BigDecimal("8.000"), result.getLastNetPnl());
        assertTrue(result.getTradeList().isEmpty());
        verify(redisUtils).set(cacheKey, new BigDecimal("9.790"));
        verify(spotUtil, never()).getPrice(any(BinanceEnum.SYMBOL.class));
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
