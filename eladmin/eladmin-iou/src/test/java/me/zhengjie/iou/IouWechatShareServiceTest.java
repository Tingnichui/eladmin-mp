package me.zhengjie.iou;

import me.zhengjie.iou.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class IouWechatShareServiceTest {
    private IouWechatOptions options() {
        IouWechatOptions options = new IouWechatOptions();
        options.setEnabled(true);
        options.setAppId("synthetic-app");
        options.setAppSecret("synthetic-secret");
        options.setPublicOrigin("https://example.com");
        return options;
    }
    @Test void disabledDoesNotCallWechat() {
        IouWechatOptions options = options();
        options.setEnabled(false);
        RestTemplate http = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(http);
        assertEquals(false, new IouWechatShareService(options, http).configure("http://192.168.10.10:9000/", "123.test").get("enabled"));
        server.verify();
    }
    @Test void signatureMatchesPublishedExample() {
        assertEquals("0f9de62fce790f9a083d5c99e95740ceb90c27ed", IouWechatShareService.signature(
            "sM4AOVdWfPE4DxkXGEs8VMCPGGVi4C3VM0P37wVUCFvkVAy_90u5h9nbSlYy3-Sl-HhTdfl2fzFy1AOcHKP7qg",
            "Wm3WZYTPz0wzccnW", 1414587457L, "http://mp.weixin.qq.com?params=value"));
    }
    @Test void rejectsUnboundOriginsFragmentsAndCredentialsBeforeNetwork() {
        RestTemplate http = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(http);
        IouWechatShareService service = new IouWechatShareService(options(), http);
        for (String url : new String[]{"https://evil.test/", "https://example.com.evil.test/", "http://example.com/", "https://example.com:444/", "https://user@example.com/", "https://example.com/#/notes"}) {
            assertThrows(IouBusinessException.class, () -> service.configure(url, "123.test"));
        }
        server.verify();
    }
    @Test void cachesTicketAndReturnsOnlyPublicCardData() {
        RestTemplate http = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(http);
        server.expect(requestTo("https://api.weixin.qq.com/cgi-bin/stable_token"))
            .andExpect(content().json("{\"grant_type\":\"client_credential\",\"appid\":\"synthetic-app\",\"secret\":\"synthetic-secret\",\"force_refresh\":false}"))
            .andRespond(withSuccess("{\"access_token\":\"test-token\",\"expires_in\":7200}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.weixin.qq.com/cgi-bin/ticket/getticket?access_token=test-token&type=jsapi"))
            .andRespond(withSuccess("{\"errcode\":0,\"ticket\":\"test-ticket\",\"expires_in\":7200}", MediaType.APPLICATION_JSON));
        IouWechatShareService service = new IouWechatShareService(options(), http);
        Map<String, Object> result = service.configure("https://example.com/?a=1&b=2", "123.test");
        assertEquals(true, result.get("enabled"));
        assertEquals("https://example.com/#/notes/join?reference=123.test", result.get("link"));
        assertEquals(IouWechatShareService.signature("test-ticket", (String)result.get("nonceStr"), (Long)result.get("timestamp"), "https://example.com/?a=1&b=2"), result.get("signature"));
        assertFalse(result.toString().contains("synthetic-secret"));
        assertFalse(result.toString().contains("test-ticket"));
        service.configure("https://example.com/", "456.test");
        server.verify();
    }
    @Test void upstreamFailureIsRedactedAndBackedOff() {
        RestTemplate http = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(http);
        server.expect(requestTo("https://api.weixin.qq.com/cgi-bin/stable_token"))
            .andRespond(withSuccess("{\"errcode\":40013,\"errmsg\":\"synthetic-secret\"}", MediaType.APPLICATION_JSON));
        IouWechatShareService service = new IouWechatShareService(options(), http);
        for (int i = 0; i < 2; i++) {
            IouBusinessException error = assertThrows(IouBusinessException.class, () -> service.configure("https://example.com/", "123.test"));
            assertEquals(503, error.getStatus());
            assertFalse(error.getMessage().contains("synthetic-secret"));
        }
        server.verify();
    }
}
