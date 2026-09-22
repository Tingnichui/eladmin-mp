package me.zhengjie.invest.util;

import cn.hutool.http.Method;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import me.zhengjie.invest.constants.BinanceEnum;
import me.zhengjie.invest.domain.BinanceFuturesTradeInfo;
import me.zhengjie.invest.domain.dto.BinanceFundingRate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 币安U本位合约
 */
@Component
public class BinanceUsdFuturesUtil {

    @Value("${binance.usd_futures.api_host}")
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
        return JSON.parseArray(this.doRequest("/fapi/v1/fundingRate", params, false, true), BinanceFundingRate.class);

    }

    private String doRequest(String url, Map<String, Object> params, Boolean signFlag, Boolean getFlag) {
        return binanceHttpClient.request(apiHost, url, params, signFlag,
                getFlag ? Method.GET : Method.POST);
    }

    public List<BinanceFuturesTradeInfo> userTrades(BinanceEnum.SYMBOL symbol, Long startTime, Long endTime) {
        Map<String, Object> parmasMap = new HashMap<>();
        parmasMap.put("symbol", symbol);
        parmasMap.put("limit", "1000");
        if (null != startTime) {
            parmasMap.put("startTime", startTime);
        }
        if (null != endTime) {
            parmasMap.put("endTime", endTime);
        }
        return JSON.parseArray(this.doRequest("/fapi/v1/userTrades", parmasMap, true, true)).toJavaList(BinanceFuturesTradeInfo.class);
    }

    public List<BinanceFuturesTradeInfo> userTradesFromId(BinanceEnum.SYMBOL symbol, Long fromId, int limit) {
        return userTradesFromId(symbol.name(), fromId, limit);
    }

    public List<BinanceFuturesTradeInfo> userTradesFromId(String symbol, Long fromId, int limit) {
        Map<String, Object> params = new HashMap<>();
        params.put("symbol", symbol);
        params.put("fromId", fromId);
        params.put("limit", limit);
        return JSON.parseArray(this.doRequest("/fapi/v1/userTrades", params, true, true))
                .toJavaList(BinanceFuturesTradeInfo.class);
    }

    public JSONObject account() {
        return JSON.parseObject(this.doRequest("/fapi/v3/account", new HashMap<>(), true, true));
    }

    public List<JSONObject> positionRisk(String symbol) {
        Map<String, Object> params = new HashMap<>();
        params.put("symbol", symbol);
        return JSON.parseArray(this.doRequest("/fapi/v3/positionRisk", params, true, true))
                .stream().map(obj -> (JSONObject) obj).collect(Collectors.toList());
    }

    public JSONObject symbolConfig(String symbol) {
        Map<String, Object> params = new HashMap<>();
        params.put("symbol", symbol);
        JSONArray configs = JSON.parseArray(this.doRequest("/fapi/v1/symbolConfig", params, true, true));
        return configs.isEmpty() ? null : configs.getJSONObject(0);
    }

    public BigDecimal price(BinanceEnum.SYMBOL symbol) {
        Map<String, Object> parmasMap = new HashMap<>();
        parmasMap.put("symbol", symbol);
        return JSON.parseObject(this.doRequest("/fapi/v2/ticker/price", parmasMap, false, true)).getBigDecimal("price");
    }
}
