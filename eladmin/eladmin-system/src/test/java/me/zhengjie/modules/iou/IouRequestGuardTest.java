package me.zhengjie.modules.iou;

import me.zhengjie.exception.BadRequestException;
import me.zhengjie.modules.security.config.SecurityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.ValueOperations;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class IouRequestGuardTest {
    @Test void imageVerificationIssuesShortLivedOneUseCredential() {
        RedisTemplate<Object,Object> redis=mock(RedisTemplate.class);
        ValueOperations<Object,Object> values=mock(ValueOperations.class);when(redis.opsForValue()).thenReturn(values);
        SecurityProperties properties=new SecurityProperties();properties.setCodeKey("captcha_code:");
        IouRequestGuard guard=new IouRequestGuard(redis,properties);
        when(redis.execute(any(RedisScript.class),anyList())).thenReturn("2");
        Map<String,String> credential=guard.verifyImage("captcha_code:0123456789abcdef0123456789abcdef","2");
        assertTrue(credential.get("uuid").matches("captcha_pass:[a-f0-9]{32}"));
        assertTrue(credential.get("code").matches("[a-f0-9]{16}"));
        verify(values).set(credential.get("uuid"),credential.get("code"),120L,TimeUnit.SECONDS);
        when(redis.execute(any(RedisScript.class),anyList())).thenReturn(credential.get("code"),null);
        assertDoesNotThrow(()->guard.captcha(credential.get("uuid"),credential.get("code")));
        assertThrows(BadRequestException.class,()->guard.captcha(credential.get("uuid"),credential.get("code")));
        assertThrows(BadRequestException.class,()->guard.verifyImage(credential.get("uuid"),credential.get("code")));
    }
    @Test void incorrectImageNeverIssuesCredential() {
        RedisTemplate<Object,Object> redis=mock(RedisTemplate.class);
        SecurityProperties properties=new SecurityProperties();properties.setCodeKey("captcha_code:");
        when(redis.execute(any(RedisScript.class),anyList())).thenReturn("2");
        assertThrows(BadRequestException.class,()->new IouRequestGuard(redis,properties)
            .verifyImage("captcha_code:0123456789abcdef0123456789abcdef","wrong"));
        verify(redis,never()).opsForValue();
    }
    @Test void captchaRestrictsKeyNamespaceAndFailsClosed() {
        RedisTemplate<Object,Object> redis=mock(RedisTemplate.class);SecurityProperties properties=new SecurityProperties();properties.setCodeKey("captcha_code:");
        IouRequestGuard guard=new IouRequestGuard(redis,properties);
        assertThrows(BadRequestException.class,()->guard.captcha("online_token:anything","2"));verifyNoInteractions(redis);
        when(redis.execute(any(RedisScript.class),anyList())).thenReturn("2",null);
        String uuid="captcha_code:0123456789abcdef0123456789abcdef";
        assertDoesNotThrow(()->guard.captcha(uuid," 2 "));assertThrows(BadRequestException.class,()->guard.captcha(uuid,"2"));
    }
    @Test void redisFailureCannotEnableVerification() {
        RedisTemplate<Object,Object> redis=mock(RedisTemplate.class);SecurityProperties properties=new SecurityProperties();properties.setCodeKey("captcha_code:");
        when(redis.execute(any(RedisScript.class),anyList())).thenThrow(new IllegalStateException("redis unavailable"));
        IouRequestGuard guard=new IouRequestGuard(redis,properties);
        assertThrows(IllegalStateException.class,()->guard.captcha("captcha_code:0123456789abcdef0123456789abcdef","2"));
    }
}
