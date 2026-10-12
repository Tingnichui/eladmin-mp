package me.zhengjie.invest.service.support;

import me.zhengjie.invest.constants.BinanceEnum;
import me.zhengjie.invest.domain.BinanceTradeInfo;
import me.zhengjie.invest.domain.InvestKlinesRecord;
import me.zhengjie.invest.domain.dto.SpotActualFeeRebuildResult;
import me.zhengjie.invest.mapper.BinanceTradeInfoMapper;
import me.zhengjie.invest.util.BinanceSpotUtil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

class SpotCommissionValuationServiceTest {

    private final BinanceSpotUtil spotUtil = mock(BinanceSpotUtil.class);
    private final SpotCommissionValuationService service = new SpotCommissionValuationService(
            spotUtil, mock(BinanceTradeInfoMapper.class));

    @Test
    void shouldKeepQuoteAssetCommissionOneToOne() {
        BinanceTradeInfo trade = trade("USDT", "0.25", "50000");

        service.value(trade);

        assertEquals(new BigDecimal("1"), trade.getCommissionQuotePrice());
        assertEquals(new BigDecimal("0.2500000000000000"), trade.getCommissionQuoteAmount());
        assertEquals("QUOTE_ASSET", trade.getCommissionValuationSource());
        assertEquals(SpotCommissionValuationService.STATUS_COMPLETED,
                trade.getCommissionValuationStatus());
    }

    @Test
    void shouldUseExecutionPriceForBaseAssetCommission() {
        BinanceTradeInfo trade = trade("BTC", "0.0001", "50000");

        service.value(trade);

        assertEquals(new BigDecimal("50000"), trade.getCommissionQuotePrice());
        assertEquals(new BigDecimal("5.0000000000000000"), trade.getCommissionQuoteAmount());
        assertEquals("TRADE_PRICE", trade.getCommissionValuationSource());
    }

    @Test
    void shouldUseHistoricalBnbMinuteVwapForThirdAssetCommission() {
        BinanceTradeInfo trade = trade("BNB", "0.002", "50000");
        InvestKlinesRecord kline = new InvestKlinesRecord();
        kline.setVolume(new BigDecimal("10"));
        kline.setTurnover(new BigDecimal("8000"));
        when(spotUtil.getKlines(BinanceEnum.SYMBOL.BNBUSDT, "1m", 60_000L, 119_999L))
                .thenReturn(Collections.singletonList(kline));

        service.value(trade);

        assertEquals(new BigDecimal("800.0000000000000000"), trade.getCommissionQuotePrice());
        assertEquals(new BigDecimal("1.6000000000000000"), trade.getCommissionQuoteAmount());
        assertEquals("KLINE_1M", trade.getCommissionValuationSource());
    }

    private BinanceTradeInfo trade(String commissionAsset, String commission, String price) {
        BinanceTradeInfo trade = new BinanceTradeInfo();
        trade.setId(1L);
        trade.setSymbol("BTCUSDT");
        trade.setPrice(new BigDecimal(price));
        trade.setCommissionAsset(commissionAsset);
        trade.setCommission(new BigDecimal(commission));
        trade.setTime(new Timestamp(75_000L));
        return trade;
    }

    @Test
    void shouldRetryPendingAndFailedFeesWithoutChangingCompletedTrades() {
        BinanceTradeInfoMapper mapper = mock(BinanceTradeInfoMapper.class);
        SpotCommissionValuationService valuation = new SpotCommissionValuationService(spotUtil, mapper);
        BinanceTradeInfo completed = trade("BNB", "0.002", "50000");
        completed.setCommissionValuationStatus("COMPLETED");
        completed.setCommissionQuoteAmount(new BigDecimal("1.6"));
        BinanceTradeInfo pending = trade("USDT", "0.25", "50000");
        pending.setCommissionValuationStatus("PENDING");
        BinanceTradeInfo failed = trade("BTC", "0.0001", "50000");
        failed.setCommissionValuationStatus("FAILED");
        when(mapper.findScopeForFeeValuation(7, "BTCUSDT"))
                .thenReturn(Arrays.asList(completed, pending, failed));

        SpotActualFeeRebuildResult result = valuation.valueIncompleteScope(7, "BTCUSDT");

        assertEquals(2, result.getTradeCount());
        assertEquals(2, result.getValuedCount());
        assertEquals(0, result.getFailedCount());
        verify(mapper, never()).updateById(completed);
        verify(mapper).updateById(pending);
        verify(mapper).updateById(failed);
    }
}
