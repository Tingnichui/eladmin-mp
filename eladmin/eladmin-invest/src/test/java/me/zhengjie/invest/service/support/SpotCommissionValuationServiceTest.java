package me.zhengjie.invest.service.support;

import me.zhengjie.invest.constants.BinanceEnum;
import me.zhengjie.invest.domain.BinanceTradeInfo;
import me.zhengjie.invest.domain.InvestKlinesRecord;
import me.zhengjie.invest.mapper.BinanceTradeInfoMapper;
import me.zhengjie.invest.util.BinanceSpotUtil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
}
