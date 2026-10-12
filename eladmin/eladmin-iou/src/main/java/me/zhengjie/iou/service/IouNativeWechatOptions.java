package me.zhengjie.iou.service;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.spec.*;

@Data @Component
@ConfigurationProperties(prefix="iou.wechat-payment")
public class IouNativeWechatOptions {
    private boolean enabled;
    private String appId="";
    private String appSecret="";
    private String merchantId="";
    private String merchantSerialNo="";
    private String privateKey="";
    private String apiV3Key="";
    private String wechatpayPublicKey="";
    private String wechatpayPublicKeyId="";
    private String notifyUrl="";
    private String oauthCallbackUrl="";
    private String h5Origin="";
    private int expireMinutes=30;
    public boolean ready(){
        try{
            if(!enabled||!appId.matches("wx[a-zA-Z0-9]+")||IouPaymentOptions.blank(appSecret)||!merchantId.matches("[0-9]{6,32}")
                ||!merchantSerialNo.matches("[a-fA-F0-9]+")||!wechatpayPublicKeyId.startsWith("PUB_KEY_ID_")
                ||apiV3Key.getBytes(StandardCharsets.UTF_8).length!=32||expireMinutes<2||expireMinutes>120)return false;
            KeyFactory factory=KeyFactory.getInstance("RSA");factory.generatePrivate(new PKCS8EncodedKeySpec(IouPaymentOptions.keyBytes(privateKey)));
            factory.generatePublic(new X509EncodedKeySpec(IouPaymentOptions.keyBytes(wechatpayPublicKey)));
            return https(notifyUrl,false)&&https(oauthCallbackUrl,false)&&https(h5Origin,true);
        }catch(Exception ignored){return false;}
    }
    private boolean https(String value,boolean origin){URI uri=URI.create(value);return "https".equals(uri.getScheme())&&uri.getHost()!=null&&uri.getRawUserInfo()==null&&uri.getRawQuery()==null&&uri.getRawFragment()==null&&(!origin||uri.getRawPath().isEmpty()||"/".equals(uri.getRawPath()));}
    public void requireReady(){if(!ready())throw new IouBusinessException(503,"PAYMENT_NOT_READY","微信支付尚未配置完成");}
    @Override public String toString(){return "IouNativeWechatOptions";}
}
