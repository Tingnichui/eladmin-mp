package me.zhengjie.invest.util;

import me.zhengjie.invest.domain.BinanceTradeInfo;
import me.zhengjie.invest.domain.dto.MatchedTradeInfo;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TradeMatcherUtilTest {

    @Test
    void shouldMatchSpotTradesByTimeUsingFifoAndKeepPartialPosition() {
        List<BinanceTradeInfo> opens = new ArrayList<>(Arrays.asList(
                trade(1L, "120", "1", 1_000L),
                trade(2L, "100", "2", 2_000L)
        ));
        List<BinanceTradeInfo> closes = new ArrayList<>(Collections.singletonList(
                trade(3L, "150", "1.5", 3_000L)
        ));

        List<MatchedTradeInfo> matched = match(opens, closes);

        assertEquals(2, matched.size());
        assertEquals(new BigDecimal("120"), matched.get(0).getOpenPrice());
        assertEquals(new BigDecimal("1"), matched.get(0).getQty());
        assertEquals(new BigDecimal("100"), matched.get(1).getOpenPrice());
        assertEquals(new BigDecimal("0.5"), matched.get(1).getQty());
        assertEquals(1, opens.size());
        assertEquals(new BigDecimal("1.5"), opens.get(0).getQty());
        assertEquals(2_000L, matched.get(1).getOpenTime().getTime());
        assertEquals(3_000L, matched.get(1).getCloseTime().getTime());
        assertEquals(0, closes.size());
    }

    @Test
    void shouldNotMatchABuyThatOccursAfterTheSell() {
        List<BinanceTradeInfo> opens = new ArrayList<>(Collections.singletonList(
                trade(2L, "100", "1", 2_000L)
        ));
        List<BinanceTradeInfo> closes = new ArrayList<>(Collections.singletonList(
                trade(1L, "110", "1", 1_000L)
        ));

        List<MatchedTradeInfo> matched = match(opens, closes);

        assertEquals(0, matched.size());
        assertEquals(new BigDecimal("1"), opens.get(0).getQty());
        assertEquals(new BigDecimal("1"), closes.get(0).getQty());
    }

    @Test
    void shouldUseTradeIdToOrderTradesWithTheSameTimestamp() {
        List<BinanceTradeInfo> opens = new ArrayList<>(Collections.singletonList(
                trade(1L, "100", "1", 1_000L)
        ));
        List<BinanceTradeInfo> closes = new ArrayList<>(Collections.singletonList(
                trade(2L, "110", "1", 1_000L)
        ));

        assertEquals(1, match(opens, closes).size());

        opens = new ArrayList<>(Collections.singletonList(trade(2L, "100", "1", 1_000L)));
        closes = new ArrayList<>(Collections.singletonList(trade(1L, "110", "1", 1_000L)));
        assertEquals(0, match(opens, closes).size());
    }

    private List<MatchedTradeInfo> match(List<BinanceTradeInfo> opens, List<BinanceTradeInfo> closes) {
        return TradeMatcherUtil.matchTradesFifo(
                true,
                "0.001",
                opens,
                closes,
                BinanceTradeInfo::getQty,
                BinanceTradeInfo::setQty,
                BinanceTradeInfo::getPrice,
                BinanceTradeInfo::getTime,
                BinanceTradeInfo::getId
        );
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
