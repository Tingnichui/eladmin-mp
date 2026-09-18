package me.zhengjie.invest.service.impl;

import me.zhengjie.invest.constants.BinanceEnum;
import me.zhengjie.invest.domain.BinanceTradeInfo;
import me.zhengjie.invest.domain.dto.BinanceTradeInfoQueryCriteria;
import me.zhengjie.invest.domain.dto.BinanceTradeStatsInfoVO;
import me.zhengjie.invest.mapper.BinanceTradeInfoMapper;
import me.zhengjie.invest.util.BinanceSpotUtil;
import me.zhengjie.utils.RedisUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BinanceTradeInfoServiceImplTest {

    private final BinanceTradeInfoMapper mapper = mock(BinanceTradeInfoMapper.class);
    private final BinanceSpotUtil spotUtil = mock(BinanceSpotUtil.class);
    private final RedisUtils redisUtils = mock(RedisUtils.class);
    private final BinanceTradeInfoServiceImpl service = new BinanceTradeInfoServiceImpl();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "binanceTradeInfoMapper", mapper);
        ReflectionTestUtils.setField(service, "binanceSpotUtil", spotUtil);
        ReflectionTestUtils.setField(service, "redisUtils", redisUtils);
    }

    @Test
    void shouldReturnStableEmptyStatsWithoutMutatingCriteria() {
        BinanceTradeInfoQueryCriteria criteria = criteria();
        when(mapper.findAll(any(BinanceTradeInfoQueryCriteria.class)))
                .thenReturn(Collections.emptyList());

        BinanceTradeStatsInfoVO result = service.stats(criteria, null);

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
    void shouldReturnEmptyPositionWhenAllTradesAreClosed() {
        String cacheKey = "SPOT_LAST_NET_PNL:1:BTCUSDT";
        BinanceTradeInfo open = trade(1L, "100", "1", 1_000L);
        BinanceTradeInfo close = trade(2L, "110", "1", 2_000L);
        when(redisUtils.get(cacheKey)).thenReturn(new BigDecimal("8.000"));
        when(mapper.findAll(any(BinanceTradeInfoQueryCriteria.class)))
                .thenReturn(new ArrayList<>(Collections.singletonList(open)))
                .thenReturn(new ArrayList<>(Collections.singletonList(close)));

        BinanceTradeStatsInfoVO result = service.stats(criteria(), null);

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
    void shouldNotUseRealtimeComparisonCacheForHistoricalStats() {
        BinanceTradeInfoQueryCriteria criteria = criteria();
        criteria.setEndTime(new Timestamp(5_000L));
        when(mapper.findAll(any(BinanceTradeInfoQueryCriteria.class)))
                .thenReturn(Collections.emptyList());

        BinanceTradeStatsInfoVO result = service.stats(criteria, null);

        assertNull(result.getLastNetPnl());
        verify(redisUtils, never()).get(any(String.class));
        verify(redisUtils, never()).set(any(String.class), any());
    }

    @Test
    void shouldKeepHistoricalStatsWhenRealtimePriceFails() {
        BinanceTradeInfo open = trade(1L, "100", "2", 1_000L);
        when(mapper.findAll(any(BinanceTradeInfoQueryCriteria.class)))
                .thenReturn(new ArrayList<>(Collections.singletonList(open)))
                .thenReturn(Collections.emptyList());
        when(spotUtil.getPrice(BinanceEnum.SYMBOL.BTCUSDT))
                .thenThrow(new RuntimeException("proxy unavailable"));

        BinanceTradeStatsInfoVO result = service.stats(criteria(), null);

        assertEquals(new BigDecimal("2"), result.getPosQty());
        assertEquals(new BigDecimal("200"), result.getPosAmount());
        assertEquals(new BigDecimal("100.00000000"), result.getPosAvgPrice());
        assertNull(result.getCurrentSpotPrice());
        assertTrue(result.getTradeList().isEmpty());
        assertTrue(result.getWarnings().contains("现货持仓实时估值暂不可用"));
    }

    private BinanceTradeInfoQueryCriteria criteria() {
        BinanceTradeInfoQueryCriteria criteria = new BinanceTradeInfoQueryCriteria();
        criteria.setUid(1);
        criteria.setSymbol(BinanceEnum.SYMBOL.BTCUSDT.name());
        return criteria;
    }

    private BinanceTradeInfo trade(Long id, String price, String qty, long time) {
        BinanceTradeInfo trade = new BinanceTradeInfo();
        trade.setId(id);
        trade.setPrice(new BigDecimal(price));
        trade.setQty(new BigDecimal(qty));
        trade.setTime(new Timestamp(time));
        return trade;
    }
}
