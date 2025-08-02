package me.zhengjie.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.security.PrivateKey;
import java.security.PublicKey;

@Component
public class DataSecurityUtil {

    @Value("${data.rsa.private_key}")
    private String rsaPrivateKeyStr;
    @Value("${data.rsa.public_key}")
    private String rsaPublicKeyStr;

    private PrivateKey rsaPrivateKey;
    private PublicKey rsaPublicKey;

    @PostConstruct
    public void initKey() throws Exception {
        this.rsaPrivateKey = RsaUtils.getPrivateKey(rsaPrivateKeyStr);
        this.rsaPublicKey = RsaUtils.getPublicKey(rsaPublicKeyStr);
    }

    public String decrypt(String str) throws Exception {
        return RsaUtils.decryptByPrivateKey(rsaPrivateKey, str);
    }

    public String encrypt(String str) throws Exception {
        return RsaUtils.encryptByPublicKey(rsaPublicKey, str);
    }


}
