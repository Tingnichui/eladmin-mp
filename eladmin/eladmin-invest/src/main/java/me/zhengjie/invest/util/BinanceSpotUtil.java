package me.zhengjie.invest.util;

import cn.hutool.http.Method;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import me.zhengjie.invest.constants.BinanceEnum;
import me.zhengjie.invest.domain.BinanceTradeInfo;
import me.zhengjie.invest.domain.InvestKlinesRecord;
import me.zhengjie.invest.domain.dto.BinanceOrderApiDto;
import me.zhengjie.invest.domain.dto.BinanceSpotOpenOrderDto;
import me.zhengjie.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;

@Component
public class BinanceSpotUtil {

    private static final Logger log = LoggerFactory.getLogger(BinanceSpotUtil.class);

    @Value("${binance.spot.api_host}")
    private String apiHost;
    @Resource
    private BinanceHttpClient binanceHttpClient;
    public List<BinanceTradeInfo> getMyTrades(String symbol) {
        return getMyTrades(symbol, null, 1000);
    }

    public List<BinanceTradeInfo> getMyTrades(String symbol, Long fromId, int limit) {
        Map<String, Object> params = new HashMap<>();
        params.put("symbol", symbol);
        params.put("fromId", fromId);
        params.put("limit", limit);
        return JSON.parseArray(this.doRequest("/api/v3/myTrades", params, true, true))
                .toJavaList(BinanceTradeInfo.class);
    }

    public BigDecimal getPrice(BinanceEnum.SYMBOL symbol) {
        Map<String, Object> params = new HashMap<>();
        params.put("symbol", symbol);

        String resStr = this.doRequest("/api/v3/ticker/price", params, false, true);
        JSONObject resJson = JSON.parseObject(resStr);
        return resJson.getBigDecimal("price");
    }

    public BigDecimal getAvgPrice(BinanceEnum.SYMBOL symbol) {
        Map<String, Object> params = new HashMap<>();
        params.put("symbol", symbol);
        return JSON.parseObject(this.doRequest("/api/v3/avgPrice", params, false, true))
                .getBigDecimal("price");
    }

    /**
     * @param symbol 交易对
     * @param intervalCode 周期，单位分钟
     * @param startTime 开始时间，毫秒时间戳
     * @param endTime 结束时间，毫秒时间戳
     * @return 返回   开始时间 <= 开盘时间 的数据
     */
    public List<InvestKlinesRecord> getKlines(BinanceEnum.SYMBOL symbol, String intervalCode, Long startTime, Long endTime) {
        Map<String, Object> params = new HashMap<>();
        params.put("symbol", symbol);
        params.put("interval", intervalCode);
        if (null != startTime) {
            params.put("startTime", startTime);
        }
        if (null != endTime) {
            params.put("endTime", endTime);
        }
        params.put("limit", 1000);
        List<List> rawKlinesList = JSON.parseArray(this.doRequest("/api/v3/klines", params, false, true)).toJavaList(List.class);

        List<InvestKlinesRecord> investKlinesRecordList = new ArrayList<>();
        for (List item : rawKlinesList) {

            InvestKlinesRecord record = new InvestKlinesRecord();
            record.setSymbol(symbol.toString());
            record.setOpenTime((Long) item.get(0));// 开盘时间
            record.setOpenPrice(new BigDecimal((String) item.get(1)));// 开盘价
            record.setHighPrice(new BigDecimal((String) item.get(2)));// 最高价
            record.setLowPrice(new BigDecimal((String) item.get(3)));// 最低价
            record.setClosePrice(new BigDecimal((String) item.get(4)));// 收盘价(当前K线未结束的即为最新价)
            record.setVolume(new BigDecimal((String) item.get(5)));// 成交量
            record.setCloseTime((Long) item.get(6));// 收盘时间
            record.setTurnover(new BigDecimal((String) item.get(7)));// 成交额
            record.setTradeCount((Integer) item.get(8));// 成交笔数
            record.setBuyVolume(new BigDecimal((String) item.get(9)));// 主动买入成交量
            record.setBuyTurnover(new BigDecimal((String) item.get(10)));// 主动买入成交额
            record.setIntervalCode(intervalCode);
            investKlinesRecordList.add(record);
        }

        return investKlinesRecordList;

    }

    public JSONObject account() {
        return JSON.parseObject(this.doRequest("/api/v3/account", new HashMap<>(), true, true));
    }

