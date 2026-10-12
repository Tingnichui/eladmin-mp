package me.zhengjie.modules.iou;

import lombok.RequiredArgsConstructor;
import me.zhengjie.annotation.rest.*;
import me.zhengjie.iou.service.IouFeePaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import java.net.URI;

/** Anonymous provider endpoints; authorization is a one-time state or a verified RSA signature. */
@RestController
@RequiredArgsConstructor
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(prefix="iou.payment",name="huifu-legacy-enabled",havingValue="true")
public class IouPaymentCallbackController {
    private final IouFeePaymentService payments;
    @AnonymousGetMapping("/api/iou/payment/huifu/wechat/callback")
    public ResponseEntity<Void> oauth(@RequestParam String code,@RequestParam String state) {
        return ResponseEntity.status(302).header("Cache-Control","no-store").header("Referrer-Policy","no-referrer")
            .location(URI.create(payments.oauthCallback(code,state))).build();
    }
    @AnonymousPostMapping("/api/iou/payment/huifu/notify")
    public ResponseEntity<String> notify(@RequestParam("resp_data") String data,@RequestParam String sign) {
        return ResponseEntity.ok().header("Cache-Control","no-store").header("Content-Type","text/plain;charset=UTF-8").body(payments.notify(data,sign));
    }
}
