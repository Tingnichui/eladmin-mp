package me.zhengjie.modules.iou;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import me.zhengjie.iou.domain.dto.IouNoteConditions;
import me.zhengjie.iou.service.*;
import me.zhengjie.utils.SecurityUtils;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/iou/app")
@PreAuthorize("@iouAccessPolicy.canAccessH5(authentication)")
public class IouNoteBusinessController {
    private final IouNoteBusinessService service;
    @GetMapping("/note-options") public Map<String,Object> options(){return service.options(SecurityUtils.getCurrentUserId());}
    @PostMapping("/notes/quote") public Map<String,Object> quote(@RequestBody IouNoteConditions body){return service.quote(SecurityUtils.getCurrentUserId(),body);}
    @PostMapping("/notes") public ResponseEntity<Map<String,Object>> create(@RequestHeader("Idempotency-Key") String key,@RequestBody IouNoteConditions body){return ResponseEntity.status(201).body(service.create(SecurityUtils.getCurrentUserId(),key,body));}
    @PutMapping("/notes/{id}") public Map<String,Object> update(@PathVariable long id,@RequestBody Edit body){return service.update(SecurityUtils.getCurrentUserId(),id,body.getVersion(),body.getConditions());}
    @DeleteMapping("/notes/{id}") public ResponseEntity<Void> delete(@PathVariable long id,@RequestParam String version){service.delete(SecurityUtils.getCurrentUserId(),id,version);return ResponseEntity.noContent().build();}
    @GetMapping("/notes/{id}/plan") public Map<String,Object> plan(@PathVariable long id){return service.plan(SecurityUtils.getCurrentUserId(),id);}
    @GetMapping("/notes/{id}/preview") public Map<String,Object> preview(@PathVariable long id){return service.preview(SecurityUtils.getCurrentUserId(),id);}
    @PostMapping("/notes/{id}/signatures") public Map<String,Object> sign(@PathVariable long id,@RequestHeader("Idempotency-Key") String key,@RequestBody Sign body,HttpServletRequest request){
        return service.sign(SecurityUtils.getCurrentUserId(),id,body.getVersion(),body.getPreviewHash(),key,body.getShareReference(),body.getSignatureImage(),Boolean.TRUE.equals(body.getConfirmed()),request.getRemoteAddr(),request.getHeader("User-Agent"));
    }
    @GetMapping("/notes/{id}/signatures/current") public Map<String,Object> current(@PathVariable long id,@RequestParam(required=false) String shareReference){return service.current(SecurityUtils.getCurrentUserId(),id,shareReference);}
    @GetMapping("/notes/{id}/share") public Map<String,Object> share(@PathVariable long id){return service.share(SecurityUtils.getCurrentUserId(),id);}
    @GetMapping("/shared-notes/{reference}") public Map<String,Object> shared(@PathVariable String reference){return service.shared(SecurityUtils.getCurrentUserId(),reference);}
    @PostMapping("/notes/{id}/cancel") public Map<String,Object> cancel(@PathVariable long id,@RequestBody Cancel body){return service.cancel(SecurityUtils.getCurrentUserId(),id,body.getVersion(),body.getReason());}
    @GetMapping("/notes/{id}/agreement") public ResponseEntity<byte[]> agreement(@PathVariable long id,@RequestParam(defaultValue="download") String disposition){
        if(!"inline".equals(disposition)&&!"download".equals(disposition))throw new IouBusinessException(400,"INVALID_CONDITIONS","文件打开方式不正确");
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/html;charset=UTF-8"))
            .header("Content-Disposition",("inline".equals(disposition)?"inline":"attachment")+"; filename=\"iou-"+id+".html\"")
            .header("Cache-Control","no-store").header("X-Content-Type-Options","nosniff")
            .header("Content-Security-Policy","default-src 'none'; img-src data:; style-src 'unsafe-inline'; base-uri 'none'; frame-ancestors 'self'")
            .body(service.agreement(SecurityUtils.getCurrentUserId(),id));
    }
    public static class StrictBody {
        @JsonAnySetter public void rejectUnknown(String key,Object value){throw new IouBusinessException(400,"INVALID_CONDITIONS","请求包含不允许的字段");}
    }
    @Data public static class Edit extends StrictBody {private String version;private IouNoteConditions conditions;}
    @Data public static class Cancel extends StrictBody {private String version;private String reason;}
    @Data public static class Sign extends StrictBody {
        private String version;
        private String previewHash;
        private Boolean confirmed;
        @ToString.Exclude private String shareReference;
        @ToString.Exclude private String signatureImage;
    }
}
