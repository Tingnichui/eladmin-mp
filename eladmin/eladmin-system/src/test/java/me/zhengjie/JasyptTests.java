package me.zhengjie;

import org.jasypt.encryption.StringEncryptor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class JasyptTests {

    @Resource
    private StringEncryptor encryptor;

    /**
     * 参数解密
     */
    @Test
    void decryptTest() {
        System.err.println(encryptor.decrypt("JGuVKc+ECqOSFxHSIDDt17PDlN8WVcz535TYbupL/TsKWE8ESED4+Q8jTqNzD2dneKWKOeuF1haAuXfI4X+RB1HfZ28zdBk8DbP2wOfgiYqouAKMeWzRBz9i9qMYs7piignc+NKIwjiBP4iU16csNCbVZ/Ly6sQY8E+2842rK2AV7b+sZa4GbuVXMfipm3Ae/Yjq4ljV0ADMdqrmvHWvcw=="));
        System.err.println(encryptor.decrypt("9w7cKcQLIn7fidkAddmFoKc1qxO2o52z6NXVL+xm2+hZHTSgNF4Z+i0MvBxM0aMR"));
        System.err.println(encryptor.decrypt("HR5Uk+mC7JLrlImx6VDVM60SpeRIkt1VDtz2KLQTDvwOpT+JP0U+l4JCqHu8ZfRl38Mgk7Ff8fp6WPS37k+UdA=="));
    }

    /**
     * 参数加密
     */
    @Test
    void encryptTest() {
        List<String> values = new ArrayList<>();
        addIfPresent(values, System.getProperty("jasypt.encrypt.value"));
        addIfPresent(values, System.getenv("ALIYUN_OCR_ACCESS_KEY_ID"));
        addIfPresent(values, System.getenv("ALIYUN_OCR_ACCESS_KEY_SECRET"));
        if (values.isEmpty()) {
            values.add("jdbc:log4jdbc:mysql://127.0.0.1:3306/media_crawler?serverTimezone=Asia/Shanghai&characterEncoding=utf8&useSSL=false");
            values.add("media_crawler");
            values.add("123123");
        }
        values.forEach(value -> System.err.println("ENC(" + encryptor.encrypt(value) + ")"));
    }

    private void addIfPresent(List<String> values, String value) {
        if (StringUtils.hasText(value)) {
            values.add(value);
        }
    }

}

