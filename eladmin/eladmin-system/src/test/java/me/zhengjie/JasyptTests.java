package me.zhengjie;

import org.jasypt.encryption.StringEncryptor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

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
    }

    /**
     * 参数加密
     */
    @Test
    void encryptTest() {
        System.err.println(encryptor.encrypt("jdbc:log4jdbc:mysql://127.0.0.1:3306/media_crawler?serverTimezone=Asia/Shanghai&characterEncoding=utf8&useSSL=false"));
    }

}

