package me.zhengjie.modules.iou;

import lombok.RequiredArgsConstructor;
import me.zhengjie.iou.service.IouNativeWechatPaymentService;
import me.zhengjie.utils.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/iou/app")
@PreAuthorize("@iouAccessPolicy.canAccessH5(authentication)")
public class IouFeePaymentController {
    private final IouNativeWechatPaymentService payments;
    @PostMapping("/notes/{id}/fee-orders") public ResponseEntity<Map<String,Object>> create(@PathVariable long id) {
        return response(payments.create(SecurityUtils.getCurrentUserId(),id));
    }
    @GetMapping("/fee-orders/{orderNo}") public ResponseEntity<Map<String,Object>> detail(@PathVariable String orderNo) {
        return response(payments.detail(SecurityUtils.getCurrentUserId(),orderNo));
    }
    @PostMapping("/fee-orders/{orderNo}/wechat-authorization") public ResponseEntity<Map<String,Object>> authorization(@PathVariable String orderNo) {
        return response(payments.authorize(SecurityUtils.getCurrentUserId(),orderNo));
    }
    @PostMapping("/fee-orders/{orderNo}/payment") public ResponseEntity<Map<String,Object>> payment(@PathVariable String orderNo) {
        return response(payments.payment(SecurityUtils.getCurrentUserId(),orderNo));
    }
    private ResponseEntity<Map<String,Object>> response(Map<String,Object> body){return ResponseEntity.ok().header("Cache-Control","no-store").body(body);}
}
