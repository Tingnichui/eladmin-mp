package me.zhengjie.modules.iou;

import me.zhengjie.annotation.rest.AnonymousGetMapping;
import me.zhengjie.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.util.StreamUtils;
import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** 同一份正文供未登录的注册页和公网协议地址使用，不接受任意资源路径。 */
@RestController
public class IouPolicyController {
    private final String version;
    public IouPolicyController(@Value("${iou.registration.agreement-version:}") String version) {this.version=version;}

    private String read(String kind) throws IOException {
        if (!"agreement".equals(kind) && !"privacy".equals(kind) && !"identity".equals(kind)) {
            throw new BadRequestException("协议不存在");
        }
        try (InputStream stream=new ClassPathResource("iou-policies/"+kind+".txt").getInputStream()) {
            return StreamUtils.copyToString(stream,StandardCharsets.UTF_8);
        }
    }

    @AnonymousGetMapping("/api/iou/auth/policies")
    public Map<String,String> policies() throws IOException {
        Map<String,String> result=new LinkedHashMap<>();
        result.put("version",version);
        result.put("agreement",read("agreement")); result.put("privacy",read("privacy"));result.put("identity",read("identity"));
        return result;
    }

    @AnonymousGetMapping("/api/iou/auth/policies/{kind}")
    public ResponseEntity<String> policy(@PathVariable String kind) throws IOException {
        return ResponseEntity.ok().header("Content-Type","text/plain;charset=UTF-8")
            .header("X-Content-Type-Options","nosniff").body("版本："+version+"\n\n"+read(kind));
    }
}
