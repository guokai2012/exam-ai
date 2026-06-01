package com.exam.ai.user.dto;

import lombok.Builder;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "用户响应")
@Builder
public record UserResponse(
        @Schema(description = "用户 ID")
        Long id,
        @Schema(description = "用户名")
        String username,
        @Schema(description = "昵称")
        String nickname,
        @Schema(description = "状态：1 启用，0 禁用")
        Integer status,
        @Schema(description = "角色编码列表")
        List<String> roles,
        @Schema(description = "是否强制修改密码")
        Boolean forcePasswordChange,
        @Schema(description = "最近登录时间")
        LocalDateTime lastLoginAt,
        @Schema(description = "创建时间")
        LocalDateTime createdAt
) {
}

