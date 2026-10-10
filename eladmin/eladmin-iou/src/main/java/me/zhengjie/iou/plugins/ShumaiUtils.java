package me.zhengjie.iou.plugins;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import me.zhengjie.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.UUID;

/** 数脉 API 工具：凭据由 Jasypt 解密，签名和表单请求遵循官方二要素协议。 */
@Component
@ConditionalOnProperty(name = "iou.shumai.url")
public class ShumaiUtils {
    private static final Logger log = LoggerFactory.getLogger(ShumaiUtils.class);
    private static final String UNAVAILABLE = "身份核验服务暂不可用，请稍后重试";
    private final String url;
    private final String appId;
    private final String appSecurity;
    private final RestTemplate restTemplate;

    @Autowired
    public ShumaiUtils(@Value("${iou.shumai.url:}") String url,
                      @Value("${iou.shumai.app-id:}") String appId,
                      @Value("${iou.shumai.app-security:}") String appSecurity) {
        this(url, appId, appSecurity, createRestTemplate());
    }

    ShumaiUtils(String url, String appId, String appSecurity, RestTemplate restTemplate) {
        try {
            URI endpoint = URI.create(url);
            if (!"https".equalsIgnoreCase(endpoint.getScheme()) || endpoint.getHost() == null
                || endpoint.getRawUserInfo() != null || endpoint.getRawQuery() != null
                || endpoint.getRawFragment() != null || appId == null || appId.trim().isEmpty()
                || appSecurity == null || appSecurity.trim().isEmpty()) {
                throw new IllegalArgumentException();
            }
        } catch (RuntimeException e) {
            throw new IllegalStateException("数脉身份核验配置无效，请检查 iou.shumai 配置");
        }
        this.url = url.replaceAll("/+$", "") + "/v4/id_card/check";
        this.appId = appId;
        this.appSecurity = appSecurity;
        this.restTemplate = restTemplate;
    }

    /** 一致返回 true，不一致返回 false；无记录、权限、余额及通信异常均抛出异常。 */
    public boolean iDCardVerify(String idCardName, String idCardNo) {
        String name = idCardName == null ? "" : idCardName.trim();
        String idCard = idCardNo == null ? "" : idCardNo.trim().toUpperCase(Locale.ROOT);
        if (name.isEmpty() || name.length() > 100 || !idCard.matches("[0-9]{17}[0-9X]")) {
            throw new BadRequestException("请填写姓名和18位身份证号");
        }
        String requestId = UUID.randomUUID().toString();
        JSONObject result;
        try {
            String timestamp = Long.toString(System.currentTimeMillis());
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("appid", appId);
            form.add("timestamp", timestamp);
            form.add("sign", sign(timestamp));
            form.add("name", name);
            form.add("idcard", idCard);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(new MediaType("application", "x-www-form-urlencoded", StandardCharsets.UTF_8));
            ResponseEntity<String> response = restTemplate.postForEntity(url, new HttpEntity<>(form, headers), String.class);
            result = JSON.parseObject(response.getBody());
            if (response.getStatusCodeValue() != 200 || result == null) {
                throw new IllegalStateException();
            }
        } catch (Exception e) {
            // 不输出异常详情，以免 HTTP 请求或响应中的证件、密钥进入日志。
            log.warn("数脉身份核验请求异常，requestId={}", requestId);
            throw new BadRequestException(UNAVAILABLE);
        }
        Object code = result.get("code");
        log.info("数脉身份核验完成，requestId={}，code={}", requestId,
            code instanceof Integer ? code : "unknown");
        if (!Integer.valueOf(200).equals(code) || !Boolean.TRUE.equals(result.get("success"))) {
            // 业务权限、额度、供应商异常不是身份不匹配；不回传供应商原始消息。
            throw new BadRequestException(UNAVAILABLE);
        }
        Object data = result.get("data");
        Object identityResult = data instanceof JSONObject ? ((JSONObject) data).get("result") : null;
        if (Integer.valueOf(0).equals(identityResult)) {
            return true;
        }
        if (Integer.valueOf(1).equals(identityResult)) {
            return false;
        }
        if (Integer.valueOf(2).equals(identityResult)) {
            throw new BadRequestException("未查询到身份记录，请核对信息或稍后重试");
        }
        // 缺失字段和未知状态不能按默认值 0 放行。
        throw new BadRequestException(UNAVAILABLE);
    }

    private String sign(String timestamp) {
        try {
            byte[] digest = MessageDigest.getInstance("MD5").digest(
                (appId + "&" + timestamp + "&" + appSecurity).getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(32);
            for (byte value : digest) {
                result.append(String.format(Locale.ROOT, "%02x", value & 0xff));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("数脉请求签名生成失败");
        }
    }

    private static RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                super.prepareConnection(connection, httpMethod);
                connection.setInstanceFollowRedirects(false);
            }
        };
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        // 默认 TLS 校验；不重试付费请求，不将敏感表单重定向到其他地址。
        return new RestTemplate(factory);
    }
}
