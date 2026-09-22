package me.zhengjie.invest.util;

import cn.hutool.core.net.URLEncodeUtil;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.alibaba.fastjson2.JSON;
import me.zhengjie.invest.domain.BinanceAccountInfo;
import me.zhengjie.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class BinanceHttpClient {

    private static final Logger log = LoggerFactory.getLogger(BinanceHttpClient.class);

    private final BinanceProxyExecutor proxyExecutor;

    public BinanceHttpClient(BinanceProxyExecutor proxyExecutor) {
        this.proxyExecutor = proxyExecutor;
    }

    public String request(String apiHost, String path, Map<String, Object> params,
                          boolean signed, Method method) {
        Map<String, Object> baseParams = params.entrySet().stream()
                .filter(entry -> entry.getValue() != null
                        && StringUtils.isNotBlank(entry.getValue().toString()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (left, right) -> right,
                        LinkedHashMap::new
                ));
        log.info("币安接口入参: path={}, params={}", path, JSON.toJSONString(baseParams));

        BinanceAccountInfo account = signed ? requireAccount() : null;
        boolean failoverAllowed = Method.GET == method;
        HttpResponse response = proxyExecutor.execute(failoverAllowed, proxy -> {
            Map<String, Object> attemptParams = new LinkedHashMap<>(baseParams);
            HttpRequest request = HttpUtil.createRequest(method,
                    buildUrl(apiHost, path, attemptParams, account));
            if (account != null) {
                request.header("X-MBX-APIKEY", account.getApiKey());
            }
            return request
                    .setProxy(proxy)
                    .setConnectionTimeout(proxyExecutor.getConnectTimeoutMs())
                    .setReadTimeout(proxyExecutor.getReadTimeoutMs())
                    .execute();
        });

        String body = response.body();
        log.info("币安接口出参: path={}, status={}, body={}", path, response.getStatus(), body);
        if (response.getStatus() != 200) {
            throw new RuntimeException("币安接口调用失败: status=" + response.getStatus() + ", body=" + body);
        }
        return body;
    }

    private String buildUrl(String apiHost, String path, Map<String, Object> params,
                            BinanceAccountInfo account) {
        if (account != null) {
            params.put("timestamp", System.currentTimeMillis());
            String queryString = toQueryString(params);
            String signature = new HMac(HmacAlgorithm.HmacSHA256,
                    account.getApiSecret().getBytes(StandardCharsets.UTF_8)).digestHex(queryString);
            params.put("signature", signature);
        }
        String queryString = toQueryString(params);
        return queryString.isEmpty() ? apiHost + path : apiHost + path + "?" + queryString;
    }

    private String toQueryString(Map<String, Object> params) {
        return params.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + URLEncodeUtil.encode(entry.getValue().toString()))
                .collect(Collectors.joining("&"));
    }

    private BinanceAccountInfo requireAccount() {
        BinanceAccountInfo account = BinanceAccountContextHolder.get();
        if (account == null) {
            throw new RuntimeException("币安账户信息为空");
        }
        if (StringUtils.isAnyBlank(account.getApiKey(), account.getApiSecret())) {
            throw new RuntimeException(account.getIdCardName() + "-币安账户API配置为空");
        }
        return account;
    }
}
