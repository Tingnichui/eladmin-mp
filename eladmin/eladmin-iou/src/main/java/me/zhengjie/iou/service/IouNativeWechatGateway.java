package me.zhengjie.iou.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.*;
import java.time.*;
import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.spec.*;

/** WeChat APIv3 ordinary merchant + WeChat Pay public key mode. */
@Service
public class IouNativeWechatGateway {
    private final IouNativeWechatOptions options;
    private final RestTemplate http;
    private final ObjectMapper json=new ObjectMapper();
    @Autowired public IouNativeWechatGateway(IouNativeWechatOptions options){this(options,client());}
    public IouNativeWechatGateway(IouNativeWechatOptions options,RestTemplate http){this.options=options;this.http=http;}
    private static RestTemplate client(){SimpleClientHttpRequestFactory f=new SimpleClientHttpRequestFactory();f.setConnectTimeout(3000);f.setReadTimeout(5000);return new NativePaymentHttpClient(f);}
    public String prepay(String orderNo,long amount,String openid,String expiresAt){
        Map<String,Object> response=request(HttpMethod.POST,"/v3/pay/transactions/jsapi",map("appid",options.getAppId(),"mchid",options.getMerchantId(),
            "description","电子借条平台服务费","out_trade_no",orderNo,"notify_url",options.getNotifyUrl(),"time_expire",expiresAt,
            "amount",map("total",amount,"currency","CNY"),"payer",map("openid",openid)));
        String id=(String)response.get("prepay_id");if(id==null||!id.matches("[a-zA-Z0-9_-]{8,128}"))throw unavailable();return id;
    }
    public Map<String,Object> query(String orderNo){return request(HttpMethod.GET,"/v3/pay/transactions/out-trade-no/"+orderNo+"?mchid="+options.getMerchantId(),null);}
    public void close(String orderNo){request(HttpMethod.POST,"/v3/pay/transactions/out-trade-no/"+orderNo+"/close",map("mchid",options.getMerchantId()));}
    public Map<String,Object> payParams(String prepayId){
        String time=String.valueOf(Instant.now().getEpochSecond()),nonce=random(),pkg="prepay_id="+prepayId;
        return map("appId",options.getAppId(),"timeStamp",time,"nonceStr",nonce,"package",pkg,"signType","RSA","paySign",sign(options.getAppId()+"\n"+time+"\n"+nonce+"\n"+pkg+"\n"));
    }
    private Map<String,Object> request(HttpMethod method,String path,Map<String,Object> body){
        options.requireReady();String raw=body==null?"":encode(body),time=String.valueOf(Instant.now().getEpochSecond()),nonce=random();
        HttpHeaders headers=new HttpHeaders();headers.setContentType(MediaType.parseMediaType("application/json;charset=UTF-8"));headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("Wechatpay-Serial",options.getWechatpayPublicKeyId());
        headers.set("Authorization","WECHATPAY2-SHA256-RSA2048 mchid=\""+options.getMerchantId()+"\",nonce_str=\""+nonce+"\",timestamp=\""+time+"\",serial_no=\""+options.getMerchantSerialNo()+"\",signature=\""+sign(method.name()+"\n"+path+"\n"+time+"\n"+nonce+"\n"+raw+"\n")+"\"");
        try{
            ResponseEntity<String> response=http.exchange("https://api.mch.weixin.qq.com"+path,method,new HttpEntity<>(raw,headers),String.class);
            String responseBody=response.getBody()==null?"":response.getBody();verify(response.getHeaders(),responseBody);
            return responseBody.isEmpty()?Collections.emptyMap():decode(responseBody);
        }catch(HttpStatusCodeException e){
            // Error bodies need exactly the same signature verification as successful responses.
            verify(e.getResponseHeaders(),e.getResponseBodyAsString());Map<String,Object> error=decode(e.getResponseBodyAsString());
            String code=String.valueOf(error.get("code"));
            if(Arrays.asList("ORDER_NOT_EXIST","ORDER_CLOSED","ORDERPAID","OUT_TRADE_NO_USED").contains(code))throw new IouBusinessException(409,code,"微信订单状态需重新查询");
            throw unavailable();
        }catch(IouBusinessException e){throw e;}catch(Exception ignored){throw unavailable();}
    }
    public Map<String,Object> notification(HttpHeaders headers,String raw){
        if(raw==null||raw.length()>1048576)throw unavailable();verify(headers,raw);Map<String,Object> envelope=decode(raw);
        if(!"TRANSACTION.SUCCESS".equals(envelope.get("event_type"))||!(envelope.get("resource") instanceof Map))throw unavailable();
        Map<String,Object> resource=(Map<String,Object>)envelope.get("resource");
        if(!"AEAD_AES_256_GCM".equals(resource.get("algorithm")))throw unavailable();
        try{
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,new SecretKeySpec(options.getApiV3Key().getBytes(StandardCharsets.UTF_8),"AES"),new GCMParameterSpec(128,((String)resource.get("nonce")).getBytes(StandardCharsets.UTF_8)));
            Object aad=resource.get("associated_data");if(aad!=null)cipher.updateAAD(aad.toString().getBytes(StandardCharsets.UTF_8));
            return decode(new String(cipher.doFinal(Base64.getDecoder().decode((String)resource.get("ciphertext"))),StandardCharsets.UTF_8));
        }catch(Exception ignored){throw unavailable();}
    }
    public void verify(HttpHeaders headers,String raw){
        try{
            if(headers==null||!options.getWechatpayPublicKeyId().equals(headers.getFirst("Wechatpay-Serial")))throw unavailable();
            String time=headers.getFirst("Wechatpay-Timestamp"),nonce=headers.getFirst("Wechatpay-Nonce"),signed=headers.getFirst("Wechatpay-Signature");
            if(time==null||nonce==null||signed==null||Math.abs(Instant.now().getEpochSecond()-Long.parseLong(time))>300)throw unavailable();
            Signature signature=Signature.getInstance("SHA256withRSA");signature.initVerify(KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(IouPaymentOptions.keyBytes(options.getWechatpayPublicKey()))));
            signature.update((time+"\n"+nonce+"\n"+raw+"\n").getBytes(StandardCharsets.UTF_8));if(!signature.verify(Base64.getDecoder().decode(signed)))throw unavailable();
        }catch(Exception ignored){throw new IouBusinessException(502,"WECHAT_SIGNATURE_INVALID","微信支付结果验签失败");}
    }
    public String sign(String value){try{Signature signature=Signature.getInstance("SHA256withRSA");signature.initSign(KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(IouPaymentOptions.keyBytes(options.getPrivateKey()))));signature.update(value.getBytes(StandardCharsets.UTF_8));return Base64.getEncoder().encodeToString(signature.sign());}catch(Exception ignored){throw unavailable();}}
    public String oauthOpenid(String code){
        options.requireReady();try{Map<?,?> response=http.getForObject("https://api.weixin.qq.com/sns/oauth2/access_token?appid={appid}&secret={secret}&code={code}&grant_type=authorization_code",Map.class,options.getAppId(),options.getAppSecret(),code);
            Object id=response==null?null:response.get("openid");if(id==null||response.containsKey("errcode")||!id.toString().matches("[a-zA-Z0-9_-]{10,128}"))throw unavailable();return id.toString();
        }catch(Exception ignored){throw new IouBusinessException(502,"WECHAT_AUTH_FAILED","微信授权失败，请返回页面重新授权");}
    }
    public Map<String,Object> decode(String raw){try{return json.readValue(raw,new TypeReference<Map<String,Object>>(){});}catch(Exception ignored){throw unavailable();}}
    public String encode(Object value){try{return json.writeValueAsString(value);}catch(Exception ignored){throw unavailable();}}
    static Map<String,Object> map(Object... args){Map<String,Object> result=new LinkedHashMap<>();for(int i=0;i<args.length;i+=2)result.put((String)args[i],args[i+1]);return result;}
    static String random(){return UUID.randomUUID().toString().replace("-","");}
    private static IouBusinessException unavailable(){return new IouBusinessException(502,"PAYMENT_RESULT_UNKNOWN","微信支付结果暂未确认，请查询原单，勿重复付款");}
    private static class NativePaymentHttpClient extends RestTemplate{NativePaymentHttpClient(SimpleClientHttpRequestFactory f){super(f);}}
}
