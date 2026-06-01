package com.exam.ai.common.result;

import lombok.Builder;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * ApiResponse 记录对象，封装当前业务流程中的不可变数据。
 */
@Schema(description = "统一接口响应")
@Builder
public record ApiResponse<T>(
        @Schema(description = "业务状态码，OK 表示成功")
        String code,
        @Schema(description = "响应消息")
        String message,
        @Schema(description = "响应数据")
        T data
) {

    /**
     * 执行当前业务步骤，维护调用方需要的处理结果。
     * @param data 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>("OK", "success", data);
    }

    /**
     * 执行当前业务步骤，维护调用方需要的处理结果。
     * @param data 业务参数，参与当前方法的校验、查询或状态变更。
     * @param message 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>("OK", message, data);
    }

    /**
     * 执行当前业务步骤，维护调用方需要的处理结果。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public static ApiResponse<Void> ok() {
        return new ApiResponse<>("OK", "success", null);
    }

    /**
     * 执行当前业务步骤，维护调用方需要的处理结果。
     * @param code 业务参数，参与当前方法的校验、查询或状态变更。
     * @param message 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public static ApiResponse<Void> error(String code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}

