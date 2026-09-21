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
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

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
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        encrypt(target, visited);
    }

    private void encrypt(Object target, Set<Object> visited) {
        if (target == null || isTerminalClass(target.getClass()) || !visited.add(target)) {
            return;
        }
        Class<?> clazz = target.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                continue;
            }
            field.setAccessible(true);
            try {
                Object value = field.get(target);
                if (field.isAnnotationPresent(EncryptField.class)) {
                    if (value instanceof String && StringUtils.isNotBlank((String) value)) {
                        field.set(target, dataSecurityUtil.encrypt((String) value));
                    }
                } else if (value != null && !isTerminalClass(value.getClass())) {
                    // 递归处理内部对象
                    encrypt(value, visited);
                }
            } catch (Exception e) {
                throw new RuntimeException("加密字段失败: " + field.getName(), e);
            }
        }
    }

    private boolean isJdkClass(Class<?> clazz) {
        return clazz.getClassLoader() == null;
    }

    private boolean isTerminalClass(Class<?> clazz) {
        return clazz.isPrimitive() || clazz.isEnum() || isJdkClass(clazz);
    }

}
