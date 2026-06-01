package com.exam.ai.auth.service.impl;

import com.exam.ai.auth.service.RateLimitService;
import com.exam.ai.common.exception.BusinessException;
import com.exam.ai.common.config.SecurityProperties;
import com.exam.ai.security.RedisKeys;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * RateLimitServiceImpl 类，当前分层的业务组件，负责本模块对应的请求、服务或数据模型职责。
 */
@Service
public class RateLimitServiceImpl implements RateLimitService {

    private final StringRedisTemplate redisTemplate;

    /**
     * 构造 RateLimitServiceImpl 实例并注入运行所需依赖。
     * @param redisTemplate 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public RateLimitServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 校验业务参数或业务状态，阻止非法流程继续执行。
     * @param type 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param discriminator 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param limit 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
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

    /**
     * 校验业务参数或业务状态，阻止非法流程继续执行。
     * @param type 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param discriminator 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param limit 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param window 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public void check(String type, String discriminator, int limit, Duration window) {
        check(type, discriminator, new SecurityProperties.RateLimit(limit, window));
    }
}
