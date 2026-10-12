package me.zhengjie.iou.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import me.zhengjie.iou.domain.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** V4 aggregate payment, separate from hosting payment. Never retry a payment POST here. */
@Service
public class IouHuifuGateway {
    private final IouPaymentOptions options;
    private final RestTemplate http;
    private final ObjectMapper json = new ObjectMapper().configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    @Autowired public IouHuifuGateway(IouPaymentOptions options) { this(options, client()); }
    public IouHuifuGateway(IouPaymentOptions options, RestTemplate http) { this.options = options; this.http = http; }
    private static RestTemplate client() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000); factory.setReadTimeout(5000);
        return new PaymentHttpClient(factory);
    }
    public Map<String,Object> create(IouFeeOrder order, IouPaymentAttempt attempt) {
        Map<String,Object> data = map("req_date", attempt.getReqDate(), "req_seq_id", attempt.getReqSeqId(),
            "huifu_id", attempt.getHuifuId(), "trade_type", "T_JSAPI", "trans_amt", order.getAmount().toPlainString(),
            "goods_desc", "电子借条平台服务费", "notify_url", options.getNotifyUrl(),
            "time_expire", attempt.getExpiresAt().toInstant().atZone(ZoneId.of("Asia/Shanghai")).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")),
            "method_expand", encode(map("sub_appid", options.getAppId(), "sub_openid", order.getPayerOpenid())));
        return request("/v4/trade/payment/create", data);
    }
    public Map<String,Object> query(IouPaymentAttempt attempt) {
        return request("/v4/trade/payment/scanpay/query", map("huifu_id", attempt.getHuifuId(), "req_date", attempt.getReqDate(), "req_seq_id", attempt.getReqSeqId()));
    }
    public Map<String,Object> close(IouPaymentAttempt attempt) {
        return request("/v2/trade/payment/scanpay/close", map("huifu_id", attempt.getHuifuId(), "req_date", today(),
            "req_seq_id", UUID.randomUUID().toString().replace("-", ""), "org_req_date", attempt.getReqDate(), "org_req_seq_id", attempt.getReqSeqId()));
    }
    public String oauthOpenid(String code) {
        options.requireReady();
        try {
            Map<?,?> response = http.getForObject("https://api.weixin.qq.com/sns/oauth2/access_token?appid={app}&secret={secret}&code={code}&grant_type=authorization_code",
                Map.class, options.getAppId(), options.getAppSecret(), code);
            String openid = response == null ? "" : String.valueOf(response.get("openid"));
            if (response == null || response.containsKey("errcode") || !openid.matches("[a-zA-Z0-9_-]{10,128}")) throw unavailable();
            return openid;
        } catch (Exception ignored) { throw new IouBusinessException(502, "WECHAT_AUTH_FAILED", "微信授权失败，请重新授权"); }
    }
    public Map<String,Object> notification(String raw, String sign) {
        if (raw == null || raw.length() > 65536 || !verify(raw, sign)) throw new IouBusinessException(400, "INVALID_PAYMENT_SIGNATURE", "支付通知验签失败");
        return decode(raw);
    }
    private Map<String,Object> request(String path, Map<String,Object> data) {
        options.requireReady();
        try {
            String serialized = encode(data);
            HttpHeaders headers = new HttpHeaders(); headers.setContentType(MediaType.parseMediaType("application/json;charset=UTF-8"));
            headers.set("jpt-x-skill-source", "eladmin-iou"); headers.set("jpt-x-skill-huifu_id", options.getHuifuId());
            String raw = http.postForObject(options.getGatewayUrl().replaceAll("/+$", "") + path,
                new HttpEntity<>(encode(map("sys_id", options.getSysId(), "product_id", options.getProductId(), "data", data, "sign", sign(serialized))), headers), String.class);
            Map<String,Object> response = decode(raw);
            Object body = response.get("data");
            if (!(body instanceof Map) || !verify(encode(body), String.valueOf(response.get("sign")))) throw unavailable();
            Map<String,Object> result=(Map<String,Object>) body;
            if(!Objects.equals(data.get("req_seq_id"),result.get("req_seq_id"))||!Objects.equals(data.get("huifu_id"),result.get("huifu_id")))throw unavailable();
            return result;
        } catch (Exception ignored) { throw unavailable(); }
    }
    public String sign(String raw) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(IouPaymentOptions.keyBytes(options.getPrivateKey()))));
            signature.update(raw.getBytes(StandardCharsets.UTF_8)); return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception ignored) { throw unavailable(); }
    }
    public boolean verify(String raw, String sign) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(IouPaymentOptions.keyBytes(options.getPublicKey()))));
            signature.update(raw.getBytes(StandardCharsets.UTF_8)); return signature.verify(Base64.getDecoder().decode(sign));
        } catch (Exception ignored) { return false; }
    }
    public Map<String,Object> decode(String raw) {
        try { return json.readValue(raw, new TypeReference<Map<String,Object>>() { }); }
        catch (Exception ignored) { throw unavailable(); }
    }
    public String encode(Object value) {
        try { return json.writeValueAsString(value); } catch (Exception ignored) { throw unavailable(); }
    }
    static String today() { return LocalDate.now(ZoneId.of("Asia/Shanghai")).format(DateTimeFormatter.BASIC_ISO_DATE); }
    static Map<String,Object> map(Object... items) {
        Map<String,Object> result = new TreeMap<>(); for (int i = 0; i < items.length; i += 2) result.put((String) items[i], items[i+1]); return result;
    }
    private static IouBusinessException unavailable() { return new IouBusinessException(502, "PAYMENT_RESULT_UNKNOWN", "支付结果暂未确认，请稍后查询，勿重复付款"); }
    /** Dedicated logger name prevents HTTP debug logging of credentials and pay_info. */
    private static class PaymentHttpClient extends RestTemplate {
        PaymentHttpClient(SimpleClientHttpRequestFactory factory) { super(factory); }
    }
}
