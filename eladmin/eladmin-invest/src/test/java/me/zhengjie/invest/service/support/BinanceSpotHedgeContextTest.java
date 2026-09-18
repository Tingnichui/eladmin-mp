package me.zhengjie.invest.service.support;

import me.zhengjie.invest.domain.BinanceTradeInfo;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BinanceSpotHedgeContextTest {

    @Test
    void shouldShareSpotAvailabilityAcrossAllocationsWithoutPersistence() {
        BinanceTradeInfo first = trade(1L, "100", "1");
        BinanceTradeInfo second = trade(2L, "110", "1");
        BinanceTradeInfo third = trade(3L, "120", "1");
        BinanceSpotHedgeContext context = new BinanceSpotHedgeContext(Arrays.asList(third, first, second));

        BinanceSpotHedgeContext.HedgeResult usdResult = context.allocate(
                new BigDecimal("100"), new BigDecimal("110"), new BigDecimal("1.5"), 100
        );
        BinanceSpotHedgeContext.HedgeResult coinResult = context.allocate(
                new BigDecimal("100"), new BigDecimal("110"), BigDecimal.ONE, 100
        );

        assertEquals(0, usdResult.getQty().compareTo(new BigDecimal("1.5")));
        assertEquals(0, usdResult.getAmount().compareTo(new BigDecimal("155")));
        assertEquals(0, coinResult.getQty().compareTo(BigDecimal.ONE));
        assertEquals(0, coinResult.getAmount().compareTo(new BigDecimal("115")));
        assertEquals(0, context.getHedgedQty(1L).compareTo(BigDecimal.ONE));
        assertEquals(0, context.getHedgedQty(2L).compareTo(BigDecimal.ONE));
        assertEquals(0, context.getHedgedQty(3L).compareTo(new BigDecimal("0.5")));
        assertEquals(3, context.getHedgedTrades().size());
    }

    private BinanceTradeInfo trade(Long id, String price, String qty) {
        BinanceTradeInfo trade = new BinanceTradeInfo();
        trade.setId(id);
        trade.setPrice(new BigDecimal(price));
        trade.setQty(new BigDecimal(qty));
        return trade;
    }
}
