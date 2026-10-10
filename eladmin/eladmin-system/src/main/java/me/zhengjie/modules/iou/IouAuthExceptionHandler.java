package me.zhengjie.modules.iou;

import me.zhengjie.exception.handler.ApiError;
import me.zhengjie.modules.security.rest.AuthController;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 框架默认校验异常会打印 rejectedValue；身份入口禁止将真实资料写入异常日志。 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {IouAuthController.class, AuthController.class,
    me.zhengjie.modules.security.rest.CaptchaVerificationController.class})
public class IouAuthExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException exception) {
        return ResponseEntity.badRequest().body(ApiError.error("请求参数不合法，请检查必填项、格式和协议确认"));
    }
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> unreadable() {
        return ResponseEntity.badRequest().body(ApiError.error("请求内容格式不正确"));
    }
}
