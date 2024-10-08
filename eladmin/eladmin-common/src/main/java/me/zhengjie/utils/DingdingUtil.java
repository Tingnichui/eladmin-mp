package me.zhengjie.utils;

import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Geng Hui
 * @date 2022/8/13 0:17
 */
@Slf4j
@Component
public class DingdingUtil {

    @Value("${notify.dingding:}")
    public String webhook;

    @Value("${notify.secret:}")
    public String secret;

    public void sendMsg(String content) {
        try {
            Map<String, Object> text = new HashMap<>();
            text.put("content", content);
            Map<String, Object> params = new HashMap<>();
            params.put("msgtype", "text");
            params.put("text", text);
            String url = this.createUrl();
            String jsonString = JSON.toJSONString(params);
            String res = HttpUtil.post(url, jsonString);
            log.info("发送地址：{}，发送内容：{}，发送结果：{}", url, jsonString, res);
        } catch (Exception e) {
            log.error("钉钉发送消息失败", e);
        }
    }

    private String createUrl() throws Exception {
        Long timestamp = System.currentTimeMillis();
        String stringToSign = timestamp + "\n" + secret;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
        String sign = URLEncoder.encode(new String(Base64.encodeBase64(signData)), "UTF-8");
        return webhook + "&timestamp=" + timestamp + "&sign=" + sign;
    }

}
