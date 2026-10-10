package me.zhengjie.modules.iou;

import lombok.RequiredArgsConstructor;
import me.zhengjie.modules.system.domain.User;
import me.zhengjie.modules.system.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/** 与角色权限叠加的账号边界，避免“已登录即可访问”的后台接口被 H5 账号访问。 */
@Component("iouAccessPolicy")
@RequiredArgsConstructor
public class IouAccessPolicy {
    private final UserService users;
    private User account(Authentication authentication) {
        if (authentication==null || !authentication.isAuthenticated()) return null;
        return users.getLoginData(authentication.getName());
    }
    public boolean canAccessBackend(Authentication authentication) {
        User user=account(authentication);
        return user!=null && Boolean.TRUE.equals(user.getEnabled()) && "BACKEND".equals(user.getUserType());
    }
    public boolean canAccessH5(Authentication authentication) {
        User user=account(authentication);
        return user!=null && Boolean.TRUE.equals(user.getEnabled()) && "IOU".equals(user.getUserType());
    }
}
