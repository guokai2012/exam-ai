package com.exam.ai.user.dto;

import lombok.Builder;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * AdminCreateUserRequest 不可变业务数据记录，用于接口入参、接口返回或服务间传输。
 */
@Schema(description = "后台新建用户请求")
@Builder
public record AdminCreateUserRequest(
        @Schema(description = "用户名，3 到 64 位", example = "teacher01")
        @NotBlank @Size(min = 3, max = 64) String username,
        @Schema(description = "初始密码，6 到 64 位", example = "password123")
        @NotBlank @Size(min = 6, max = 64) String password,
        @Schema(description = "昵称", example = "张老师")
        @NotBlank @Size(max = 64) String nickname,
        @Schema(description = "角色编码列表", example = "[\"TEACHER\"]")
        @NotEmpty List<String> roles
) {
}

