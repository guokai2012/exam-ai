package com.exam.ai.common.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * SecurityProperties 类，当前分层的业务组件，负责本模块对应的请求、服务或数据模型职责。
 */
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    private String jwtSecret;
    private Duration accessTokenTtl = Duration.ofMinutes(30);
    private Duration refreshTokenTtl = Duration.ofDays(14);
    private Duration captchaTtl = Duration.ofMinutes(5);
    private String refreshTokenPepper;
    private String refreshCookieName = "refresh_token";
    private boolean refreshCookieSecure = false;
    private BootstrapAdmin bootstrapAdmin = new BootstrapAdmin();
    private Map<String, RateLimit> rateLimits = new HashMap<>();

    /**
     * 查询或解析业务数据，返回前端或内部流程需要的结果。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public String getJwtSecret() {
        return jwtSecret;
    }

    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param jwtSecret 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    /**
     * 查询或解析业务数据，返回前端或内部流程需要的结果。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public Duration getAccessTokenTtl() {
        return accessTokenTtl;
    }

    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param accessTokenTtl 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public void setAccessTokenTtl(Duration accessTokenTtl) {
        this.accessTokenTtl = accessTokenTtl;
    }

    /**
     * 查询或解析业务数据，返回前端或内部流程需要的结果。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public Duration getRefreshTokenTtl() {
        return refreshTokenTtl;
    }

    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param refreshTokenTtl 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public void setRefreshTokenTtl(Duration refreshTokenTtl) {
        this.refreshTokenTtl = refreshTokenTtl;
    }

    /**
     * 查询或解析业务数据，返回前端或内部流程需要的结果。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public Duration getCaptchaTtl() {
        return captchaTtl;
    }

    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param captchaTtl 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public void setCaptchaTtl(Duration captchaTtl) {
        this.captchaTtl = captchaTtl;
    }

    /**
     * 查询或解析业务数据，返回前端或内部流程需要的结果。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public String getRefreshTokenPepper() {
        return refreshTokenPepper;
    }

    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param refreshTokenPepper 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public void setRefreshTokenPepper(String refreshTokenPepper) {
        this.refreshTokenPepper = refreshTokenPepper;
    }

    /**
     * 查询或解析业务数据，返回前端或内部流程需要的结果。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public String getRefreshCookieName() {
        return refreshCookieName;
    }

    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param refreshCookieName 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public void setRefreshCookieName(String refreshCookieName) {
        this.refreshCookieName = refreshCookieName;
    }

    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public boolean isRefreshCookieSecure() {
        return refreshCookieSecure;
    }

    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param refreshCookieSecure 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public void setRefreshCookieSecure(boolean refreshCookieSecure) {
        this.refreshCookieSecure = refreshCookieSecure;
    }

    /**
     * 查询或解析业务数据，返回前端或内部流程需要的结果。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public BootstrapAdmin getBootstrapAdmin() {
        return bootstrapAdmin;
    }

    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param bootstrapAdmin 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public void setBootstrapAdmin(BootstrapAdmin bootstrapAdmin) {
        this.bootstrapAdmin = bootstrapAdmin;
    }

    /**
     * 查询或解析业务数据，返回前端或内部流程需要的结果。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public Map<String, RateLimit> getRateLimits() {
        return rateLimits;
    }

    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param M 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param rateLimits 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public void setRateLimits(Map<String, RateLimit> rateLimits) {
        this.rateLimits = rateLimits;
    }

    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param name 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param defaultLimit 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param defaultWindow 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public RateLimit rateLimit(String name, int defaultLimit, Duration defaultWindow) {
        return rateLimits.getOrDefault(name, new RateLimit(defaultLimit, defaultWindow));
    }

    /**
     * RateLimit 不可变业务数据记录，用于接口入参、接口返回或服务间传输。
     * @param limit 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param window 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     */
    public record RateLimit(int limit, Duration window) {
    }

    /**
     * 启动引导管理员配置，保存首次启动时需要创建的管理员账号信息。
     */
    public static class BootstrapAdmin {
        private String username = "admin";
        private String password = "";
        private String nickname = "管理员";

        /**
         * 查询或解析业务数据，返回前端或内部流程需要的结果。
         * @return 封装后的业务处理结果。
         * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
         */
        public String getUsername() {
            return username;
        }

        /**
         * 执行当前业务步骤，并返回调用方需要的处理结果。
         * @param username 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
         * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
         */
        public void setUsername(String username) {
            this.username = username;
        }

        /**
         * 查询或解析业务数据，返回前端或内部流程需要的结果。
         * @return 封装后的业务处理结果。
         * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
         */
        public String getPassword() {
            return password;
        }

        /**
         * 执行当前业务步骤，并返回调用方需要的处理结果。
         * @param password 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
         * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
         */
        public void setPassword(String password) {
            this.password = password;
        }

        /**
         * 查询或解析业务数据，返回前端或内部流程需要的结果。
         * @return 封装后的业务处理结果。
         * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
         */
        public String getNickname() {
            return nickname;
        }

        /**
         * 执行当前业务步骤，并返回调用方需要的处理结果。
         * @param nickname 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
         * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
         */
        public void setNickname(String nickname) {
            this.nickname = nickname;
        }
    }
}
