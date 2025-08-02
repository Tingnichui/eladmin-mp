package me.zhengjie.aspect;

import lombok.extern.slf4j.Slf4j;
import me.zhengjie.annotation.DecryptField;
import me.zhengjie.annotation.MaskField;
import me.zhengjie.utils.DataSecurityUtil;
import me.zhengjie.utils.PageResult;
import me.zhengjie.utils.RsaUtils;
import me.zhengjie.utils.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.security.PrivateKey;
import java.util.Collection;

@Slf4j
@Aspect
@Component
public class GlobalResponseAspect {

    @Resource
    private DataSecurityUtil dataSecurityUtil;

    @AfterReturning(pointcut = "execution(* me.zhengjie.*.rest..*(..))", returning = "result")
    public void afterControllerReturn(JoinPoint joinPoint, Object result) {
        if (result == null) return;

        // 处理返回值
        if (result instanceof ResponseEntity) {
            Object body = ((ResponseEntity<?>) result).getBody();
            handle(body);
        } else {
            handle(result);
        }
    }

    private void handle(Object target) {
        if (target == null) return;

        // 处理列表
        if (target instanceof Collection<?>) {
            for (Object item : (Collection<?>) target) {
                handle(item);
            }
            return;
        }

        // 处理分页结果 PageResult<T>
        if (target instanceof PageResult<?>) {
            handle(((PageResult<?>) target).getContent());
            return;
        }

        // 跳过 JDK 类型
        if (isJdkClass(target.getClass())) return;

        doHandle(target);
    }

    private void doHandle(Object target) {
        for (Field field : target.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(DecryptField.class)) {
                try {
                    Object value = field.get(target);
                    if (value instanceof String && StringUtils.isNotBlank((String) value)) {
                        field.set(target, dataSecurityUtil.decrypt((String) value));
                    }
                } catch (Exception e) {
                    log.error("解密字段失败: " + field.getName(), e);
                }
            }
            if (field.isAnnotationPresent(MaskField.class)) {
                try {
                    field.set(target, null);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private boolean isJdkClass(Class<?> clazz) {
        return clazz != null && clazz.getClassLoader() == null;
    }

}