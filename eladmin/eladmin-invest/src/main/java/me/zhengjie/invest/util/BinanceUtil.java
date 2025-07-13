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
    @Resource
    private DingdingUtil dingdingUtil;

    public String getMyTrades(String symbol) {
        Map<String, Object> params = new HashMap<>();
        params.put("symbol", symbol);
//        params.put("startTime", startTime.getTime());
        return this.doRequest("/api/v3/myTrades", params, true);
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

    public JSONObject order(BinanceOrderApiDto apiDto) {
        Map<String, Object> map = apiDto.toMap();
        JSONObject resultJson = null;
        try {
            resultJson = JSON.parseObject(this.doRequest("/api/v3/order", map, false));
            if (StringUtils.isBlank(resultJson.getString("orderId"))) {
                throw new RuntimeException("币安下单未获取到交易订单号");
            }
        } catch (Exception e) {
            dingdingUtil.sendMsg("币安下单出现异常" + e.getMessage());
        }
        return resultJson;
    }

    private String doRequest(String url, Map<String, Object> params, Boolean getFlag) {
        log.info("入参：{}", JSON.toJSONString(params));

        // 过滤空值
        params = params.entrySet().stream()
                .filter(entry -> null != entry.getValue() && StringUtils.isNotBlank(entry.getValue().toString()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));

        // 增加时间戳
        params.put("timestamp", System.currentTimeMillis());

        // 加签
        String queryString = params.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + URLEncodeUtil.encode(entry.getValue().toString()))
                .collect(Collectors.joining("&"));
        String signature = new HMac(HmacAlgorithm.HmacSHA256, apiSecret.getBytes(StandardCharsets.UTF_8)).digestHex(queryString);
        params.put("signature", signature);

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
