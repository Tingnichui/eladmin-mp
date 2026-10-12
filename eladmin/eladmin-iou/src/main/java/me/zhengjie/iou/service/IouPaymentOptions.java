package me.zhengjie.iou.service;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.security.KeyFactory;
import java.security.spec.*;
import java.util.Base64;

@Data
@Component
@ConfigurationProperties(prefix = "iou.payment")
public class IouPaymentOptions {
    private boolean enabled;
    private String sysId = "";
    private String productId = "";
    private String huifuId = "";
    private String privateKey = "";
    private String publicKey = "";
    private String appId = "";
    private String appSecret = "";
    private String notifyUrl = "";
    private String oauthCallbackUrl = "";
    private String h5Origin = "";
    private String gatewayUrl = "https://api.huifu.com";
    private int expireMinutes = 10;

    public boolean ready() {
        if (!enabled || blank(sysId) || blank(productId) || blank(huifuId) || !appId.matches("wx[a-zA-Z0-9]+")
                || blank(appSecret) || expireMinutes < 2 || expireMinutes > 120) return false;
        try {
            KeyFactory factory = KeyFactory.getInstance("RSA");
            factory.generatePrivate(new PKCS8EncodedKeySpec(keyBytes(privateKey)));
            factory.generatePublic(new X509EncodedKeySpec(keyBytes(publicKey)));
            return https(notifyUrl, false) && https(oauthCallbackUrl, false) && https(h5Origin, true) && https(gatewayUrl, true);
        } catch (Exception ignored) { return false; }
    }
    static boolean blank(String value) { return value == null || value.trim().isEmpty(); }
    static byte[] keyBytes(String value) {
        return Base64.getDecoder().decode(value.replaceAll("-----[A-Z ]+-----", "").replaceAll("\\s", ""));
    }
    private boolean https(String value, boolean origin) {
        URI uri = URI.create(value);
        return "https".equals(uri.getScheme()) && uri.getHost() != null && uri.getRawUserInfo() == null
            && uri.getRawQuery() == null && uri.getRawFragment() == null
            && (!origin || uri.getRawPath().isEmpty() || "/".equals(uri.getRawPath()));
    }
    public void requireReady() {
        if (!ready()) throw new IouBusinessException(503, "PAYMENT_NOT_READY", "微信支付尚未开放，请稍后再试");
    }
    @Override public String toString() { return "IouPaymentOptions"; }
}
