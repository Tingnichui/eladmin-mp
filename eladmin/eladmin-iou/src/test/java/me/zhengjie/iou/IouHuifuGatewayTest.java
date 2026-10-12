package me.zhengjie.iou;

import me.zhengjie.iou.domain.*;
import me.zhengjie.iou.service.*;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import java.math.BigDecimal;
import java.security.*;
import java.sql.Timestamp;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class IouHuifuGatewayTest {
    IouPaymentOptions options;IouHuifuGateway gateway;MockRestServiceServer server;
    @BeforeEach void setup() throws Exception {
        KeyPairGenerator generator=KeyPairGenerator.getInstance("RSA");generator.initialize(2048);KeyPair keys=generator.generateKeyPair();
        options=new IouPaymentOptions();options.setEnabled(true);options.setSysId("system");options.setProductId("product");options.setHuifuId("merchant");
        options.setPrivateKey(Base64.getEncoder().encodeToString(keys.getPrivate().getEncoded()));options.setPublicKey(Base64.getEncoder().encodeToString(keys.getPublic().getEncoded()));
        options.setAppId("wxsynthetic");options.setAppSecret("synthetic-secret");options.setNotifyUrl("https://example.test/api/iou/payment/huifu/notify");
        options.setOauthCallbackUrl("https://example.test/api/iou/payment/wechat/callback");options.setH5Origin("https://example.test");
        RestTemplate http=new RestTemplate();server=MockRestServiceServer.bindTo(http).build();gateway=new IouHuifuGateway(options,http);
    }
    @Test void configurationIsClosedUntilEveryCredentialAndHttpsUrlIsReady(){assertTrue(options.ready());options.setAppSecret("");assertFalse(options.ready());assertEquals("IouPaymentOptions",options.toString());}
    @Test void signatureChecksRawAsyncBodyWithoutResorting(){String raw="{\"z\":1,\"a\":2}";String sign=gateway.sign(raw);assertTrue(gateway.verify(raw,sign));assertFalse(gateway.verify("{\"a\":2,\"z\":1}",sign));assertThrows(IouBusinessException.class,()->gateway.notification(raw,"bad"));}
    @Test void createUsesV4WechatFieldsUtf8AndVerifiedResponse() {
        IouFeeOrder order=new IouFeeOrder();order.setAmount(new BigDecimal("10.00"));order.setPayerOpenid("synthetic-openid");
        IouPaymentAttempt attempt=new IouPaymentAttempt();attempt.setReqDate("20261012");attempt.setReqSeqId("synthetic-sequence");attempt.setHuifuId("merchant");attempt.setExpiresAt(Timestamp.valueOf("2026-10-12 01:10:00"));
        Map<String,Object> body=new TreeMap<>();body.put("huifu_id","merchant");body.put("req_seq_id","synthetic-sequence");body.put("req_date","20261012");body.put("resp_code","00000100");body.put("trans_stat","P");
        Map<String,Object> response=new TreeMap<>();response.put("data",body);response.put("sign",gateway.sign(gateway.encode(body)));
        server.expect(requestTo("https://api.huifu.com/v4/trade/payment/create")).andExpect(method(HttpMethod.POST))
            .andExpect(header("jpt-x-skill-source","eladmin-iou")).andExpect(content().string(org.hamcrest.Matchers.containsString("\\\"sub_openid\\\":\\\"synthetic-openid\\\"")))
            .andRespond(withSuccess(gateway.encode(response),MediaType.APPLICATION_JSON));
        assertEquals("P",gateway.create(order,attempt).get("trans_stat"));server.verify();
    }
    @Test void invalidSyncSignatureDoesNotProduceTrustedPaymentResult() {
        IouPaymentAttempt attempt=new IouPaymentAttempt();attempt.setHuifuId("merchant");attempt.setReqDate("20261012");attempt.setReqSeqId("req");
        server.expect(requestTo("https://api.huifu.com/v4/trade/payment/scanpay/query")).andRespond(withSuccess("{\"data\":{\"trans_stat\":\"S\"},\"sign\":\"bad\"}",MediaType.APPLICATION_JSON));
        assertThrows(IouBusinessException.class,()->gateway.query(attempt));server.verify();
    }
}
