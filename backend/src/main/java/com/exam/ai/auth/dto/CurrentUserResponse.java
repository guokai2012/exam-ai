package com.exam.ai.auth.dto;

import lombok.Builder;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "当前用户信息")
@Builder
public record CurrentUserResponse(
        @Schema(description = "用户 ID")
        Long userId,
        @Schema(description = "用户名")
        String username,
        @Schema(description = "昵称")
        String nickname,
        @Schema(description = "角色编码列表")
        List<String> roles,
        @Schema(description = "权限码列表")
        List<String> permissions,
        @Schema(description = "是否必须修改密码")
        Boolean forcePasswordChange
) {
}

