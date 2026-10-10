package me.zhengjie.modules.iou;

import lombok.RequiredArgsConstructor;
import me.zhengjie.utils.SecurityUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/iou/app")
@PreAuthorize("@iouAccessPolicy.canAccessH5(authentication)")
public class IouAppQueryController {
    private final IouAppQueryService queries;

    @GetMapping("/summary")
    public Map<String, Object> summary() { return queries.summary(SecurityUtils.getCurrentUserId()); }

    @GetMapping("/notes")
    public Map<String, Object> notes(@RequestParam(defaultValue = "all") String role,
                                   @RequestParam(required = false) Integer status,
                                   @RequestParam(defaultValue = "false") boolean pending,
                                   @RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "20") int size) {
        return queries.list(SecurityUtils.getCurrentUserId(), role, status, pending, page, size);
    }

    @GetMapping("/notes/{id}")
    public Map<String, Object> detail(@PathVariable long id) { return queries.detail(SecurityUtils.getCurrentUserId(), id); }
}
