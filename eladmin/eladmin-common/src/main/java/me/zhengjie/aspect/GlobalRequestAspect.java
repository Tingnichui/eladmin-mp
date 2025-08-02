package me.zhengjie.aspect;

import me.zhengjie.annotation.EncryptField;
import me.zhengjie.utils.DataSecurityUtil;
import me.zhengjie.utils.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.reflect.Field;

@Aspect
@Component
public class GlobalRequestAspect {

    @Resource
    private DataSecurityUtil dataSecurityUtil;

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
                        field.set(target, dataSecurityUtil.encrypt((String) value));
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