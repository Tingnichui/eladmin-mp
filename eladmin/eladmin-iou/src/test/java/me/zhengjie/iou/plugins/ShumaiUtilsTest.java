package me.zhengjie.iou.plugins;

import me.zhengjie.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/** 聚焦协议和失败边界；不使用实际凭据、真实证件或付费外部服务。 */
class ShumaiUtilsTest {
    private static final String ENDPOINT = "https://identity.example.test/v4/id_card/check";
    private static final String NAME = "测试姓名";
    private static final String ID_CARD = "00000020000101000X";
    private MockRestServiceServer server;
    private ShumaiUtils utils;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        utils = new ShumaiUtils("https://identity.example.test/", "test-app-id", "test-secret", restTemplate);
    }

    @Test
    void requestUsesUtf8FormAndOfficialSignature() {
        server.expect(requestTo(ENDPOINT)).andExpect(method(HttpMethod.POST))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(request -> {
                String body = ((MockClientHttpRequest) request).getBodyAsString(StandardCharsets.UTF_8);
                assertFalse(body.contains("test-secret"));
                assertFalse(body.contains(NAME));
                Map<String, String> form = new HashMap<>();
                for (String field : body.split("&")) {
                    String[] pair = field.split("=", 2);
                    form.put(URLDecoder.decode(pair[0], "UTF-8"), URLDecoder.decode(pair[1], "UTF-8"));
                }
                assertEquals(5, form.size());
                assertEquals("test-app-id", form.get("appid"));
                assertEquals(NAME, form.get("name"));
                assertEquals(ID_CARD, form.get("idcard"));
                assertTrue(Math.abs(System.currentTimeMillis() - Long.parseLong(form.get("timestamp"))) < 5000);
                try {
                    byte[] digest = MessageDigest.getInstance("MD5").digest(
                        ("test-app-id&" + form.get("timestamp") + "&test-secret").getBytes(StandardCharsets.UTF_8));
                    String expected = String.format("%032x", new java.math.BigInteger(1, digest));
                    assertEquals(expected, form.get("sign"));
                    assertTrue(form.get("sign").matches("[0-9a-f]{32}"));
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
            }).andRespond(withSuccess(response("0"), MediaType.APPLICATION_JSON));
        assertTrue(utils.iDCardVerify(" " + NAME + " ", " " + ID_CARD.toLowerCase() + " "));
        server.verify();
    }

    @Test
    void mismatchIsFalseEvenWhenRequestSucceeded() {
        server.expect(requestTo(ENDPOINT)).andRespond(withSuccess(response("1"), MediaType.APPLICATION_JSON));
        assertFalse(utils.iDCardVerify(NAME, ID_CARD));
        server.verify();
    }

    @Test
    void noRecordDoesNotPass() {
        server.expect(requestTo(ENDPOINT)).andRespond(withSuccess(response("2"), MediaType.APPLICATION_JSON));
        BadRequestException error = assertThrows(BadRequestException.class, () -> utils.iDCardVerify(NAME, ID_CARD));
        assertEquals("未查询到身份记录，请核对信息或稍后重试", error.getMessage());
        server.verify();
    }

    @Test
    void missingMalformedOrUnexpectedResultFailsClosed() {
        String[] bodies = {"{}", "not-json", "null", "[]",
            "{\"success\":true,\"code\":200}", "{\"success\":true,\"code\":200,\"data\":{}}",
            "{\"success\":true,\"code\":200,\"data\":\"unexpected\"}",
            response("null"), response("false"), response("\"0\""), response("0.0"), response("9"),
            "{\"success\":false,\"code\":200,\"data\":{\"result\":0}}",
            "{\"code\":200,\"data\":{\"result\":0}}",
            "{\"success\":true,\"data\":{\"result\":0}}"};
        for (String body : bodies) {
            server.expect(requestTo(ENDPOINT)).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        }
        for (String ignored : bodies) {
            assertThrows(BadRequestException.class, () -> utils.iDCardVerify(NAME, ID_CARD));
        }
        server.verify();
    }

    @Test
    void permissionBalanceAndServiceErrorsDoNotExposeProviderMessage() {
        int[] codes = {400, 404, 500, 501, 601, 602, 603, 604, 605, 1001};
        for (int code : codes) {
            server.expect(requestTo(ENDPOINT)).andRespond(withSuccess(
                "{\"success\":false,\"code\":" + code + ",\"msg\":\"sensitive-data\",\"data\":{\"result\":0}}",
                MediaType.APPLICATION_JSON));
        }
        for (int ignored : codes) {
            BadRequestException error = assertThrows(BadRequestException.class, () -> utils.iDCardVerify(NAME, ID_CARD));
            assertEquals("身份核验服务暂不可用，请稍后重试", error.getMessage());
        }
        server.verify();
    }

    @Test
    void httpErrorsAndRedirectsCannotPass() {
        server.expect(requestTo(ENDPOINT)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(response("0")).contentType(MediaType.APPLICATION_JSON));
        server.expect(requestTo(ENDPOINT)).andRespond(withStatus(HttpStatus.FOUND)
            .body(response("0")).contentType(MediaType.APPLICATION_JSON));
        assertThrows(BadRequestException.class, () -> utils.iDCardVerify(NAME, ID_CARD));
        assertThrows(BadRequestException.class, () -> utils.iDCardVerify(NAME, ID_CARD));
        server.verify();
    }

    @Test
    void invalidInputsDoNotCallProvider() {
        assertThrows(BadRequestException.class, () -> utils.iDCardVerify(null, ID_CARD));
        assertThrows(BadRequestException.class, () -> utils.iDCardVerify(" ", ID_CARD));
        assertThrows(BadRequestException.class, () -> utils.iDCardVerify(NAME, null));
        assertThrows(BadRequestException.class, () -> utils.iDCardVerify(NAME, "invalid"));
        server.verify();
    }

    @Test
    void invalidConfigurationFailsWithoutExposingSecrets() {
        String[] urls = {"http://identity.example.test", "https://user:secret@identity.example.test",
            "https://identity.example.test?key=secret", "https://identity.example.test#fragment", "invalid"};
        for (String url : urls) {
            IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> new ShumaiUtils(url, "test-app-id", "test-secret", new RestTemplate()));
            assertFalse(error.getMessage().contains("test-secret"));
        }
        assertThrows(IllegalStateException.class,
            () -> new ShumaiUtils("https://identity.example.test", "", "test-secret", new RestTemplate()));
    }

    private static String response(String result) {
        return "{\"success\":true,\"code\":200,\"data\":{\"result\":" + result + "}}";
    }
}
