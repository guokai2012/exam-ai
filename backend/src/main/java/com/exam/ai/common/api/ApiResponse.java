package com.exam.ai.common.api;

import lombok.Builder;

import io.swagger.v3.oas.annotations.media.Schema;

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

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>("OK", "success", data);
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>("OK", message, data);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>("OK", "success", null);
    }

    public static ApiResponse<Void> error(String code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}

