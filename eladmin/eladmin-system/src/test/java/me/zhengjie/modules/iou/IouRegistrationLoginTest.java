package me.zhengjie.modules.iou;

import me.zhengjie.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.HashMap;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IouRegistrationLoginTest {
    @Test void successfulRegistrationIssuesCredentialOnlyAfterAccountCommit() {
        IouAccountService accounts=mock(IouAccountService.class);IouRequestGuard guard=mock(IouRequestGuard.class);
        HttpServletRequest http=mock(HttpServletRequest.class);when(http.getRemoteAddr()).thenReturn("127.0.0.1");
        Map<String,String> credential=new HashMap<>();credential.put("uuid","pass");credential.put("code","secret");
        when(guard.issuePass()).thenReturn(credential);
        IouRegisterRequest request=new IouRegisterRequest();
        org.springframework.http.ResponseEntity<Map<String,Object>> response=new IouAuthController(accounts,guard).register(request,http);
        assertEquals(201,response.getStatusCodeValue());assertSame(credential,response.getBody().get("loginCaptcha"));
        org.mockito.InOrder order=inOrder(accounts,guard);order.verify(accounts).register(request,"127.0.0.1");order.verify(guard).issuePass();
    }
    @Test void failedRegistrationDoesNotIssueLoginCredential() {
        IouAccountService accounts=mock(IouAccountService.class);IouRequestGuard guard=mock(IouRequestGuard.class);
        HttpServletRequest http=mock(HttpServletRequest.class);when(http.getRemoteAddr()).thenReturn("127.0.0.1");
        doThrow(new BadRequestException("核验失败")).when(accounts).register(any(),anyString());
        assertThrows(BadRequestException.class,()->new IouAuthController(accounts,guard).register(new IouRegisterRequest(),http));
        verifyNoInteractions(guard);
    }
    @Test void credentialFailureDoesNotReportCreatedAccountAsFailedRegistration() {
        IouAccountService accounts=mock(IouAccountService.class);IouRequestGuard guard=mock(IouRequestGuard.class);
        HttpServletRequest http=mock(HttpServletRequest.class);
        when(guard.issuePass()).thenThrow(new IllegalStateException("Redis unavailable"));
        org.springframework.http.ResponseEntity<Map<String,Object>> response=new IouAuthController(accounts,guard).register(new IouRegisterRequest(),http);
        assertEquals(201,response.getStatusCodeValue());assertTrue(response.getBody().containsKey("loginCaptcha"));assertNull(response.getBody().get("loginCaptcha"));
    }
}
