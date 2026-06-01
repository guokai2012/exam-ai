package com.exam.ai.common.exception;

import org.springframework.http.HttpStatus;

/**
 * BusinessException 类，承载当前分层中的业务职责。
 */
public class BusinessException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    /**
     * 构造 BusinessException 实例并注入运行所需依赖。
     * @param code 业务参数，参与当前方法的校验、查询或状态变更。
     * @param message 业务参数，参与当前方法的校验、查询或状态变更。
     * @param status 业务参数，参与当前方法的校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public BusinessException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    /**
     * 执行当前业务步骤，维护调用方需要的处理结果。
     * @param message 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public static BusinessException badRequest(String message) {
        return new BusinessException("BAD_REQUEST", message, HttpStatus.BAD_REQUEST);
    }

    /**
     * 执行当前业务步骤，维护调用方需要的处理结果。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public static BusinessException unauthorized() {
        return new BusinessException("UNAUTHORIZED", "认证失败", HttpStatus.UNAUTHORIZED);
    }

    /**
     * 执行当前业务步骤，维护调用方需要的处理结果。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public static BusinessException forbidden() {
        return new BusinessException("FORBIDDEN", "无权访问", HttpStatus.FORBIDDEN);
    }

    /**
     * 转换业务对象，生成前端返回视图或内部传输结构。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public static BusinessException tooManyRequests() {
        return new BusinessException("TOO_MANY_REQUESTS", "请求过于频繁，请稍后再试", HttpStatus.TOO_MANY_REQUESTS);
    }

    /**
     * 执行当前业务步骤，维护调用方需要的处理结果。
     * @param message 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public static BusinessException conflict(String message) {
        return new BusinessException("CONFLICT", message, HttpStatus.CONFLICT);
    }

    /**
     * 查询或解析业务数据，返回前端或内部流程需要的结果。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public String getCode() {
        return code;
    }

    /**
     * 查询或解析业务数据，返回前端或内部流程需要的结果。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public HttpStatus getStatus() {
        return status;
    }
}
