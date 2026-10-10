package me.zhengjie.iou.service;

import me.zhengjie.exception.BadRequestException;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.iv.RandomIvGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** 后台录入时统一规范化、加密和计算身份证关联值；不执行三要素认证。 */
@Component
public class IouIdentitySupport {
    private final StandardPBEStringEncryptor encryptor;
    private final String hmacKey;

    public IouIdentitySupport(@Value("${jasypt.encryptor.password}") String encryptionPassword,
                              @Value("${IOU_IDENTITY_HMAC_KEY:}") String hmacKey) {
        this.hmacKey = hmacKey;
        this.encryptor = new StandardPBEStringEncryptor();
        this.encryptor.setPassword(encryptionPassword);
        this.encryptor.setAlgorithm("PBEWITHHMACSHA512ANDAES_256");
        this.encryptor.setIvGenerator(new RandomIvGenerator());
    }

    public String normalize(String idNo) {
        String value = idNo == null ? "" : idNo.trim().toUpperCase(Locale.ROOT);
        if (!value.matches("[0-9]{17}[0-9X]")) {
            throw new BadRequestException("身份证号须为18位");
        }
        return value;
    }

    public String encrypt(String idNo) {
        return encryptor.encrypt(normalize(idNo));
    }

    public String hash(String idNo) {
        if (hmacKey == null || hmacKey.length() < 32) {
            throw new BadRequestException("尚未配置 IOU_IDENTITY_HMAC_KEY");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hmacKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(normalize(idNo).getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(64);
            for (byte b : digest) {
                result.append(String.format("%02x", b & 0xff));
            }
            return result.toString();
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("身份证关联值计算失败", e);
        }
    }
}
