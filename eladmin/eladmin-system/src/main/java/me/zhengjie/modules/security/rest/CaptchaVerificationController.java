package me.zhengjie.modules.security.rest;

import lombok.Getter;
import lombok.Setter;
import lombok.RequiredArgsConstructor;
import me.zhengjie.annotation.rest.AnonymousPostMapping;
import me.zhengjie.modules.iou.IouRequestGuard;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.validation.annotation.Validated;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.Map;

/** 公共验证码校验入口，不执行业务注册、登录或付费请求。 */
@RestController
@RequiredArgsConstructor
public class CaptchaVerificationController {
    private final IouRequestGuard guard;

    @AnonymousPostMapping("/api/security/captcha/verify")
    public Map<String,String> verify(@Validated @RequestBody Challenge request) {
        return guard.verifyImage(request.getUuid(),request.getCode());
    }

    @Getter @Setter
    public static class Challenge {
        @NotBlank @Size(max=100) private String uuid;
        @NotBlank @Size(max=16) private String code;
    }
}
