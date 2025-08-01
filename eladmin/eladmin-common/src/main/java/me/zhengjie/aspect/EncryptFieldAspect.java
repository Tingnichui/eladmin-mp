package me.zhengjie.aspect;

import me.zhengjie.annotation.EncryptField;
import me.zhengjie.utils.RsaUtils;
import me.zhengjie.utils.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.lang.reflect.Field;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;

@Aspect
@Component
public class EncryptFieldAspect {

    @Value("${data.rsa.public_key}")
    private String rsaPublicKeyStr;

    private PublicKey rsaPublicKey;

    @PostConstruct
    public void initKey() throws Exception {
        this.rsaPublicKey = RsaUtils.getPublicKey(rsaPublicKeyStr);
    }


    @Before("execution(* me.zhengjie.*.rest..*(..))")
    public void beforeController(JoinPoint joinPoint) {
        for (Object arg : joinPoint.getArgs()) {
            if (arg != null) {
                encrypt(arg);
            }
        }
    }

    public void encrypt(Object target) {
        Class<?> clazz = target.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object value = field.get(target);
                if (field.isAnnotationPresent(EncryptField.class)) {
                    if (value instanceof String && StringUtils.isNotBlank((String) value)) {
                        String encrypted = RsaUtils.encryptByPublicKey(rsaPublicKey, (String) value);
                        field.set(target, encrypted);
                    }
                } else if (value != null && !isJdkClass(value.getClass())) {
                    // 递归处理内部对象
                    encrypt(value);
                }
            } catch (Exception e) {
                throw new RuntimeException("加密字段失败: " + field.getName(), e);
            }
        }
    }

    private boolean isJdkClass(Class<?> clazz) {
        return clazz.getClassLoader() == null;
    }

}