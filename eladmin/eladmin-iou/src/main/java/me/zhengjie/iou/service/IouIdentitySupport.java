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
                              @Value("${iou.identity.hmac-key:${IOU_IDENTITY_HMAC_KEY:}}") String hmacKey) {
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

    public String decrypt(String encrypted) {
        if (encrypted == null || encrypted.trim().isEmpty()) {
            throw new BadRequestException("身份证信息暂不可查看");
        }
        try {
            return normalize(encryptor.decrypt(encrypted));
        } catch (RuntimeException e) {
            // 不向响应或异常日志传递密文、明文及底层解密异常。
            throw new BadRequestException("身份证信息暂不可查看");
        }
    }

    public String mask(String encrypted) {
        try {
            String idNo = decrypt(encrypted);
            return idNo.substring(0, 3) + "***********" + idNo.substring(14);
        } catch (BadRequestException e) {
            return null;
        }
    }

    public String hash(String idNo) {
        if (hmacKey == null || hmacKey.length() < 32) {
            throw new BadRequestException("实名注册配置不可用，请联系管理员");
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
