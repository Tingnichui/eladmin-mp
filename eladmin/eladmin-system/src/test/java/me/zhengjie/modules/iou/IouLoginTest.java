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
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import javax.crypto.Cipher;
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
        when(passwords.matches("111111","test-bcrypt")).thenReturn(true);when(tokens.createToken(principal)).thenReturn("test-token");
        when(accounts.profile(123L)).thenReturn(new HashMap<>());
    }
    @AfterEach void cleanup() {RsaProperties.privateKey=originalKey;SecurityContextHolder.clearContext();}
    private AuthUserDto request() throws Exception {
        AuthUserDto dto=new AuthUserDto();dto.setUsername("110101199001010010");dto.setLoginType("IOU");
        dto.setPassword(RsaUtils.encryptByPublicKey(keyPair.getPublicKey(),"111111"));return dto;
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
    @Test void invalidNumericPasswordCannotResolveIdentityOrCreateSession() throws Exception {
        for (String password : Arrays.asList("", "12345", "1234567", "abc123", "12 345", "123456\n", "１２３４５６")) {
            AuthUserDto dto=request();
            if (password.isEmpty()) {
                // 分段加密工具对空字符串不生成密文，使用有效的 RSA 空明文密文验证密码规则。
                Cipher cipher=Cipher.getInstance("RSA");
                cipher.init(Cipher.ENCRYPT_MODE,RsaUtils.getPublicKey(keyPair.getPublicKey()));
                dto.setPassword(Base64.getEncoder().encodeToString(cipher.doFinal(new byte[0])));
            } else {
                dto.setPassword(RsaUtils.encryptByPublicKey(keyPair.getPublicKey(),password));
            }
            assertEquals("密码必须为 6 位数字", assertThrows(BadRequestException.class,
                () -> controller.login(dto,new MockHttpServletRequest())).getMessage());
        }
        verifyNoInteractions(accounts,details,passwords,tokens,online);
    }
    @Test void leadingZeroIsPreservedWhenCheckingLoginPassword() throws Exception {
        AuthUserDto dto=request();
        dto.setPassword(RsaUtils.encryptByPublicKey(keyPair.getPublicKey(),"012345"));
        when(passwords.matches("012345","test-bcrypt")).thenReturn(true);
        controller.login(dto,new MockHttpServletRequest());
        verify(passwords).matches("012345","test-bcrypt");
    }
    @Test void backendLoginKeepsExistingPasswordRules() throws Exception {
        AuthUserDto dto=request();dto.setLoginType("BACKEND");dto.setUsername("backend_user");
        dto.setPassword(RsaUtils.encryptByPublicKey(keyPair.getPublicKey(),"backend-password"));
        dto.setUuid("captcha-key");dto.setCode("ok");
        principal.getUser().setUserType("BACKEND");
        when(details.loadUserByUsername("backend_user")).thenReturn(principal);
        when(passwords.matches("backend-password","test-bcrypt")).thenReturn(true);
        RedisUtils redis=mock(RedisUtils.class);when(redis.get("captcha-key",String.class)).thenReturn("ok");
        SecurityProperties properties=new SecurityProperties();properties.setTokenStartWith("Bearer");
        controller=new AuthController(properties,redis,online,tokens,mock(CaptchaConfig.class),new LoginProperties(),passwords,details,accounts);
        ApplicationContext previous=(ApplicationContext)ReflectionTestUtils.getField(cn.hutool.extra.spring.SpringUtil.class,"applicationContext");
        StaticApplicationContext context=new StaticApplicationContext();context.getEnvironment().setActiveProfiles("prod");
        new cn.hutool.extra.spring.SpringUtil().setApplicationContext(context);
        try {
            controller.login(dto,new MockHttpServletRequest());
            verify(passwords).matches("backend-password","test-bcrypt");
            verify(accounts,never()).resolveLogin(anyString());
        } finally {
            ReflectionTestUtils.setField(cn.hutool.extra.spring.SpringUtil.class,"applicationContext",previous);
            context.close();
        }
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