    public Long order(BinanceOrderApiDto apiDto) {
        if (StringUtils.isBlank(apiDto.getNewClientOrderId())) {
            apiDto.setNewClientOrderId("el_" + UUID.randomUUID().toString().replace("-", ""));
        }
        Map<String, Object> map = apiDto.toMap();
        try {
            JSONObject resultJson = JSON.parseObject(this.doRequest("/api/v3/order", map, true, false));
            String orderIdStr = resultJson.getString("orderId");
            if (StringUtils.isBlank(orderIdStr)) {
                throw new RuntimeException("币安下单未获取到交易订单号");
            }
            return Long.parseLong(orderIdStr);
        } catch (RuntimeException placeError) {
            try {
                BinanceSpotOpenOrderDto reconciled = queryOrder(apiDto.getSymbol(), apiDto.getNewClientOrderId());
                if (reconciled != null && reconciled.getOrderId() != null) {
                    return reconciled.getOrderId();
                }
            } catch (RuntimeException queryError) {
                placeError.addSuppressed(queryError);
            }
            throw placeError;
        }
    }

    public List<BinanceSpotOpenOrderDto> listOpenOrders(String symbol) {
        Map<String, Object> params = new HashMap<>();
        params.put("symbol", symbol);
        return JSON.parseArray(this.doRequest("/api/v3/openOrders", params, true, true))
                .toJavaList(BinanceSpotOpenOrderDto.class);
    }

    public BinanceSpotOpenOrderDto queryOrder(String symbol, Long orderId) {
        Map<String, Object> params = new HashMap<>();
        params.put("symbol", symbol);
        params.put("orderId", orderId);
        return JSON.parseObject(this.doRequest("/api/v3/order", params, true, true),
                BinanceSpotOpenOrderDto.class);
    }

    public BinanceSpotOpenOrderDto queryOrder(String symbol, String clientOrderId) {
        Map<String, Object> params = new HashMap<>();
        params.put("symbol", symbol);
        params.put("origClientOrderId", clientOrderId);
        return JSON.parseObject(this.doRequest("/api/v3/order", params, true, true),
                BinanceSpotOpenOrderDto.class);
    }

    public Long cancelOrder(String symbol, Long orderId) {
        Map<String, Object> params = new HashMap<>();
        params.put("symbol", symbol);
        params.put("orderId", orderId);
        JSONObject resultJson = JSON.parseObject(
                this.doRequest("/api/v3/order", params, true, Method.DELETE));
        String canceledOrderId = resultJson.getString("orderId");
        if (StringUtils.isBlank(canceledOrderId)) {
            throw new RuntimeException("币安撤单未获取到交易订单号");
        }
        return Long.parseLong(canceledOrderId);
    }

    public Long order(BinanceOrderApiDto apiDto, int maxRetries) {
        try {
            return this.order(apiDto);
        } catch (Exception e) {
            log.error("下单失败，已按客户端订单号查单确认，不再盲目重试", e);
            return null;
        }
    }

    private String doRequest(String url, Map<String, Object> params, Boolean signFlag, Boolean getFlag) {
        return doRequest(url, params, signFlag, getFlag ? Method.GET : Method.POST);
    }

    private String doRequest(String url, Map<String, Object> params, Boolean signFlag, Method method) {
        return binanceHttpClient.request(apiHost, url, params, signFlag, method);
    }


    public List<JSONObject> listUserOrderHistory(Long startTimestamp, Long endTimestamp) {
        return listUserOrderHistory(startTimestamp, endTimestamp, null, null);
    }

    public List<JSONObject> listUserOrderHistory(Long startTimestamp, Long endTimestamp,
                                                 Integer page, Integer rows) {
        Map<String, Object> params = new HashMap<>();
        if (null != startTimestamp) {
            params.put("startTimestamp", startTimestamp);
        }
        if (null != endTimestamp) {
            params.put("endTimestamp", endTimestamp);
        }
        if (null != page) {
            params.put("page", page);
        }
        if (null != rows) {
            params.put("rows", rows);
        }
        String resStr = this.doRequest("/sapi/v1/c2c/orderMatch/listUserOrderHistory", params, true, true);
        JSONObject resJson = JSON.parseObject(resStr);
        if (!resJson.getString("code").equals("000000")) {
            throw new RuntimeException("币安接口调用失败" + resJson.getString("message"));
        }
        return resJson.getJSONArray("data").toJavaList(JSONObject.class);
    }

}
