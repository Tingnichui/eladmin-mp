package me.zhengjie.invest.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import me.zhengjie.invest.domain.BinanceCoinFuturesTradeInfo;
import me.zhengjie.invest.domain.dto.BinanceCoinFuturesContractInfo;
import me.zhengjie.invest.domain.dto.BinanceCoinFuturesPositionInfo;
import me.zhengjie.invest.domain.dto.BinanceCoinFuturesPositionStatsVO;
import me.zhengjie.invest.domain.dto.BinanceCoinFuturesStatsInfoVO;
import me.zhengjie.invest.mapper.BinanceCoinFuturesTradeInfoMapper;
import me.zhengjie.invest.util.BinanceCoinFuturesUtil;
import me.zhengjie.utils.RedisUtils;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BinanceCoinFuturesStatsServiceTest {

    @Test
    @SuppressWarnings("unchecked")
    void shouldUseCachedPositionAndDisableOrdersWhenRealtimeRequestFails() {
        BinanceCoinFuturesUtil futuresUtil = mock(BinanceCoinFuturesUtil.class);
        BinanceCoinFuturesTradeInfoMapper mapper = mock(BinanceCoinFuturesTradeInfoMapper.class);
        RedisUtils redisUtils = mock(RedisUtils.class);
        BinanceCoinFuturesTradeInfoServiceImpl service = new BinanceCoinFuturesTradeInfoServiceImpl();
        ReflectionTestUtils.setField(service, "binanceCoinFuturesUtil", futuresUtil);
        ReflectionTestUtils.setField(service, "binanceCoinFuturesTradeInfoMapper", mapper);
        ReflectionTestUtils.setField(service, "redisUtils", redisUtils);

        BinanceCoinFuturesContractInfo contract = new BinanceCoinFuturesContractInfo();
        contract.setSymbol("BTCUSD_PERP");
        contract.setContractSize(new BigDecimal("100"));
        contract.setMarginAsset("BTC");
        BinanceCoinFuturesPositionInfo cachedPosition = new BinanceCoinFuturesPositionInfo();
        cachedPosition.setSymbol("BTCUSD_PERP");
        cachedPosition.setPositionSide("SHORT");
        cachedPosition.setPositionAmt(new BigDecimal("-2"));
        cachedPosition.setMarkPrice(new BigDecimal("90000"));
        JSONObject premium = new JSONObject();
        premium.put("markPrice", "90100");

        when(redisUtils.get("BINANCE:COIN_FUTURES:CONTRACT:BTCUSD_PERP",
                BinanceCoinFuturesContractInfo.class)).thenReturn(contract);
        when(redisUtils.get("BINANCE:COIN_FUTURES:POSITION:7:BTCUSD_PERP:SHORT",
                BinanceCoinFuturesPositionInfo.class)).thenReturn(cachedPosition);
        when(futuresUtil.positionRisk("BTCUSD_PERP")).thenThrow(new RuntimeException("unavailable"));
        when(futuresUtil.premiumIndex("BTCUSD_PERP")).thenReturn(premium);
        when(mapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());

        BinanceCoinFuturesPositionStatsVO result = service.queryPositionStats(
                7, "BTCUSD_PERP", "SHORT");

        assertFalse(result.isRealtime());
        assertEquals(0, result.getPositionInfo().getPositionAmt().compareTo(new BigDecimal("-2")));
        assertTrue(result.getWarnings().stream().anyMatch(item -> item.contains("缓存数据")));
        verify(futuresUtil, never()).contractInfo("BTCUSD_PERP");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldLoadPositionStatsWithoutAccountOrClosedSummaryRequests() {
        BinanceCoinFuturesUtil futuresUtil = mock(BinanceCoinFuturesUtil.class);
        BinanceCoinFuturesTradeInfoMapper mapper = mock(BinanceCoinFuturesTradeInfoMapper.class);
        BinanceCoinFuturesTradeInfoServiceImpl service = new BinanceCoinFuturesTradeInfoServiceImpl();
        ReflectionTestUtils.setField(service, "binanceCoinFuturesUtil", futuresUtil);
        ReflectionTestUtils.setField(service, "binanceCoinFuturesTradeInfoMapper", mapper);

        JSONObject contract = new JSONObject();
        contract.put("symbol", "BTCUSD_PERP");
        contract.put("contractSize", "100");
        contract.put("marginAsset", "BTC");
        JSONObject position = new JSONObject();
        position.put("symbol", "BTCUSD_PERP");
        position.put("positionSide", "SHORT");
        position.put("positionAmt", "-2");
        position.put("markPrice", "90000");

        when(futuresUtil.contractInfo("BTCUSD_PERP")).thenReturn(contract);
        when(futuresUtil.positionRisk("BTCUSD_PERP")).thenReturn(Collections.singletonList(position));
        when(mapper.selectList(any(Wrapper.class))).thenReturn(Collections.singletonList(
                trade(1L, "SELL", "2", "0.002", "100000", "0", "0.00001")));

        assertEquals(1, service.queryPositionStats(7, "BTCUSD_PERP", "SHORT").getTradeList().size());
        verify(futuresUtil, never()).account();
        verify(futuresUtil, never()).listIncome(
                anyString(), anyLong(), anyLong(), anyString(), anyInt(), anyInt());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldCombineRealtimeShortPositionAndInverseContractTrades() {
        BinanceCoinFuturesUtil futuresUtil = mock(BinanceCoinFuturesUtil.class);
        BinanceCoinFuturesTradeInfoMapper mapper = mock(BinanceCoinFuturesTradeInfoMapper.class);
        BinanceCoinFuturesTradeInfoServiceImpl service = new BinanceCoinFuturesTradeInfoServiceImpl();
        ReflectionTestUtils.setField(service, "binanceCoinFuturesUtil", futuresUtil);
        ReflectionTestUtils.setField(service, "binanceCoinFuturesTradeInfoMapper", mapper);

        JSONObject contract = new JSONObject();
        contract.put("symbol", "BTCUSD_PERP");
        contract.put("pair", "BTCUSD");
        contract.put("contractType", "PERPETUAL");
        contract.put("contractSize", "100");
        contract.put("baseAsset", "BTC");
        contract.put("quoteAsset", "USD");
        contract.put("marginAsset", "BTC");
        JSONObject position = new JSONObject();
        position.put("symbol", "BTCUSD_PERP");
        position.put("positionSide", "SHORT");
        position.put("positionAmt", "-2");
        position.put("entryPrice", "100000");
        position.put("markPrice", "90000");
        position.put("unRealizedProfit", "0.00022");
        position.put("leverage", 5);
        JSONObject asset = new JSONObject();
        asset.put("asset", "BTC");
        asset.put("walletBalance", "0.5");
        asset.put("availableBalance", "0.4");
        JSONObject account = new JSONObject();
        account.put("assets", new JSONArray(Collections.singletonList(asset)));

        when(futuresUtil.contractInfo("BTCUSD_PERP")).thenReturn(contract);
        when(futuresUtil.positionRisk("BTCUSD_PERP")).thenReturn(Collections.singletonList(position));
        when(futuresUtil.account()).thenReturn(account);
        when(futuresUtil.listIncome("BTCUSD_PERP", 1000L, 4000L,
                "FUNDING_FEE", 1, 1000))
                .thenReturn(Collections.singletonList(income("0.0001")));
        when(mapper.selectList(any(Wrapper.class))).thenReturn(Arrays.asList(
                trade(1L, "SELL", "2", "0.002", "100000", "0", "0.00001"),
                trade(2L, "BUY", "2", "0.00222222", "90000", "0.002", "0.00002"),
                trade(3L, "SELL", "1", "0.00105263", "95000", "0", "0.00001"),
                trade(4L, "BUY", "1", "0.00117647", "85000", "0.001", "0.00001"),
                trade(5L, "SELL", "3", "0.003", "100000", "0", "0.00001"),
                trade(6L, "BUY", "1", "0.00111111", "90000", "0.001", "0.00001")
        ));

        BinanceCoinFuturesStatsInfoVO result = service.queryStats(7, "BTCUSD_PERP", "SHORT");

        assertEquals(new BigDecimal("-2"), result.getPositionInfo().getPositionAmt());
        assertEquals(new BigDecimal("100"), result.getContractInfo().getContractSize());
        assertEquals(new BigDecimal("0.5"), result.getAccountInfo().getWalletBalance());
        assertEquals(2, result.getTradeSummary().getClosedPositionCount());
        assertEquals(4, result.getTradeSummary().getTotalTradeCount());
        assertEquals(0, result.getTradeSummary().getNetPnl().compareTo(new BigDecimal("0.00305")));
        assertEquals(1, result.getTradeList().size());
        assertEquals(0, result.getTradeList().get(0).getContractQty().compareTo(new BigDecimal("2")));
        assertEquals(0, result.getTradeList().get(0).getBaseQty().compareTo(new BigDecimal("0.002")));
        assertTrue(result.getTradeList().get(0).getUnrealizedPnl().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldLoadPremiumIndexWhenEmptyPositionHasZeroMarkPrice() {
        BinanceCoinFuturesUtil futuresUtil = mock(BinanceCoinFuturesUtil.class);
        BinanceCoinFuturesTradeInfoMapper mapper = mock(BinanceCoinFuturesTradeInfoMapper.class);
        BinanceCoinFuturesTradeInfoServiceImpl service = new BinanceCoinFuturesTradeInfoServiceImpl();
        ReflectionTestUtils.setField(service, "binanceCoinFuturesUtil", futuresUtil);
        ReflectionTestUtils.setField(service, "binanceCoinFuturesTradeInfoMapper", mapper);

        JSONObject contract = new JSONObject();
        contract.put("symbol", "BTCUSD_PERP");
        contract.put("contractSize", "100");
        contract.put("marginAsset", "BTC");
        JSONObject position = new JSONObject();
        position.put("symbol", "BTCUSD_PERP");
        position.put("positionSide", "SHORT");
        position.put("positionAmt", "0");
        position.put("markPrice", "0.00000000");
        JSONObject premium = new JSONObject();
        premium.put("markPrice", "85493.2");

        when(futuresUtil.contractInfo("BTCUSD_PERP")).thenReturn(contract);
        when(futuresUtil.positionRisk("BTCUSD_PERP")).thenReturn(Collections.singletonList(position));
        when(futuresUtil.premiumIndex("BTCUSD_PERP")).thenReturn(premium);
        when(futuresUtil.account()).thenReturn(new JSONObject());
        when(mapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());

        BinanceCoinFuturesStatsInfoVO result = service.queryStats(7, "BTCUSD_PERP", "SHORT");

        assertEquals(0, result.getPositionInfo().getMarkPrice().compareTo(new BigDecimal("85493.2")));
        verify(futuresUtil).premiumIndex("BTCUSD_PERP");
    }

    private JSONObject income(String value) {
        JSONObject income = new JSONObject();
        income.put("income", value);
        return income;
    }

    private BinanceCoinFuturesTradeInfo trade(Long id, String side, String qty, String baseQty,
                                                String price, String realizedPnl, String commission) {
        BinanceCoinFuturesTradeInfo trade = new BinanceCoinFuturesTradeInfo();
        trade.setId(id);
        trade.setOrderId(id + 100L);
        trade.setSide(side);
        trade.setPositionSide("SHORT");
        trade.setQty(new BigDecimal(qty));
        trade.setBaseQty(new BigDecimal(baseQty));
        trade.setPrice(new BigDecimal(price));
        trade.setRealizedPnl(new BigDecimal(realizedPnl));
        trade.setCommission(new BigDecimal(commission));
        trade.setCommissionAsset("BTC");
        trade.setTime(new Timestamp(id * 1000L));
        return trade;
    }
}
