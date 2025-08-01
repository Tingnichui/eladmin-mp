package me.zhengjie.aspect;

import me.zhengjie.annotation.DecryptField;
import me.zhengjie.annotation.EncryptField;
import me.zhengjie.utils.PageResult;
import me.zhengjie.utils.RsaUtils;
import me.zhengjie.utils.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.lang.reflect.Field;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Collection;

@Aspect
@Component
public class DecryptFieldAspect {

    private static final Logger log = LoggerFactory.getLogger(DecryptFieldAspect.class);
    @Value("${data.rsa.private_key}")
    private String rsaPrivateKeyStr;

    private PrivateKey rsaPrivateKey;

    @PostConstruct
    public void initKey() throws Exception {
        rsaPrivateKey = RsaUtils.getPrivateKey(rsaPrivateKeyStr);
    }

    @AfterReturning(pointcut = "execution(* me.zhengjie.*.rest..*(..))", returning = "result")
    public void afterControllerReturn(JoinPoint joinPoint, Object result) {
        if (result == null) return;

        // 处理返回值
        if (result instanceof ResponseEntity) {
            Object body = ((ResponseEntity<?>) result).getBody();
            decrypt(body);
        } else {
            decrypt(result);
        }
    }

    private void decrypt(Object target) {
        if (target == null) return;

        // 处理列表
        if (target instanceof Collection<?>) {
            for (Object item : (Collection<?>) target) {
                decrypt(item);
            }
            return;
        }

        // 处理分页结果 PageResult<T>
        if (target instanceof PageResult<?>) {
            decrypt(((PageResult<?>) target).getContent());
            return;
        }

        // 跳过 JDK 类型
        if (isJdkClass(target.getClass())) return;

        for (Field field : target.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(DecryptField.class)) {
                field.setAccessible(true);
                try {
                    Object value = field.get(target);
                    if (value instanceof String && StringUtils.isNotBlank((String) value)) {
                        String decrypted = RsaUtils.decryptByPrivateKey(rsaPrivateKey, (String) value);
                        field.set(target, decrypted);
                    }
                } catch (Exception e) {
                    log.error("解密字段失败: " + field.getName(), e);
                }
            }
        }
    }

    private boolean isJdkClass(Class<?> clazz) {
        return clazz != null && clazz.getClassLoader() == null;
    }

}