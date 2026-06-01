package com.exam.ai.common.result;

import lombok.Builder;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * ApiResponse 不可变业务数据记录，用于接口入参、接口返回或服务间传输。
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
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param data 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>("OK", "success", data);
    }

    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param data 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param message 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>("OK", message, data);
    }

    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public static ApiResponse<Void> ok() {
        return new ApiResponse<>("OK", "success", null);
    }

    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param code 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param message 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public static ApiResponse<Void> error(String code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}

