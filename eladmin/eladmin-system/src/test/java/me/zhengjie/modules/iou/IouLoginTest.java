package me.zhengjie.modules.iou;

import com.alibaba.fastjson2.JSON;
import me.zhengjie.config.properties.RsaProperties;
import me.zhengjie.exception.BadRequestException;
import me.zhengjie.modules.security.config.*;
import me.zhengjie.modules.security.rest.AuthController;
import me.zhengjie.modules.security.security.TokenProvider;
import me.zhengjie.modules.security.service.*;
import me.zhengjie.modules.security.service.dto.*;
import me.zhengjie.modules.system.domain.User;
import me.zhengjie.utils.RedisUtils;
import me.zhengjie.utils.RsaUtils;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IouLoginTest {
    private AuthController controller;
    private IouAccountService accounts;
    private UserDetailsServiceImpl details;
    private OnlineUserService online;
    private TokenProvider tokens;
    private PasswordEncoder passwords;
    private JwtUserDto principal;
    private RsaUtils.RsaKeyPair keyPair;
    private String originalKey;
    @BeforeEach void setup() throws Exception {
        originalKey=RsaProperties.privateKey;keyPair=RsaUtils.generateKeyPair();RsaProperties.privateKey=keyPair.getPrivateKey();
        accounts=mock(IouAccountService.class);details=mock(UserDetailsServiceImpl.class);
        online=mock(OnlineUserService.class);tokens=mock(TokenProvider.class);passwords=mock(PasswordEncoder.class);
        SecurityProperties properties=new SecurityProperties();properties.setTokenStartWith("Bearer");
        LoginProperties login=new LoginProperties();login.setSingleLogin(true);
        controller=new AuthController(properties,mock(RedisUtils.class),online,tokens,mock(CaptchaConfig.class),login,passwords,details,accounts);
        User user=new User();user.setId(123L);user.setUsername("iou_canonical");user.setUserType("IOU");user.setPassword("test-bcrypt");user.setEnabled(true);
        principal=new JwtUserDto(user,Collections.emptyList(),Collections.singletonList(new AuthorityDto("iou:profile:read")));
        when(accounts.resolveLogin(anyString())).thenReturn("iou_canonical");when(details.loadUserByUsername("iou_canonical")).thenReturn(principal);
        when(passwords.matches("test-password","test-bcrypt")).thenReturn(true);when(tokens.createToken(principal)).thenReturn("test-token");
        when(accounts.profile(123L)).thenReturn(new HashMap<>());
    }
    @AfterEach void cleanup() {RsaProperties.privateKey=originalKey;SecurityContextHolder.clearContext();}
    private AuthUserDto request() throws Exception {
        AuthUserDto dto=new AuthUserDto();dto.setUsername("110101199001010010");dto.setLoginType("IOU");
        dto.setPassword(RsaUtils.encryptByPublicKey(keyPair.getPublicKey(),"test-password"));return dto;
    }
    @Test void reusesLoginTokenAndOnlineFlowWithCanonicalUsername() throws Exception {
        Map<?,?> result=(Map<?,?>)controller.login(request(),new MockHttpServletRequest()).getBody();
        assertEquals("Bearer test-token",result.get("token"));assertFalse(JSON.toJSONString(result).contains("test-bcrypt"));
        verify(details).loadUserByUsername("iou_canonical");
        verify(online).kickOutForUsername("iou_canonical");verify(tokens).createToken(principal);verify(online).save(eq(principal),eq("test-token"),any());
        assertFalse(JSON.toJSONString(result).contains("110101199001010010"));
    }
    @Test void missingCaptchaStillRequiresCorrectIdentityAndPassword() throws Exception {
        when(accounts.resolveLogin(anyString())).thenThrow(new BadRequestException("账号或密码错误"));
        assertThrows(BadRequestException.class,()->controller.login(request(),new MockHttpServletRequest()));
        verifyNoInteractions(details,tokens,online);
    }
    @Test void incorrectPasswordCannotCreateOnlineSession() throws Exception {
        when(passwords.matches(any(),any())).thenReturn(false);
        assertThrows(BadRequestException.class,()->controller.login(request(),new MockHttpServletRequest()));verifyNoInteractions(tokens,online);
    }
    @Test void iouLoginCannotUseBackendAccount() throws Exception {
        principal.getUser().setUserType("BACKEND");
        assertThrows(BadRequestException.class,()->controller.login(request(),new MockHttpServletRequest()));verifyNoInteractions(tokens,online);
    }
    @Test void authAuditSerializationDoesNotContainIdentityOrPassword() throws Exception {
        AuthUserDto r=request();String json=JSON.toJSONString(r);
        assertFalse(json.contains(r.getUsername()));assertFalse(json.contains(r.getPassword()));
        IouRegisterRequest registration=new IouRegisterRequest();registration.setRealName("合成姓名");registration.setIdNo(r.getUsername());registration.setPassword(r.getPassword());
        String payload=JSON.toJSONString(registration);
        assertFalse(payload.contains("合成姓名"));assertFalse(payload.contains(r.getUsername()));assertFalse(payload.contains(r.getPassword()));
    }
}
