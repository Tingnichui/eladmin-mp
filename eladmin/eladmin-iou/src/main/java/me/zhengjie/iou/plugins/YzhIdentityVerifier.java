package me.zhengjie.iou.plugins;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import me.zhengjie.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
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

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;

/** 云账户姓名、身份证二要素核验；配置由 Jasypt 解密，不依赖云账户 SDK。 */
@Component
@ConditionalOnProperty(name = "iou.yzh.url")
public class YzhIdentityVerifier {
    private static final Logger log = LoggerFactory.getLogger(YzhIdentityVerifier.class);
    private static final String UNAVAILABLE = "身份核验服务暂不可用，请稍后重试";
    private final String url;
    private final String dealerId;
    private final String appKey;
    private final byte[] desKey;
    private final PrivateKey privateKey;
    private final RestTemplate restTemplate;

    @Autowired
    public YzhIdentityVerifier(@Value("${iou.yzh.url:}") String url,
                               @Value("${iou.yzh.dealer-id:}") String dealerId,
                               @Value("${iou.yzh.app-key:}") String appKey,
                               @Value("${iou.yzh.des-key:}") String desKey,
                               @Value("${iou.yzh.rsa-private-key:}") String privateKey,
                               @Value("${iou.yzh.sign-type:rsa}") String signType) {
        this(url, dealerId, appKey, desKey, privateKey, signType, createRestTemplate());
    }

    YzhIdentityVerifier(String url, String dealerId, String appKey, String desKey,
                        String privateKey, String signType, RestTemplate restTemplate) {
        this.url = url.replaceAll("/+$", "") + "/authentication/verify-id";
        this.dealerId = dealerId;
        this.appKey = appKey;
        this.desKey = desKey.getBytes(StandardCharsets.UTF_8);
        this.restTemplate = restTemplate;
        try {
            URI endpoint = URI.create(this.url);
            if (!"https".equalsIgnoreCase(endpoint.getScheme()) || endpoint.getHost() == null
                || endpoint.getRawUserInfo() != null || endpoint.getRawQuery() != null
                || endpoint.getRawFragment() != null || dealerId.trim().isEmpty()
                || appKey.trim().isEmpty() || this.desKey.length != 24 || !"rsa".equalsIgnoreCase(signType)) {
                throw new IllegalArgumentException();
            }
            String pem = privateKey.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "").replaceAll("\\s", "");
            this.privateKey = KeyFactory.getInstance("RSA").generatePrivate(
                new PKCS8EncodedKeySpec(Base64.getDecoder().decode(pem)));
        } catch (Exception e) {
            // 不附带配置值或底层异常，避免密钥进入启动日志。
            throw new IllegalStateException("云账户身份核验配置无效，请检查 iou.yzh 配置");
        }
    }

    public boolean iDCardVerify(String idCardName, String idCardNo) {
        String name = idCardName == null ? "" : idCardName.trim();
        String idCard = idCardNo == null ? "" : idCardNo.trim().toUpperCase(Locale.ROOT);
        if (name.isEmpty() || name.length() > 100 || !idCard.matches("[0-9]{17}[0-9X]")) {
            throw new BadRequestException("请填写姓名和18位身份证号");
        }
        String requestId = UUID.randomUUID().toString();
        ResponseEntity<String> response;
        JSONObject result;
        try {
            JSONObject payload = new JSONObject();
            payload.put("real_name", name);
            payload.put("id_card", idCard);
            String data = encrypt(JSON.toJSONString(payload));
            String mess = UUID.randomUUID().toString().replace("-", "");
            String timestamp = Long.toString(System.currentTimeMillis() / 1000);
            String plain = "data=" + data + "&mess=" + mess + "&timestamp=" + timestamp + "&key=" + appKey;
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("data", data);
            form.add("mess", mess);
            form.add("timestamp", timestamp);
            form.add("sign_type", "rsa");
            form.add("sign", sign(plain));
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(new MediaType("application", "x-www-form-urlencoded", StandardCharsets.UTF_8));
            headers.set("dealer-id", dealerId);
            headers.set("request-id", requestId);
            response = restTemplate.postForEntity(url, new HttpEntity<>(form, headers), String.class);
            result = JSON.parseObject(response.getBody());
            if (response.getStatusCodeValue() != 200 || result == null || result.getString("code") == null) {
                throw new IllegalStateException();
            }
        } catch (Exception e) {
            // 不记录请求体、响应体、供应商 message 或异常详情。
            log.warn("云账户身份核验请求异常，requestId={}", requestId);
            throw new BadRequestException(UNAVAILABLE);
        }
        String code = result.getString("code");
        log.info("云账户身份核验完成，requestId={}，code={}", requestId,
            code.matches("[0-9]{4}") ? code : "unknown");
        if ("0000".equals(code)) {
            return true;
        }
        if ("6000".equals(code)) {
            throw new BadRequestException(UNAVAILABLE);
        }
        // 非成功码可能包含不匹配、账户权限或额度问题，不能一律解释为身份不匹配。
        throw new BadRequestException("身份核验未通过，请核对信息或稍后重试");
    }

    private String encrypt(String payload) throws Exception {
        Cipher cipher = Cipher.getInstance("DESede/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(desKey, "DESede"),
            new IvParameterSpec(Arrays.copyOf(desKey, 8)));
        return Base64.getEncoder().encodeToString(cipher.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }

    private String sign(String plain) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(plain.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signature.sign());
    }

    private static RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        // 使用 Java 默认 TLS 证书和主机名校验。
        return new RestTemplate(factory);
    }
}
