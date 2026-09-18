package me.zhengjie.config;

import com.alibaba.fastjson2.JSON;
import me.zhengjie.utils.RedisUtils;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RedisUtilsDateTest {

    @Test
    @SuppressWarnings("unchecked")
    void shouldConvertLegacyTimestampObjectToDate() {
        Date expected = new Date(1735689600000L);
        Object cachedValue = JSON.parseObject(
                "{\"@type\":\"java.sql.Timestamp\",\"val\":1735689600000}",
                Object.class
        );

        RedisTemplate<Object, Object> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<Object, Object> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("date-key")).thenReturn(cachedValue);

        RedisUtils redisUtils = new RedisUtils(redisTemplate);

        assertEquals(expected, redisUtils.get("date-key", Date.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldConvertPlainDateStringToDate() {
        RedisTemplate<Object, Object> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<Object, Object> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("date-key")).thenReturn("2025-11-17 22:42:31");

        RedisUtils redisUtils = new RedisUtils(redisTemplate);

        assertEquals(
                cn.hutool.core.date.DateUtil.parse("2025-11-17 22:42:31"),
                redisUtils.get("date-key", Date.class)
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldConvertMillisecondStringToDate() {
        RedisTemplate<Object, Object> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<Object, Object> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("date-key")).thenReturn("1735689600000");

        RedisUtils redisUtils = new RedisUtils(redisTemplate);

        assertEquals(new Date(1735689600000L), redisUtils.get("date-key", Date.class));
    }
}
