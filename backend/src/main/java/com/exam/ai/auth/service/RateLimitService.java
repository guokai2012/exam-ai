package com.exam.ai.auth.service;

import com.exam.ai.common.exception.BusinessException;
import com.exam.ai.config.SecurityProperties;
import com.exam.ai.security.RedisKeys;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;

    public RateLimitService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void check(String type, String discriminator, SecurityProperties.RateLimit limit) {
        String key = RedisKeys.rate(type, discriminator);
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, limit.window());
        }
        if (count != null && count > limit.limit()) {
            throw BusinessException.tooManyRequests();
        }
    }

    public void check(String type, String discriminator, int limit, Duration window) {
        check(type, discriminator, new SecurityProperties.RateLimit(limit, window));
    }
}
