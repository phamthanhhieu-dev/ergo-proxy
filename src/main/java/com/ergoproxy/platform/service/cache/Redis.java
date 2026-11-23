package com.ergoproxy.platform.service.cache;

import com.ergoproxy.platform.common.constant.Const;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class Redis implements CacheRouter {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public String getSource() {
        return Const.Source.REDIS;
    }

    public void set(String key, Object value, Duration duration) {
        redisTemplate.opsForValue().set(key, value, duration);
    }

    public <T> T get(String key, Class<T> type) {
        Object value = redisTemplate.opsForValue().get(key);
        return value == null ? null : objectMapper.convertValue(value, type);
    }

    public void hashSet(String key, String field, Object value, Duration duration) {
        redisTemplate.opsForHash().put(key, field, value);
        redisTemplate.expire(key, duration);
    }

    public <T> List<T> hashValues(String key, Class<T> type) {
        return redisTemplate.opsForHash().values(key).stream()
            .map(value -> objectMapper.convertValue(value, type))
            .toList();
    }

    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    public Boolean exists(String key) {
        return redisTemplate.hasKey(key);
    }
}
