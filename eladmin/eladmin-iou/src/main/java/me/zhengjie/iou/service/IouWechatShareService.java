package me.zhengjie.iou.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Service
public class IouWechatShareService {
    private final IouWechatOptions options;
    private final RestTemplate http;
    private String token;
    private String ticket;
    private long tokenUntil;
    private long ticketUntil;
    private long retryAfter;

    @Autowired
    public IouWechatShareService(IouWechatOptions options) {
        this(options, newClient());
    }
    public IouWechatShareService(IouWechatOptions options, RestTemplate http) {
        this.options = options;
        this.http = http;
    }
    private static RestTemplate newClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        return new IouWechatHttpClient(factory);
    }
    public Map<String, Object> configure(String pageUrl, String reference) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("enabled", false);
        if (!options.isEnabled() || blank(options.getAppId()) || blank(options.getAppSecret()) || blank(options.getPublicOrigin())) {
            return result;
        }
        String origin = options.getPublicOrigin().replaceAll("/+$", "");
        URI allowed = parse(origin);
        URI page = parse(pageUrl);
        if (!"https".equals(allowed.getScheme()) || allowed.getHost() == null || allowed.getRawUserInfo() != null
                || !blank(allowed.getRawPath()) || allowed.getRawQuery() != null || allowed.getRawFragment() != null
                || !sameOrigin(allowed, page) || page.getRawUserInfo() != null || page.getRawFragment() != null) {
            throw new IouBusinessException(400, "WECHAT_URL_INVALID", "当前地址暂不支持微信卡片，请复制邀请发送");
        }
        String nonce = UUID.randomUUID().toString().replace("-", "");
        long timestamp = System.currentTimeMillis() / 1000;
        result.put("signature", signature(getTicket(), nonce, timestamp, pageUrl));
        result.put("enabled", true);
        result.put("appId", options.getAppId());
        result.put("nonceStr", nonce);
        result.put("timestamp", timestamp);
        result.put("title", "借条签署邀请");
        result.put("desc", "请核对借款约定及还款计划，确认后签署。");
        result.put("link", origin + "/#/notes/join?reference=" + reference);
        result.put("imgUrl", blank(options.getImageUrl()) ? origin + "/wechat-invitation.png" : options.getImageUrl());
        return result;
    }
    private synchronized String getTicket() {
        long now = System.currentTimeMillis();
        if (ticket != null && now < ticketUntil) { return ticket; }
        if (now < retryAfter) { throw unavailable(); }
        try {
            if (token == null || now >= tokenUntil) {
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("grant_type", "client_credential");
                body.put("appid", options.getAppId());
                body.put("secret", options.getAppSecret());
                body.put("force_refresh", false);
                Map<?, ?> response = http.postForObject("https://api.weixin.qq.com/cgi-bin/stable_token", body, Map.class);
                token = field(response, "access_token");
                tokenUntil = expires(response);
            }
            Map<?, ?> response = http.getForObject("https://api.weixin.qq.com/cgi-bin/ticket/getticket?access_token={token}&type=jsapi", Map.class, token);
            ticket = field(response, "ticket");
            ticketUntil = expires(response);
            return ticket;
        } catch (Exception ignored) {
            // Do not expose upstream URLs, token, ticket, secret or exception messages.
            token = null;
            ticket = null;
            retryAfter = now + 30000;
            throw unavailable();
        }
    }
    private static String field(Map<?, ?> response, String name) {
        if (response == null || response.get("errcode") != null && !"0".equals(String.valueOf(response.get("errcode")))
                || !(response.get(name) instanceof String) || blank((String) response.get(name))) {
            throw unavailable();
        }
        return (String) response.get(name);
    }
    private static long expires(Map<?, ?> response) {
        long seconds = Long.parseLong(String.valueOf(response.get("expires_in")));
        if (seconds <= 120) { throw unavailable(); }
        return System.currentTimeMillis() + (Math.min(seconds, 7200) - 120) * 1000;
    }
    public static String signature(String ticket, String nonce, long timestamp, String url) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-1").digest(("jsapi_ticket=" + ticket + "&noncestr=" + nonce
                    + "&timestamp=" + timestamp + "&url=" + url).getBytes(StandardCharsets.UTF_8));
            StringBuilder value = new StringBuilder();
            for (byte b : digest) { value.append(String.format("%02x", b & 255)); }
            return value.toString();
        } catch (Exception ignored) { throw unavailable(); }
    }
    private static boolean blank(String value) { return value == null || value.trim().isEmpty(); }
    private static URI parse(String value) {
        try {
            if (blank(value) || value.length() > 2048) { throw new IllegalArgumentException(); }
            return new URI(value);
        } catch (Exception ignored) { throw new IouBusinessException(400, "WECHAT_URL_INVALID", "当前地址暂不支持微信卡片，请复制邀请发送"); }
    }
    private static boolean sameOrigin(URI a, URI b) {
        return Objects.equals(a.getScheme(), b.getScheme()) && a.getHost() != null && a.getHost().equalsIgnoreCase(b.getHost())
                && port(a) == port(b);
    }
    private static int port(URI uri) { return uri.getPort() == -1 ? 443 : uri.getPort(); }
    private static IouBusinessException unavailable() {
        return new IouBusinessException(503, "WECHAT_SHARE_UNAVAILABLE", "微信卡片暂不可用，请复制邀请发送");
    }
}

// A dedicated logger category lets deployments suppress HTTP credential logging.
class IouWechatHttpClient extends RestTemplate {
    IouWechatHttpClient(SimpleClientHttpRequestFactory factory) { super(factory); }
}
