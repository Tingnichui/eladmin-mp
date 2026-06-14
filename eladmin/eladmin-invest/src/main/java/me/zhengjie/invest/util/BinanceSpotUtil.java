package me.zhengjie.invest.util;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.net.URLEncodeUtil;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import me.zhengjie.invest.constants.BinanceEnum;
import me.zhengjie.invest.domain.BinanceAccountInfo;
import me.zhengjie.invest.domain.BinanceTradeInfo;
import me.zhengjie.invest.domain.InvestKlinesRecord;
import me.zhengjie.invest.domain.dto.BinanceOrderApiDto;
import me.zhengjie.utils.DingdingUtil;
import me.zhengjie.utils.RedisUtils;
import me.zhengjie.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class BinanceSpotUtil {

    private static final Logger log = LoggerFactory.getLogger(BinanceSpotUtil.class);

    @Value("${proxy.host}")
    private String proxyHost;
    @Value("${proxy.port}")
    private Integer proxyPort;
    @Value("${binance.spot.api_host}")
    private String apiHost;
    @Resource
    private DingdingUtil dingdingUtil;
    @Resource
    private RedisUtils redisUtils;

    public List<BinanceTradeInfo> getMyTrades(String symbol) {
        Map<String, Object> params = new HashMap<>();
        params.put("symbol", symbol);
//        params.put("startTime", startTime.getTime());
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
        HttpRequest request = HttpUtil.createGet(apiHost + "/api/v3/avgPrice?symbol=" + symbol);
        request.setProxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort)));
        return JSON.parseObject(request.execute().body()).getBigDecimal("price");
    }

    /**
     * @param symbol 交易对
     * @param interval 周期，单位分钟
     * @param startTime 开始时间，毫秒时间戳
     * @param endTime 结束时间，毫秒时间戳
     * @return 返回   开始时间 <= 开盘时间 的数据
     */
    public List<InvestKlinesRecord> getKlines(BinanceEnum.SYMBOL symbol, BinanceEnum.KLINES_INTERVAL interval, Long startTime, Long endTime) {
        Map<String, Object> params = new HashMap<>();
        params.put("symbol", symbol);
        params.put("interval", interval.getValue());
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
            record.setPeriod(interval.getPeriod());
            investKlinesRecordList.add(record);
        }

        return investKlinesRecordList;

    }

    public JSONObject account() {
        return JSON.parseObject(this.doRequest("/api/v3/account", new HashMap<>(), true, true));
    }

    public Long order(BinanceOrderApiDto apiDto) {
        Map<String, Object> map = apiDto.toMap();
        JSONObject resultJson = JSON.parseObject(this.doRequest("/api/v3/order", map, true, false));
        String orderIdStr = resultJson.getString("orderId");
        if (StringUtils.isBlank(orderIdStr)) {
            throw new RuntimeException("币安下单未获取到交易订单号");
        }
        return Long.parseLong(orderIdStr);
    }

    public Long order(BinanceOrderApiDto apiDto, int maxRetries) {
        for (int i = 0; i < maxRetries ; i++) {
            try {
                return this.order(apiDto);
            } catch (Exception e) {
                log.error("下单失败，第 {} 次尝试", i + 1, e);
            }
        }
        return null;
    }

    private String doRequest(String url, Map<String, Object> params, Boolean signFlag, Boolean getFlag) {
        log.info("入参：{}", JSON.toJSONString(params));

        // 过滤空值
        params = params.entrySet().stream()
                .filter(entry -> null != entry.getValue() && StringUtils.isNotBlank(entry.getValue().toString()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));

        // 是否需要加签
        Map<String, String> headerMap = new HashMap<>();
        if (signFlag) {
            BinanceAccountInfo binanceAccountInfo = BinanceAccountContextHolder.get();
            if (null == binanceAccountInfo) {
                throw new RuntimeException("币安账户信息为空");
            }
            final String apiKey = binanceAccountInfo.getApiKey();
            final String apiSecret = binanceAccountInfo.getApiSecret();
            if (StringUtils.isAnyBlank(apiKey, apiSecret)) {
                throw new RuntimeException(binanceAccountInfo.getIdCardName() + "-币安账户API配置为空");
            }
            headerMap.put("X-MBX-APIKEY", apiKey);

            // 增加时间戳
            params.put("timestamp", System.currentTimeMillis());

            // 加签
            String queryString = params.entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + URLEncodeUtil.encode(entry.getValue().toString()))
                    .collect(Collectors.joining("&"));
            String signature = new HMac(HmacAlgorithm.HmacSHA256, apiSecret.getBytes(StandardCharsets.UTF_8)).digestHex(queryString);
            params.put("signature", signature);
        }

        // 拼接完整参数
        String finalQuery = params.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + URLEncodeUtil.encode(entry.getValue().toString()))
                .collect(Collectors.joining("&"));
        String fullUrl = apiHost + url + "?" + finalQuery;

        // 发送请求
        HttpRequest request = getFlag ? HttpUtil.createGet(fullUrl) : HttpUtil.createPost(fullUrl);
        headerMap.forEach(request::header);
        request.setProxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort)));
        HttpResponse response = request.execute();
        String body = response.body();
        log.info("出参：{}", body);

        // 判断响应状态
        if (200 != response.getStatus()) {
            throw new RuntimeException("币安接口调用失败" + response);
        }

        return body;
    }


    public List<JSONObject> listUserOrderHistory(Long startTimestamp, Long endTimestamp) {
        Map<String, Object> params = new HashMap<>();
        if (null != startTimestamp) {
            params.put("startTimestamp", startTimestamp);
        }
        if (null != endTimestamp) {
            params.put("endTimestamp", endTimestamp);
        }
        String resStr = this.doRequest("/sapi/v1/c2c/orderMatch/listUserOrderHistory", params, true, true);
        JSONObject resJson = JSON.parseObject(resStr);
        if (!resJson.getString("code").equals("000000")) {
            throw new RuntimeException("币安接口调用失败" + resJson.getString("message"));
        }
        return resJson.getJSONArray("data").toJavaList(JSONObject.class);
    }

    public Object usdStats(boolean cacheFlag) {

        BinanceAccountInfo accountInfo = BinanceAccountContextHolder.get();
        return redisUtils.getOrLoad("BINANCE:USTSTATS:" + accountInfo.getUid(), -1, cacheFlag, () -> {

            Date now = new Date();
            DateTime startTime = DateUtil.parse("2024-10-01", DatePattern.NORM_DATE_PATTERN);

            List<JSONObject> allRecord = new ArrayList<>();
            while (startTime.isBefore(now)) {
                startTime = DateUtil.beginOfMonth(startTime);
                DateTime endTime = DateUtil.endOfMonth(startTime);
                List<JSONObject> jsonObjects = this.listUserOrderHistory(startTime.getTime(), endTime.getTime());
                allRecord.addAll(jsonObjects);

                startTime = DateUtil.offsetMonth(startTime, 1);
            }

            List<JSONObject> successRecord = allRecord.stream()
                    .filter(v -> v.getString("orderStatus").equals("COMPLETED"))
                    .collect(Collectors.toList());

            Map<String, Object> resMap = new HashMap<>();
            BigDecimal rmbAmount = successRecord
                    .stream().map(v -> v.getBigDecimal("totalPrice"))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            resMap.put("rmbAmount", rmbAmount);

            BigDecimal usdAmount = successRecord
                    .stream().map(v -> v.getBigDecimal("takerAmount"))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            resMap.put("usdAmount", usdAmount);

            resMap.put("rmbToUsdRate", rmbAmount.divide(usdAmount, 4, RoundingMode.HALF_UP));

            return resMap;
        });

    }
}
