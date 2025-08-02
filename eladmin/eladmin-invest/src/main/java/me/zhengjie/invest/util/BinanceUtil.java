package me.zhengjie.invest.util;

import cn.hutool.core.net.URLEncodeUtil;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import me.zhengjie.invest.constants.BinanceEnum;
import me.zhengjie.invest.domain.BinanceAccountInfo;
import me.zhengjie.invest.domain.BinanceTradeInfo;
import me.zhengjie.invest.domain.InvestKlinesRecord;
import me.zhengjie.invest.domain.dto.BinanceOrderApiDto;
import me.zhengjie.utils.DingdingUtil;
import me.zhengjie.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class BinanceUtil {

    private static final Logger log = LoggerFactory.getLogger(BinanceUtil.class);

    @Value("${proxy.host}")
    private String proxyHost;
    @Value("${proxy.port}")
    private Integer proxyPort;
    @Value("${binance.api_host}")
    private String apiHost;
    @Resource
    private DingdingUtil dingdingUtil;

    public List<BinanceTradeInfo> getMyTrades(String symbol) {
        Map<String, Object> params = new HashMap<>();
        params.put("symbol", symbol);
//        params.put("startTime", startTime.getTime());
        return JSON.parseArray(this.doRequest("/api/v3/myTrades", params, true, true))
                .toJavaList(BinanceTradeInfo.class);
    }

    public BigDecimal getPrice(BinanceEnum.SYMBOL symbol) {
        HttpRequest request = HttpUtil.createGet(apiHost + "/api/v3/ticker/price?symbol=" + symbol);
        request.setProxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort)));
        return JSON.parseObject(request.execute().body()).getBigDecimal("price");
    }

    public BigDecimal getAvgPrice(BinanceEnum.SYMBOL symbol) {
        HttpRequest request = HttpUtil.createGet(apiHost + "/api/v3/avgPrice?symbol=" + symbol);
        request.setProxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort)));
        return JSON.parseObject(request.execute().body()).getBigDecimal("price");
    }

    public List<InvestKlinesRecord> getKlines(BinanceEnum.SYMBOL symbol, BinanceEnum.KLINES_INTERVAL interval, Date startTime, Date endTime) {
        Map<String, Object> params = new HashMap<>();
        params.put("symbol", symbol);
        params.put("interval", interval.getValue());
        params.put("startTime", startTime.getTime());
        params.put("endTime", endTime.getTime());
        params.put("limit", 1000);
        List<List> rawKlinesList = JSON.parseArray(this.doRequest("/api/v3/klines", params, false, true)).toJavaList(List.class);

        List<InvestKlinesRecord> investKlinesRecordList = new ArrayList<>();
        for (List item : rawKlinesList) {

            InvestKlinesRecord record = new InvestKlinesRecord();
            record.setSymbol(symbol.toString());
            record.setOpenTime(new Timestamp((Long) item.get(0)));// 开盘时间
            record.setOpenPrice(new BigDecimal((String) item.get(1)));// 开盘价
            record.setHighPrice(new BigDecimal((String) item.get(2)));// 最高价
            record.setLowPrice(new BigDecimal((String) item.get(3)));// 最低价
            record.setClosePrice(new BigDecimal((String) item.get(4)));// 收盘价(当前K线未结束的即为最新价)
            record.setVolume(new BigDecimal((String) item.get(5)));// 成交量
            record.setCloseTime(new Timestamp((Long) item.get(6)));// 收盘时间
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

    public JSONObject order(BinanceOrderApiDto apiDto) {
        Map<String, Object> map = apiDto.toMap();
        JSONObject resultJson = JSON.parseObject(this.doRequest("/api/v3/order", map, true, false));
        if (StringUtils.isBlank(resultJson.getString("orderId"))) {
            throw new RuntimeException("币安下单未获取到交易订单号");
        }
        return resultJson;
    }

    private String doRequest(String url, Map<String, Object> params, Boolean signFlag, Boolean getFlag) {
        log.info("入参：{}", JSON.toJSONString(params));

        BinanceAccountInfo binanceAccountInfo = BinanceAccountContextHolder.get();
        if (null == binanceAccountInfo) {
            throw new RuntimeException("币安账户信息为空");
        }
        final String apiKey = binanceAccountInfo.getApiKey();
        final String apiSecret = binanceAccountInfo.getApiSecret();
        if (StringUtils.isAnyBlank(apiKey, apiSecret)) {
            throw new RuntimeException(binanceAccountInfo.getIdCardName() + "-币安账户API配置为空");
        }

        // 过滤空值
        params = params.entrySet().stream()
                .filter(entry -> null != entry.getValue() && StringUtils.isNotBlank(entry.getValue().toString()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));

        // 是否需要加签
        if (signFlag) {
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
        request.header("X-MBX-APIKEY", apiKey);
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


}
