package me.zhengjie.invest.util;

import cn.hutool.http.Method;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import me.zhengjie.invest.constants.BinanceEnum;
import me.zhengjie.invest.domain.BinanceCoinFuturesTradeInfo;
import me.zhengjie.invest.domain.dto.BinanceFundingRate;
import me.zhengjie.invest.domain.dto.BinanceCoinFuturesOrderDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

/**
 * 币安U本位合约
 */
@Component
public class BinanceCoinFuturesUtil {

    @Value("${binance.coin_futures.api_host}")
    private String apiHost;
    @Resource
    private BinanceHttpClient binanceHttpClient;

    /**
     * @param symbol    交易对
     * @param startTime 开始时间，毫秒时间戳
     * @param endTime   结束时间，毫秒时间戳
     * @return 返回   开始时间 <= 开盘时间 的数据
     */

    public List<BinanceFundingRate> getFundingRate(BinanceEnum.SYMBOL symbol, Long startTime, Long endTime) {
        Map<String, Object> params = new HashMap<>();
        params.put("symbol", symbol);
        if (null != startTime) {
            params.put("startTime", startTime);
        }
        if (null != endTime) {
            params.put("endTime", endTime);
        }
        params.put("limit", 1000);
        return JSON.parseArray(this.doRequest("/dapi/v1/fundingRate", params, false, true), BinanceFundingRate.class);

    }

    public List<BinanceCoinFuturesTradeInfo> userTrades(BinanceEnum.SYMBOL symbol, Long startTime, Long endTime) {
        Map<String, Object> parmasMap = new HashMap<>();
        parmasMap.put("symbol", symbol);
        parmasMap.put("limit", "1000");
        if (null != startTime) {
            parmasMap.put("startTime", startTime);
        }
        if (null != endTime) {
            parmasMap.put("endTime", endTime);
        }
        return JSON.parseArray(this.doRequest("/dapi/v1/userTrades", parmasMap, true, true)).toJavaList(BinanceCoinFuturesTradeInfo.class);
    }

    public List<BinanceCoinFuturesTradeInfo> userTradesFromId(BinanceEnum.SYMBOL symbol, Long fromId, int limit) {
        return userTradesFromId(symbol.name(), fromId, limit);
    }

    public List<BinanceCoinFuturesTradeInfo> userTradesFromId(String symbol, Long fromId, int limit) {
        Map<String, Object> params = new HashMap<>();
        params.put("symbol", symbol);
        params.put("fromId", fromId);
        params.put("limit", limit);
        return JSON.parseArray(this.doRequest("/dapi/v1/userTrades", params, true, true))
                .toJavaList(BinanceCoinFuturesTradeInfo.class);
    }

    private String doRequest(String url, Map<String, Object> params, Boolean signFlag, Boolean getFlag) {
        return doRequest(url, params, signFlag, getFlag ? Method.GET : Method.POST);
    }

    private String doRequest(String url, Map<String, Object> params, Boolean signFlag, Method method) {
        return binanceHttpClient.request(apiHost, url, params, signFlag, method);
    }

    public boolean isHedgeMode() {
        JSONObject result = JSON.parseObject(
                this.doRequest("/dapi/v1/positionSide/dual", new HashMap<>(), true, Method.GET));
        return result.getBooleanValue("dualSidePosition");
    }

    public BinanceCoinFuturesOrderDto placeOrder(Map<String, Object> params) {
        return JSON.parseObject(this.doRequest("/dapi/v1/order", params, true, Method.POST),
                BinanceCoinFuturesOrderDto.class);
    }

    public List<BinanceCoinFuturesOrderDto> listOpenOrders(String symbol) {
        Map<String, Object> params = new HashMap<>();
        params.put("symbol", symbol);
        return JSON.parseArray(this.doRequest("/dapi/v1/openOrders", params, true, Method.GET))
                .toJavaList(BinanceCoinFuturesOrderDto.class);
    }

    public BinanceCoinFuturesOrderDto queryOrder(String symbol, Long orderId) {
        Map<String, Object> params = new HashMap<>();
        params.put("symbol", symbol);
        params.put("orderId", orderId);
        return JSON.parseObject(this.doRequest("/dapi/v1/order", params, true, Method.GET),
                BinanceCoinFuturesOrderDto.class);
    }

    public BinanceCoinFuturesOrderDto queryOrder(String symbol, String clientOrderId) {
        Map<String, Object> params = new HashMap<>();
        params.put("symbol", symbol);
        params.put("origClientOrderId", clientOrderId);
        return JSON.parseObject(this.doRequest("/dapi/v1/order", params, true, Method.GET),
                BinanceCoinFuturesOrderDto.class);
    }

    public BinanceCoinFuturesOrderDto cancelOrder(String symbol, Long orderId) {
        Map<String, Object> params = new HashMap<>();
        params.put("symbol", symbol);
        params.put("orderId", orderId);
        return JSON.parseObject(this.doRequest("/dapi/v1/order", params, true, Method.DELETE),
                BinanceCoinFuturesOrderDto.class);
    }

    public BigDecimal price(BinanceEnum.SYMBOL symbol) {
        Map<String, Object> parmasMap = new HashMap<>();
        parmasMap.put("symbol", symbol);
        return JSON.parseArray(this.doRequest("/dapi/v1/ticker/price", parmasMap, false, true)).getJSONObject(0).getBigDecimal("price");
    }

    public List<JSONObject> positionRisk(String symbol) {
        Map<String, Object> params = new HashMap<>();
        params.put("symbol", symbol);
        return JSON.parseArray(this.doRequest("/dapi/v1/positionRisk", params, true, true), JSONObject.class);
    }

    public JSONObject account() {
        return JSON.parseObject(this.doRequest("/dapi/v1/account", new HashMap<>(), true, true));
    }

    public JSONObject contractInfo(String symbol) {
        JSONObject exchangeInfo = JSON.parseObject(
                this.doRequest("/dapi/v1/exchangeInfo", new HashMap<>(), false, true));
        JSONArray symbols = exchangeInfo.getJSONArray("symbols");
        if (symbols == null) {
            return null;
        }
        return symbols.stream()
                .map(JSONObject.class::cast)
                .filter(item -> symbol.equalsIgnoreCase(item.getString("symbol")))
                .findFirst().orElse(null);
    }

    public JSONObject premiumIndex(String symbol) {
        Map<String, Object> params = new HashMap<>();
        params.put("symbol", symbol);
        String body = this.doRequest("/dapi/v1/premiumIndex", params, false, true);
        if (body != null && body.trim().startsWith("[")) {
            JSONArray values = JSON.parseArray(body);
            return values.isEmpty() ? null : values.getJSONObject(0);
        }
        return JSON.parseObject(body);
    }

    public List<JSONObject> listIncome(BinanceEnum.SYMBOL symbol, Long startTime, String incomeType) {
        return listIncome(symbol.name(), startTime, incomeType);
    }

    public List<JSONObject> listIncome(String symbol, Long startTime, String incomeType) {
        Map<String, Object> params = new HashMap<>();
        params.put("symbol", symbol);
        params.put("incomeType", incomeType);
        if (null != startTime) {
            params.put("startTime", startTime);
        }
        params.put("limit", 1000);
        return JSON.parseArray(this.doRequest("/dapi/v1/income", params, true, true), JSONObject.class);
    }


}
