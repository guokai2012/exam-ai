package com.exam.ai.auth.service;

import com.exam.ai.common.exception.BusinessException;
import com.exam.ai.common.config.SecurityProperties;
import com.exam.ai.security.RedisKeys;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * RateLimitService 接口，定义当前业务模块对外提供的服务契约。
 */
public interface RateLimitService {

    /**
     * 校验业务参数或业务状态，阻止非法流程继续执行。
     * @param type 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param discriminator 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param limit 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public void check(String type, String discriminator, SecurityProperties.RateLimit limit);
    /**
     * 校验业务参数或业务状态，阻止非法流程继续执行。
     * @param type 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param discriminator 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param limit 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param window 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public void check(String type, String discriminator, int limit, Duration window);
}
