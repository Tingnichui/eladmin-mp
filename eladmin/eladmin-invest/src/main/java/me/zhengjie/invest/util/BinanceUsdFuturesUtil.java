package me.zhengjie.invest.util;

import cn.hutool.core.net.URLEncodeUtil;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import me.zhengjie.invest.constants.BinanceEnum;
import me.zhengjie.invest.domain.BinanceAccountInfo;
import me.zhengjie.invest.domain.dto.BinanceFundingRate;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 币安U本位合约
 */
@Component
public class BinanceUsdFuturesUtil {

    private static final Logger log = LoggerFactory.getLogger(BinanceUsdFuturesUtil.class);

    @Value("${proxy.host}")
    private String proxyHost;
    @Value("${proxy.port}")
    private Integer proxyPort;
    @Value("${binance.usd_futures.api_host}")
    private String apiHost;
    @Resource
    private DingdingUtil dingdingUtil;

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

    public void userTrades(BinanceEnum.SYMBOL symbol) {
        Map<String, Object> parmasMap = new HashMap<>();
        parmasMap.put("symbol", symbol);
        String string = this.doRequest("/fapi/v1/userTrades", parmasMap, true, true);
        System.err.println(string);
    }

    public JSONObject account() {
        Map<String, Object> parmasMap = new HashMap<>();
        parmasMap.put("symbol", "symbol");
        JSONObject resJson = JSON.parseObject(this.doRequest("/fapi/v2/account", parmasMap, true, true));

        List<JSONObject> positions = resJson.getJSONArray("positions").stream().map(obj -> (JSONObject) obj).collect(Collectors.toList());
        return positions.stream().filter(v -> v.getString("symbol").equals("BTCUSDT") && v.getString("positionSide").equals("SHORT")).findFirst().orElse(null);
    }
}
