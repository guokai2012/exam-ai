package com.exam.ai.security;

/**
 * RedisKeys 类，当前分层的业务组件，负责本模块对应的请求、服务或数据模型职责。
 */
public final class RedisKeys {

    private RedisKeys() {
    }

    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param captchaId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public static String captcha(String captchaId) {
        return "captcha:" + captchaId;
    }

    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param userId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public static String session(Long userId) {
        return "auth:session:" + userId;
    }

    /**
     * 创建业务数据并完成必要的默认状态初始化。
     * @param tokenHash 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public static String refresh(String tokenHash) {
        return "auth:refresh:" + tokenHash;
    }

    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param type 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param discriminator 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public static String rate(String type, String discriminator) {
        return "rate:" + type + ":" + discriminator;
    }

    /**
     * 生成菜单扫描临时 Token 的 Redis Key。
     *
     * @param tokenHash 临时 Token 哈希值。
     * @return 菜单扫描临时 Token Redis Key。
     */
    public static String menuScanToken(String tokenHash) {
        return "menu:scan-token:" + tokenHash;
    }

    /**
     * 生成菜单扫描临时 Token 获取限流 Redis Key。
     *
     * @param userId 当前管理员用户 ID。
     * @param sessionId 当前登录会话 ID。
     * @return 菜单扫描临时 Token 限流 Redis Key。
     */
    public static String menuScanTokenLimit(Long userId, String sessionId) {
        return "menu:scan-token:limit:" + userId + ":" + sessionId;
    }
}
