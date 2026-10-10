package me.zhengjie.modules.iou;

import lombok.RequiredArgsConstructor;
import me.zhengjie.exception.BadRequestException;
import me.zhengjie.modules.security.config.SecurityProperties;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** Redis 原子消费验证码；Redis 异常不能降级放行。 */
@Component
@RequiredArgsConstructor
public class IouRequestGuard {
    private static final String PASS_PREFIX="captcha_pass:";
    private final RedisTemplate<Object, Object> redisTemplate;
    private final SecurityProperties securityProperties;
    private static final DefaultRedisScript<String> CONSUME = new DefaultRedisScript<>(
        "local v=redis.call('GET',KEYS[1]); redis.call('DEL',KEYS[1]); return v", String.class);

    public void captcha(String uuid, String supplied) {
        if (uuid != null && uuid.startsWith(PASS_PREFIX)) {
            consume(uuid,supplied,PASS_PREFIX,false);
            return;
        }
        imageCaptcha(uuid,supplied);
    }

    /** 只验证原始图形挑战，不能拿通过凭证续期或再签发。 */
    public void imageCaptcha(String uuid, String supplied) {
        consume(uuid,supplied,securityProperties.getCodeKey(),true);
    }

    private void consume(String uuid, String supplied, String prefix, boolean ignoreCase) {
        if (uuid == null || !uuid.startsWith(prefix)
            || !uuid.substring(prefix.length()).matches("[a-f0-9]{32}")) {
            throw new BadRequestException("验证码不存在或已过期");
        }
        String expected = redisTemplate.execute(CONSUME, Collections.singletonList(uuid));
        if (expected == null || supplied == null || !(ignoreCase?expected.equalsIgnoreCase(supplied.trim()):expected.equals(supplied.trim()))) {
            throw new BadRequestException("验证码错误或已过期");
        }
    }

    /** 验证通过后发出两分钟、单次消费的凭证；业务接口仍须消费，不能信任前端布尔值。 */
    public Map<String,String> verifyImage(String uuid, String code) {
        imageCaptcha(uuid,code);
        return issuePass();
    }

    /** 仅由已通过图形验证或成功建账号的服务端流程调用，没有匿名签发入口。 */
    public Map<String,String> issuePass() {
        String pass=PASS_PREFIX+UUID.randomUUID().toString().replace("-", "");
        String secret=UUID.randomUUID().toString().replace("-", "").substring(0,16);
        redisTemplate.opsForValue().set(pass,secret,120,TimeUnit.SECONDS);
        Map<String,String> result=new LinkedHashMap<>();result.put("uuid",pass);result.put("code",secret);
        return result;
    }

}
