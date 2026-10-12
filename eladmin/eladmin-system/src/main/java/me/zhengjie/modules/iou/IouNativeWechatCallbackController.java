package me.zhengjie.modules.iou;

import lombok.RequiredArgsConstructor;
import me.zhengjie.annotation.rest.*;
import me.zhengjie.iou.service.IouNativeWechatPaymentService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.net.URI;

@RestController @RequiredArgsConstructor
public class IouNativeWechatCallbackController {
    private final IouNativeWechatPaymentService payments;
    @AnonymousGetMapping("/api/iou/payment/wechat/callback")
    public ResponseEntity<Void> oauth(@RequestParam String code,@RequestParam String state){
        return ResponseEntity.status(302).header("Cache-Control","no-store").header("Referrer-Policy","no-referrer").location(URI.create(payments.oauthCallback(code,state))).build();
    }
    @AnonymousPostMapping("/api/iou/payment/wechat/notify")
    public ResponseEntity<Void> notify(@RequestHeader HttpHeaders headers,@RequestBody String raw){payments.notify(headers,raw);return ResponseEntity.noContent().build();}
}
