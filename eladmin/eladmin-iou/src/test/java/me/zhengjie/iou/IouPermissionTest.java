package me.zhengjie.iou;

import cn.hutool.jwt.JWT;
import me.zhengjie.config.AuthorityConfig;
import me.zhengjie.iou.domain.IouNote;
import me.zhengjie.iou.domain.IouUser;
import me.zhengjie.iou.domain.dto.IouNoteQueryCriteria;
import me.zhengjie.iou.domain.dto.IouUserQueryCriteria;
import me.zhengjie.iou.rest.IouNoteController;
import me.zhengjie.iou.rest.IouUserController;
import me.zhengjie.iou.service.IouNoteService;
import me.zhengjie.iou.service.IouUserService;
import me.zhengjie.utils.SecurityUtils;
import me.zhengjie.utils.SpringBeanHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 使用真实方法权限拦截器和 el 检查器，不访问数据库，不创建管理账号。 */
class IouPermissionTest {
    private AnnotationConfigApplicationContext context;
    private IouUserController users;
    private IouNoteController notes;

    @Configuration
    @EnableGlobalMethodSecurity(prePostEnabled = true)
    static class ConfigurationForTest {
        @Bean SpringBeanHolder holder() { return new SpringBeanHolder(); }
        @Bean(name = "el") AuthorityConfig permissions() { return new AuthorityConfig(); }
        @Bean UserDetailsService userDetails() { return mock(UserDetailsService.class); }
        @Bean IouUserService userService() { return mock(IouUserService.class); }
        @Bean IouNoteService noteService() { return mock(IouNoteService.class); }
        @Bean IouUserController users(IouUserService service) { return new IouUserController(service); }
        @Bean IouNoteController notes(IouNoteService service) { return new IouNoteController(service); }
    }

    @BeforeEach
    void setup() {
        context = new AnnotationConfigApplicationContext(ConfigurationForTest.class);
        users = context.getBean(IouUserController.class);
        notes = context.getBean(IouNoteController.class);
        SecurityUtils.header = "Authorization";
        SecurityUtils.tokenStartWith = "Bearer ";
        MockHttpServletRequest request = new MockHttpServletRequest();
        String token = JWT.create().setPayload("sub", "iou-test")
            .setKey("unit-test-only-signing-key".getBytes(StandardCharsets.UTF_8)).sign();
        request.addHeader("Authorization", "Bearer " + token);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        authenticate("other:list");
    }

    private void authenticate(String... permissions) {
        UserDetails principal = User.withUsername("iou-test").password("unused-test-password")
            .authorities(permissions).build();
        when(context.getBean(UserDetailsService.class).loadUserByUsername("iou-test")).thenReturn(principal);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void cleanup() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
        context.close();
    }

    @Test
    void unauthorizedRoleCannotReadWriteOrExportUsers() {
        assertThrows(AccessDeniedException.class, () -> users.queryIouUser(new IouUserQueryCriteria()));
        assertThrows(AccessDeniedException.class, () -> users.createIouUser(new IouUser()));
        assertThrows(AccessDeniedException.class, () -> users.updateIouUser(new IouUser()));
        assertThrows(AccessDeniedException.class, () -> users.deleteIouUser(Collections.singletonList(1L)));
        assertThrows(AccessDeniedException.class, () -> users.exportIouUser(new MockHttpServletResponse(), new IouUserQueryCriteria()));
        verifyNoInteractions(context.getBean(IouUserService.class));
    }

    @Test
    void unauthorizedRoleCannotReadWriteOrExportNotes() {
        assertThrows(AccessDeniedException.class, () -> notes.queryIouNote(new IouNoteQueryCriteria()));
        assertThrows(AccessDeniedException.class, () -> notes.createIouNote(new IouNote()));
        assertThrows(AccessDeniedException.class, () -> notes.updateIouNote(new IouNote()));
        assertThrows(AccessDeniedException.class, () -> notes.deleteIouNote(Collections.singletonList(1L)));
        assertThrows(AccessDeniedException.class, () -> notes.exportIouNote(new MockHttpServletResponse(), new IouNoteQueryCriteria()));
        verifyNoInteractions(context.getBean(IouNoteService.class));
    }

    @Test
    void listPermissionDoesNotGrantWritePermission() {
        authenticate("iouUser:list", "iouNote:list");
        assertDoesNotThrow(() -> users.queryIouUser(new IouUserQueryCriteria()));
        assertDoesNotThrow(() -> notes.queryIouNote(new IouNoteQueryCriteria()));
        assertThrows(AccessDeniedException.class, () -> users.createIouUser(new IouUser()));
        assertThrows(AccessDeniedException.class, () -> users.revealIdentity(1L));
        assertThrows(AccessDeniedException.class, () -> notes.deleteIouNote(Collections.singletonList(1L)));
    }

    @Test
    void onlyIdentityPermissionAllowsRevealAndResponseCannotBeCached() {
        authenticate("iouUser:viewIdentity");
        IouUserService service = context.getBean(IouUserService.class);
        when(service.revealIdentity(1L)).thenReturn("000000200001010001");
        org.springframework.http.ResponseEntity<java.util.Map<String, String>> response = users.revealIdentity(1L);
        assertEquals("no-store", response.getHeaders().getCacheControl());
        assertEquals("000000200001010001", response.getBody().get("idNo"));
        verify(service).revealIdentity(1L);
    }
}
