package me.zhengjie.modules.iou;

import me.zhengjie.iou.service.IouBusinessException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes={IouNoteBusinessController.class,IouAppQueryController.class})
public class IouNoteExceptionHandler {
    @ExceptionHandler(IouBusinessException.class) public ResponseEntity<Map<String,Object>> business(IouBusinessException e){return error(e.getStatus(),e.getCode(),e.getMessage());}
    @ExceptionHandler(HttpMessageNotReadableException.class) public ResponseEntity<Map<String,Object>> unreadable(){return error(400,"INVALID_CONDITIONS","请求格式不正确或包含不允许的字段");}
    private ResponseEntity<Map<String,Object>> error(int status,String code,String message){Map<String,Object>m=new LinkedHashMap<>();m.put("status",status);m.put("code",code);m.put("message",message);m.put("timestamp",OffsetDateTime.now(java.time.ZoneId.of("Asia/Shanghai")).toString());return ResponseEntity.status(status).body(m);}
}
