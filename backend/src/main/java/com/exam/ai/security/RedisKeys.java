package com.exam.ai.security;

/**
 * RedisKeys 类，承载当前分层中的业务职责。
 */
public final class RedisKeys {

    private RedisKeys() {
    }

    /**
     * 执行当前业务步骤，维护调用方需要的处理结果。
     * @param captchaId 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public static String captcha(String captchaId) {
        return "captcha:" + captchaId;
    }

    /**
     * 执行当前业务步骤，维护调用方需要的处理结果。
     * @param userId 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public static String session(Long userId) {
        return "auth:session:" + userId;
    }

    /**
     * 创建业务数据并完成必要的状态初始化。
     * @param tokenHash 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public static String refresh(String tokenHash) {
        return "auth:refresh:" + tokenHash;
    }

    /**
     * 执行当前业务步骤，维护调用方需要的处理结果。
     * @param type 业务参数，参与当前方法的校验、查询或状态变更。
     * @param discriminator 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public static String rate(String type, String discriminator) {
        return "rate:" + type + ":" + discriminator;
    }
}
