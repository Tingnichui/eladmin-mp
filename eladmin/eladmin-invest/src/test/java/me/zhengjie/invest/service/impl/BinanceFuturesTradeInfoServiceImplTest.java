package me.zhengjie.invest.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import me.zhengjie.invest.domain.BinanceFuturesTradeInfo;
import me.zhengjie.invest.domain.dto.BinanceUsdFuturesStatsInfoVO;
import me.zhengjie.invest.mapper.BinanceFuturesTradeInfoMapper;
import me.zhengjie.invest.util.BinanceUsdFuturesUtil;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BinanceFuturesTradeInfoServiceImplTest {

    @Test
    @SuppressWarnings("unchecked")
    void shouldCombineRealtimeShortPositionAndLocalTrades() {
        BinanceUsdFuturesUtil futuresUtil = mock(BinanceUsdFuturesUtil.class);
        BinanceFuturesTradeInfoMapper mapper = mock(BinanceFuturesTradeInfoMapper.class);
        BinanceFuturesTradeInfoServiceImpl service = new BinanceFuturesTradeInfoServiceImpl();
        ReflectionTestUtils.setField(service, "binanceUsdFuturesUtil", futuresUtil);
        ReflectionTestUtils.setField(service, "binanceFuturesTradeInfoMapper", mapper);

        JSONObject position = new JSONObject();
        position.put("symbol", "BTCUSDT");
        position.put("positionSide", "SHORT");
        position.put("positionAmt", "-0.6");
        position.put("entryPrice", "100000");
        position.put("breakEvenPrice", "99950");
        position.put("markPrice", "90000");
        position.put("unRealizedProfit", "6000");
        JSONObject symbolConfig = new JSONObject();
        symbolConfig.put("leverage", 5);
        symbolConfig.put("marginType", "CROSSED");
        symbolConfig.put("isAutoAddMargin", false);
        symbolConfig.put("maxNotionalValue", "200000");
        JSONObject account = new JSONObject();
        account.put("totalWalletBalance", "10000");
        account.put("totalMarginBalance", "16000");
        account.put("availableBalance", "8000");
        when(futuresUtil.positionRisk("BTCUSDT")).thenReturn(Collections.singletonList(position));
        when(futuresUtil.symbolConfig("BTCUSDT")).thenReturn(symbolConfig);
        when(futuresUtil.account()).thenReturn(account);
        when(mapper.selectList(any(Wrapper.class))).thenReturn(Arrays.asList(
                trade(1L, "SELL", "1.0", "100000", "0", "40"),
                trade(2L, "BUY", "0.4", "90000", "4000", "15")
        ));

        BinanceUsdFuturesStatsInfoVO result = service.queryStats(7, "BTCUSDT", "SHORT");

        assertEquals(new BigDecimal("-0.6"), result.getPositionInfo().getPositionAmt());
        assertEquals(Integer.valueOf(5), result.getPositionInfo().getLeverage());
        assertEquals("CROSSED", result.getPositionInfo().getMarginType());
        assertEquals(new BigDecimal("4000"), result.getTradeSummary().getRealizedPnl());
        assertEquals(new BigDecimal("55"), result.getTradeSummary().getCommission());
        assertEquals(new BigDecimal("3945"), result.getTradeSummary().getNetPnl());
        assertEquals(1, result.getTradeList().size());
        assertEquals(new BigDecimal("0.6"), result.getTradeList().get(0).getQty());
        assertEquals(new BigDecimal("90000"), result.getTradeList().get(0).getClosePrice());
    }

    private BinanceFuturesTradeInfo trade(Long id, String side, String qty, String price,
                                           String realizedPnl, String commission) {
        BinanceFuturesTradeInfo trade = new BinanceFuturesTradeInfo();
        trade.setId(id);
        trade.setOrderId(id + 100L);
        trade.setSide(side);
        trade.setPositionSide("SHORT");
        trade.setQty(new BigDecimal(qty));
        trade.setPrice(new BigDecimal(price));
        trade.setRealizedPnl(new BigDecimal(realizedPnl));
        trade.setCommission(new BigDecimal(commission));
        trade.setTime(new Timestamp(id * 1000L));
        return trade;
    }
}
