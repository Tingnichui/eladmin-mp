package me.zhengjie.modules.iou;

import lombok.RequiredArgsConstructor;
import me.zhengjie.annotation.rest.AnonymousGetMapping;
import me.zhengjie.annotation.rest.AnonymousPostMapping;
import me.zhengjie.utils.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class IouAuthController {
    private final IouAccountService accounts;
    private final IouRequestGuard guard;
    @AnonymousGetMapping("/api/iou/auth/options")
    public Map<String,Object> options() {return accounts.options();}
    @AnonymousPostMapping("/api/iou/auth/register")
    public ResponseEntity<Map<String,Object>> register(@Validated @RequestBody IouRegisterRequest request, HttpServletRequest http) {
        // 不采信客户端可伪造的 X-Forwarded-For；可信反向代理另行配置 RemoteIpValve。
        accounts.register(request,http.getRemoteAddr());
        Map<String,Object> result=new java.util.LinkedHashMap<>();
        try {
            // 注册验证码已消费。建账号提交后签发一次性凭证，继续复用原登录及密码校验流程。
            result.put("loginCaptcha",guard.issuePass());
        } catch (RuntimeException e) {
            // 账号已创建，不把登录凭证签发故障报告成注册失败，避免用户重复收费注册。
            result.put("loginCaptcha",null);
        }
        return ResponseEntity.status(201).body(result);
    }
    @GetMapping("/api/iou/app/me")
    @PreAuthorize("@el.check('iou:profile:read')")
    public Map<String,Object> me() {return accounts.profile(SecurityUtils.getCurrentUserId());}
}
