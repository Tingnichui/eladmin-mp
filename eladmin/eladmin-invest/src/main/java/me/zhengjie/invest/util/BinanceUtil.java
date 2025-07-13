package me.zhengjie.invest.util;

import cn.hutool.core.net.URLEncodeUtil;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import cn.hutool.http.HttpUtil;
import me.zhengjie.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
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
    @Value("${binance.api_key}")
    private String apiKey;
    @Value("${binance.api_secret}")
    private String apiSecret;

    public String getMyTrades(String symbol) {
        Map<String, Object> params = new HashMap<>();
        params.put("symbol", symbol);
//        params.put("startTime", startTime.getTime());
        params.put("timestamp", System.currentTimeMillis());
        return this.doGet("/api/v3/myTrades", params);
    }

    private String doGet(String url, Map<String, Object> params) {
        String queryString = params.entrySet().stream()
                .filter(entry -> StringUtils.isNotBlank(entry.getValue().toString()))
                .map(entry -> entry.getKey() + "=" + URLEncodeUtil.encode(entry.getValue().toString()))
                .collect(Collectors.joining("&"));

        String signature = new HMac(HmacAlgorithm.HmacSHA256, apiSecret.getBytes(StandardCharsets.UTF_8)).digestHex(queryString);
        params.put("signature", signature);

        String finalQuery = params.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + URLEncodeUtil.encode(entry.getValue().toString()))
                .collect(Collectors.joining("&"));

        String fullUrl = apiHost + url + "?" + finalQuery;
        String body = HttpUtil.createGet(fullUrl)
                .header("X-MBX-APIKEY", apiKey)
                .setProxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort)))
                .execute().body();

        log.info("出参：{}", body);
        return body;
    }


}
