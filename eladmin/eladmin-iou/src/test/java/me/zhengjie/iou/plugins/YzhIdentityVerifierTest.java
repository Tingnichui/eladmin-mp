package me.zhengjie.iou.plugins;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import me.zhengjie.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/** 全部 HTTP 请求由模拟服务器拦截，不使用实际商户配置或真实证件。 */
class YzhIdentityVerifierTest {
    private static final String ENDPOINT = "https://identity.example.test/authentication/verify-id";
    private static final String DES_KEY = "0123456789abcdefghijklmn";
    private MockRestServiceServer server;
    private YzhIdentityVerifier verifier;
    private KeyPair keyPair;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        verifier = new YzhIdentityVerifier("https://identity.example.test/", "test-dealer", "test-app-key",
            DES_KEY, Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()), "rsa", restTemplate);
    }

    @Test
    void requestUsesSdkCompatibleEncryptionAndSignature() {
        server.expect(requestTo(ENDPOINT)).andExpect(method(HttpMethod.POST))
            .andExpect(header("dealer-id", "test-dealer"))
            .andExpect(request -> {
                assertNotNull(request.getHeaders().getFirst("request-id"));
                String body = ((MockClientHttpRequest) request).getBodyAsString(StandardCharsets.UTF_8);
                assertFalse(body.contains("00000020000101000"));
                Map<String, String> form = new HashMap<>();
                for (String field : body.split("&")) {
                    String[] pair = field.split("=", 2);
                    form.put(URLDecoder.decode(pair[0], "UTF-8"), URLDecoder.decode(pair[1], "UTF-8"));
                }
                assertEquals("rsa", form.get("sign_type"));
                assertTrue(Math.abs(System.currentTimeMillis() / 1000 - Long.parseLong(form.get("timestamp"))) < 5);
                try {
                    byte[] key = DES_KEY.getBytes(StandardCharsets.UTF_8);
                    Cipher cipher = Cipher.getInstance("DESede/CBC/PKCS5Padding");
                    cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "DESede"),
                        new IvParameterSpec(Arrays.copyOf(key, 8)));
                    JSONObject payload = JSON.parseObject(new String(cipher.doFinal(
                        Base64.getDecoder().decode(form.get("data"))), StandardCharsets.UTF_8));
                    assertEquals("测试姓名", payload.getString("real_name"));
                    assertEquals("00000020000101000X", payload.getString("id_card"));
                    assertEquals(2, payload.size());
                    Signature signature = Signature.getInstance("SHA256withRSA");
                    signature.initVerify(keyPair.getPublic());
                    signature.update(("data=" + form.get("data") + "&mess=" + form.get("mess")
                        + "&timestamp=" + form.get("timestamp") + "&key=test-app-key").getBytes(StandardCharsets.UTF_8));
                    assertTrue(signature.verify(Base64.getDecoder().decode(form.get("sign"))));
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
            }).andRespond(withSuccess("{\"code\":\"0000\",\"message\":\"成功\"}", MediaType.APPLICATION_JSON));
        assertTrue(verifier.iDCardVerify(" 测试姓名 ", " 00000020000101000x "));
        server.verify();
    }

    @Test
    void businessFailureDoesNotExposeProviderMessage() {
        server.expect(requestTo(ENDPOINT)).andRespond(withSuccess(
            "{\"code\":\"2001\",\"message\":\"sensitive-provider-data\"}", MediaType.APPLICATION_JSON));
        BadRequestException error = assertThrows(BadRequestException.class,
            () -> verifier.iDCardVerify("测试姓名", "00000020000101000X"));
        assertFalse(error.getMessage().contains("sensitive-provider-data"));
        server.verify();
    }

    @Test
    void httpFailureCannotPassEvenWithSuccessBody() {
        server.expect(requestTo(ENDPOINT)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
            .body("{\"code\":\"0000\"}").contentType(MediaType.APPLICATION_JSON));
        BadRequestException error = assertThrows(BadRequestException.class,
            () -> verifier.iDCardVerify("测试姓名", "00000020000101000X"));
        assertEquals("身份核验服务暂不可用，请稍后重试", error.getMessage());
        server.verify();
    }

    @Test
    void missingResultAndMalformedJsonFailClosed() {
        server.expect(requestTo(ENDPOINT)).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(ENDPOINT)).andRespond(withSuccess("not-json", MediaType.APPLICATION_JSON));
        assertThrows(BadRequestException.class, () -> verifier.iDCardVerify("测试姓名", "00000020000101000X"));
        assertThrows(BadRequestException.class, () -> verifier.iDCardVerify("测试姓名", "00000020000101000X"));
        server.verify();
    }

    @Test
    void providerInternalErrorIsNotReportedAsIdentityMismatch() {
        server.expect(requestTo(ENDPOINT)).andRespond(withSuccess(
            "{\"code\":\"6000\",\"message\":\"系统内部错误，请稍后重试\"}", MediaType.APPLICATION_JSON));
        BadRequestException error = assertThrows(BadRequestException.class,
            () -> verifier.iDCardVerify("测试姓名", "00000020000101000X"));
        assertEquals("身份核验服务暂不可用，请稍后重试", error.getMessage());
        server.verify();
    }

    @Test
    void invalidInputDoesNotCallProvider() {
        assertThrows(BadRequestException.class, () -> verifier.iDCardVerify(" ", "00000020000101000X"));
        assertThrows(BadRequestException.class, () -> verifier.iDCardVerify("测试姓名", "invalid"));
        server.verify();
    }
}
