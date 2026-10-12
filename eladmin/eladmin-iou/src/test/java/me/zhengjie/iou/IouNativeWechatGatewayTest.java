package me.zhengjie.iou;

import me.zhengjie.iou.service.*;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.spec.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class IouNativeWechatGatewayTest {
    IouNativeWechatOptions options;IouNativeWechatGateway gateway;MockRestServiceServer server;KeyPair keys;
    @BeforeEach void setup() throws Exception {
        KeyPairGenerator generator=KeyPairGenerator.getInstance("RSA");generator.initialize(2048);keys=generator.generateKeyPair();
        options=new IouNativeWechatOptions();options.setEnabled(true);options.setAppId("wxsynthetic");options.setAppSecret("synthetic-secret");options.setMerchantId("1234567890");options.setMerchantSerialNo("ABC123");
        options.setPrivateKey(Base64.getEncoder().encodeToString(keys.getPrivate().getEncoded()));options.setWechatpayPublicKey(Base64.getEncoder().encodeToString(keys.getPublic().getEncoded()));options.setWechatpayPublicKeyId("PUB_KEY_ID_SYNTHETIC");
        options.setApiV3Key("12345678901234567890123456789012");options.setNotifyUrl("https://example.test/api/iou/payment/wechat/notify");options.setOauthCallbackUrl("https://example.test/api/iou/payment/wechat/callback");options.setH5Origin("https://example.test");
        RestTemplate http=new RestTemplate();server=MockRestServiceServer.bindTo(http).build();gateway=new IouNativeWechatGateway(options,http);
    }
    HttpHeaders signedHeaders(String raw){HttpHeaders h=new HttpHeaders();String time=String.valueOf(Instant.now().getEpochSecond());h.set("Wechatpay-Timestamp",time);h.set("Wechatpay-Nonce","synthetic-nonce");h.set("Wechatpay-Serial",options.getWechatpayPublicKeyId());h.set("Wechatpay-Signature",gateway.sign(time+"\nsynthetic-nonce\n"+raw+"\n"));return h;}
    @Test void defaultsFailClosedAndDoNotPrintCredentials(){assertTrue(options.ready());assertEquals("IouNativeWechatOptions",options.toString());options.setApiV3Key("");assertFalse(options.ready());}
    @Test void prepayUsesV3CentsAndVerifiedResponse(){String raw="{\"prepay_id\":\"wxsyntheticprepay123\"}";
        server.expect(requestTo("https://api.mch.weixin.qq.com/v3/pay/transactions/jsapi")).andExpect(method(HttpMethod.POST))
            .andExpect(header("Wechatpay-Serial",options.getWechatpayPublicKeyId())).andExpect(header("Authorization",org.hamcrest.Matchers.startsWith("WECHATPAY2-SHA256-RSA2048 ")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"total\":25000")))
            .andRespond(withSuccess(raw,MediaType.APPLICATION_JSON).headers(signedHeaders(raw)));
        assertEquals("wxsyntheticprepay123",gateway.prepay("WXsynthetic",25000,"synthetic-openid","2026-10-12T17:30:00+08:00"));server.verify();
    }
    @Test void nativePayParamsHaveAValidIndependentSignature() throws Exception {
        Map<String,Object> params=gateway.payParams("wxsyntheticprepay123");Signature verify=Signature.getInstance("SHA256withRSA");verify.initVerify(keys.getPublic());
        verify.update((params.get("appId")+"\n"+params.get("timeStamp")+"\n"+params.get("nonceStr")+"\n"+params.get("package")+"\n").getBytes(StandardCharsets.UTF_8));
        assertTrue(verify.verify(Base64.getDecoder().decode(params.get("paySign").toString())));
    }
    @Test void invalidOrExpiredSignatureAndWrongKeyIdAreRejected(){String raw="{}";final HttpHeaders headers=signedHeaders(raw);assertDoesNotThrow(()->gateway.verify(headers,raw));
        assertThrows(IouBusinessException.class,()->gateway.verify(headers,"{\"changed\":true}"));
        headers.set("Wechatpay-Serial","PUB_KEY_ID_OTHER");assertThrows(IouBusinessException.class,()->gateway.verify(headers,raw));
        final HttpHeaders expired=signedHeaders(raw);expired.set("Wechatpay-Timestamp","1");assertThrows(IouBusinessException.class,()->gateway.verify(expired,raw));
    }
    @Test void notificationIsVerifiedBeforeAesGcmDecryption() throws Exception {
        String transaction="{\"out_trade_no\":\"WXsynthetic\",\"trade_state\":\"SUCCESS\"}",nonce="123456789012",aad="transaction";
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(options.getApiV3Key().getBytes(StandardCharsets.UTF_8),"AES"),new GCMParameterSpec(128,nonce.getBytes(StandardCharsets.UTF_8)));cipher.updateAAD(aad.getBytes(StandardCharsets.UTF_8));
        String ciphertext=Base64.getEncoder().encodeToString(cipher.doFinal(transaction.getBytes(StandardCharsets.UTF_8)));
        Map<String,Object> resource=new HashMap<>();resource.put("algorithm","AEAD_AES_256_GCM");resource.put("nonce",nonce);resource.put("associated_data",aad);resource.put("ciphertext",ciphertext);
        Map<String,Object> envelope=new HashMap<>();envelope.put("event_type","TRANSACTION.SUCCESS");envelope.put("resource",resource);String raw=gateway.encode(envelope);
        assertEquals("SUCCESS",gateway.notification(signedHeaders(raw),raw).get("trade_state"));
        assertThrows(IouBusinessException.class,()->gateway.notification(signedHeaders(raw),raw+" "));
        resource.put("ciphertext",Base64.getEncoder().encodeToString(new byte[32]));String broken=gateway.encode(envelope);assertThrows(IouBusinessException.class,()->gateway.notification(signedHeaders(broken),broken));
    }
    @Test void unsignedHttpErrorDoesNotBecomeTrustedOrderNotExist(){server.expect(requestTo("https://api.mch.weixin.qq.com/v3/pay/transactions/out-trade-no/WXsynthetic?mchid=1234567890"))
        .andRespond(withStatus(HttpStatus.NOT_FOUND).body("{\"code\":\"ORDER_NOT_EXIST\"}").contentType(MediaType.APPLICATION_JSON));
        IouBusinessException error=assertThrows(IouBusinessException.class,()->gateway.query("WXsynthetic"));assertEquals("WECHAT_SIGNATURE_INVALID",error.getCode());server.verify();
    }
}
