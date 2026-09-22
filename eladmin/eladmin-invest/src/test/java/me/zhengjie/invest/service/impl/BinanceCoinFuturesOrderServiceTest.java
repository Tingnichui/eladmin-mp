package me.zhengjie.invest.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import me.zhengjie.exception.BadRequestException;
import me.zhengjie.invest.domain.dto.BinanceCoinFuturesOrderDto;
import me.zhengjie.invest.domain.dto.BinanceCoinFuturesOrderRequest;
import me.zhengjie.invest.util.BinanceCoinFuturesUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BinanceCoinFuturesOrderServiceTest {

    private BinanceCoinFuturesUtil futuresUtil;
    private BinanceCoinFuturesTradeInfoServiceImpl service;

    @BeforeEach
    void setUp() {
        futuresUtil = mock(BinanceCoinFuturesUtil.class);
        service = new BinanceCoinFuturesTradeInfoServiceImpl();
        ReflectionTestUtils.setField(service, "binanceCoinFuturesUtil", futuresUtil);
        when(futuresUtil.contractInfo("BTCUSD_PERP")).thenReturn(contract());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldCloseHedgeShortWithBuyAndWithoutReduceOnly() {
        when(futuresUtil.isHedgeMode()).thenReturn(true);
        when(futuresUtil.positionRisk("BTCUSD_PERP"))
                .thenReturn(Collections.singletonList(position("SHORT", "-3")));
        BinanceCoinFuturesOrderDto response = new BinanceCoinFuturesOrderDto();
        response.setOrderId(101L);
        when(futuresUtil.placeOrder(any(Map.class))).thenReturn(response);

        BinanceCoinFuturesOrderDto result = service.placeOrder(request("CLOSE", "SHORT", "MARKET", "2", null));

        assertEquals(Long.valueOf(101L), result.getOrderId());
        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        verify(futuresUtil).placeOrder(params.capture());
        assertEquals("BUY", params.getValue().get("side"));
        assertEquals("SHORT", params.getValue().get("positionSide"));
        assertEquals("2", params.getValue().get("quantity"));
        assertFalse(params.getValue().containsKey("reduceOnly"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldCloseOneWayLongWithReduceOnly() {
        when(futuresUtil.isHedgeMode()).thenReturn(false);
        when(futuresUtil.positionRisk("BTCUSD_PERP"))
                .thenReturn(Collections.singletonList(position("BOTH", "4")));
        when(futuresUtil.placeOrder(any(Map.class))).thenReturn(new BinanceCoinFuturesOrderDto());

        service.placeOrder(request("CLOSE", "LONG", "LIMIT", "1", "90000"));

        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        verify(futuresUtil).placeOrder(params.capture());
        assertEquals("SELL", params.getValue().get("side"));
        assertEquals("BOTH", params.getValue().get("positionSide"));
        assertEquals("true", params.getValue().get("reduceOnly"));
        assertEquals("GTC", params.getValue().get("timeInForce"));
    }

    @Test
    void shouldRejectCloseQuantityLargerThanLivePosition() {
        when(futuresUtil.isHedgeMode()).thenReturn(true);
        when(futuresUtil.positionRisk("BTCUSD_PERP"))
                .thenReturn(Collections.singletonList(position("LONG", "1")));

        BadRequestException error = assertThrows(BadRequestException.class,
                () -> service.placeOrder(request("CLOSE", "LONG", "MARKET", "2", null)));

        assertTrue(error.getMessage().contains("不能超过当前持仓"));
        verify(futuresUtil, never()).placeOrder(any());
    }

    @Test
    void shouldReconcileByClientOrderIdWhenPostResultIsUncertain() {
        when(futuresUtil.isHedgeMode()).thenReturn(true);
        when(futuresUtil.placeOrder(any())).thenThrow(new RuntimeException("timeout"));
        BinanceCoinFuturesOrderDto reconciled = new BinanceCoinFuturesOrderDto();
        reconciled.setOrderId(202L);
        when(futuresUtil.queryOrder(anyString(), anyString())).thenReturn(reconciled);

        BinanceCoinFuturesOrderDto result = service.placeOrder(request("OPEN", "LONG", "MARKET", "1", null));

        assertEquals(Long.valueOf(202L), result.getOrderId());
        verify(futuresUtil).queryOrder(anyString(), anyString());
    }

    private BinanceCoinFuturesOrderRequest request(String action, String side, String type,
                                                    String quantity, String price) {
        BinanceCoinFuturesOrderRequest request = new BinanceCoinFuturesOrderRequest();
        request.setUid(7);
        request.setSymbol("BTCUSD_PERP");
        request.setAction(action);
        request.setPositionSide(side);
        request.setType(type);
        request.setQuantity(new BigDecimal(quantity));
        request.setPrice(price == null ? null : new BigDecimal(price));
        return request;
    }

    private JSONObject position(String side, String amount) {
        JSONObject position = new JSONObject();
        position.put("positionSide", side);
        position.put("positionAmt", amount);
        return position;
    }

    private JSONObject contract() {
        JSONObject lotSize = new JSONObject();
        lotSize.put("filterType", "LOT_SIZE");
        lotSize.put("minQty", "1");
        lotSize.put("maxQty", "1000");
        lotSize.put("stepSize", "1");
        JSONObject marketLotSize = new JSONObject(lotSize);
        marketLotSize.put("filterType", "MARKET_LOT_SIZE");
        JSONObject priceFilter = new JSONObject();
        priceFilter.put("filterType", "PRICE_FILTER");
        priceFilter.put("minPrice", "0.1");
        priceFilter.put("maxPrice", "1000000");
        priceFilter.put("tickSize", "0.1");
        JSONObject contract = new JSONObject();
        contract.put("contractStatus", "TRADING");
        contract.put("filters", new JSONArray(java.util.Arrays.asList(lotSize, marketLotSize, priceFilter)));
        return contract;
    }
}
