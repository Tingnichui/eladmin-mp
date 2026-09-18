package me.zhengjie.invest.service.support;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import me.zhengjie.invest.constants.BinanceEnum;
import me.zhengjie.invest.domain.BinanceCoinFuturesTradeInfo;
import me.zhengjie.invest.domain.BinanceFuturesTradeInfo;
import me.zhengjie.invest.domain.BinanceTradeInfo;
import me.zhengjie.invest.domain.dto.BinanceTradeInfoQueryCriteria;
import me.zhengjie.invest.mapper.BinanceCoinFuturesTradeInfoMapper;
import me.zhengjie.invest.mapper.BinanceFuturesTradeInfoMapper;
import me.zhengjie.invest.mapper.BinanceTradeInfoMapper;
import me.zhengjie.utils.RedisUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BinanceStatsLocalDataServiceTest {

    private final BinanceTradeInfoMapper spotMapper = mock(BinanceTradeInfoMapper.class);
    private final BinanceFuturesTradeInfoMapper usdMapper = mock(BinanceFuturesTradeInfoMapper.class);
    private final BinanceCoinFuturesTradeInfoMapper coinMapper = mock(BinanceCoinFuturesTradeInfoMapper.class);
    private final RedisUtils redisUtils = mock(RedisUtils.class);
    private final BinanceStatsLocalDataService service = new BinanceStatsLocalDataService(
            spotMapper, usdMapper, coinMapper, redisUtils
    );

    @Test
    @SuppressWarnings("unchecked")
    void shouldLoadEachMarketOnceAndKeepOnlyCurrentFuturesPosition() {
        BinanceTradeInfo spot = spotTrade(1L, 1_000L);
        when(spotMapper.findAll(any(BinanceTradeInfoQueryCriteria.class)))
                .thenReturn(Collections.singletonList(spot));
        when(usdMapper.selectList(any(Wrapper.class))).thenReturn(Arrays.asList(
                usdTrade(1L, 0, 1_000L),
                usdTrade(2L, 1, 2_000L),
                usdTrade(3L, 0, 3_000L)
        ));
        when(coinMapper.selectList(any(Wrapper.class))).thenReturn(Arrays.asList(
                coinTrade(1L, 0, 1_000L),
                coinTrade(2L, 1, 2_000L),
                coinTrade(3L, 0, 3_000L)
        ));
        BinanceTradeInfoQueryCriteria criteria = new BinanceTradeInfoQueryCriteria();
        criteria.setUid(1);
        criteria.setSymbol(BinanceEnum.SYMBOL.BTCUSDT.name());
        Timestamp cutoff = new Timestamp(5_000L);
        criteria.setEndTime(cutoff);

        BinanceStatsLocalDataSnapshot snapshot = service.load(criteria);

        assertEquals(1, snapshot.getSpotTrades().size());
        assertEquals(Collections.singletonList(3L),
                Collections.singletonList(snapshot.getUsdFuturesTrades().get(0).getId()));
        assertEquals(Collections.singletonList(3L),
                Collections.singletonList(snapshot.getCoinFuturesTrades().get(0).getId()));
        assertEquals(new Timestamp(2_000L), snapshot.getCoinPositionStartTime());
        assertTrue(snapshot.getQueryElapsedMillis().containsKey("total"));
        ArgumentCaptor<BinanceTradeInfoQueryCriteria> spotCriteriaCaptor =
                ArgumentCaptor.forClass(BinanceTradeInfoQueryCriteria.class);
        verify(spotMapper, times(1)).findAll(spotCriteriaCaptor.capture());
        verify(usdMapper, times(1)).selectList(any(Wrapper.class));
        verify(coinMapper, times(1)).selectList(any(Wrapper.class));
        assertEquals(cutoff, spotCriteriaCaptor.getValue().getEndTime());
        verify(redisUtils, never()).set(any(String.class), any());
    }

    private BinanceTradeInfo spotTrade(Long id, long time) {
        BinanceTradeInfo trade = new BinanceTradeInfo();
        trade.setId(id);
        trade.setIsBuyer(1);
        trade.setQty(BigDecimal.ONE);
        trade.setPrice(BigDecimal.TEN);
        trade.setTime(new Timestamp(time));
        return trade;
    }

    private BinanceFuturesTradeInfo usdTrade(Long id, int buyer, long time) {
        BinanceFuturesTradeInfo trade = new BinanceFuturesTradeInfo();
        trade.setId(id);
        trade.setBuyer(buyer);
        trade.setSide(buyer == 1 ? "BUY" : "SELL");
        trade.setQty(BigDecimal.ONE);
        trade.setPrice(BigDecimal.TEN);
        trade.setTime(new Timestamp(time));
        return trade;
    }

    private BinanceCoinFuturesTradeInfo coinTrade(Long id, int buyer, long time) {
        BinanceCoinFuturesTradeInfo trade = new BinanceCoinFuturesTradeInfo();
        trade.setId(id);
        trade.setBuyer(buyer);
        trade.setSide(buyer == 1 ? "BUY" : "SELL");
        trade.setQty(BigDecimal.ONE);
        trade.setBaseQty(BigDecimal.ONE);
        trade.setPrice(BigDecimal.TEN);
        trade.setTime(new Timestamp(time));
        return trade;
    }
}
