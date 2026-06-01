package com.exam.ai.user.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.exam.ai.common.api.ApiResponse;
import com.exam.ai.user.dto.AdminCreateUserRequest;
import com.exam.ai.user.dto.AdminUpdateUserRequest;
import com.exam.ai.user.dto.ResetPasswordRequest;
import com.exam.ai.user.dto.UserResponse;
import com.exam.ai.user.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "后台用户管理接口", description = "管理员维护用户、角色关系、禁用账号、踢人下线和重置密码")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    @Operation(summary = "分页查询用户", description = "按关键字查询用户账号分页列表。")
    @PreAuthorize("hasAuthority('admin:user:list')")
    public ApiResponse<IPage<UserResponse>> list(@Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") long page,
                                                 @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") long size,
                                                 @Parameter(description = "用户名或昵称关键字") @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(adminUserService.list(page, size, keyword));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('admin:user:create')")
    @Operation(summary = "新建用户", description = "管理员新建用户并分配角色，新用户默认需要修改密码。")
    public ApiResponse<UserResponse> create(@Valid @RequestBody AdminCreateUserRequest request) {
        return ApiResponse.ok(adminUserService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('admin:user:update')")
    @Operation(summary = "编辑用户", description = "更新用户昵称、状态和角色。")
    public ApiResponse<UserResponse> update(@Parameter(description = "用户 ID") @PathVariable Long id, @Valid @RequestBody AdminUpdateUserRequest request) {
        return ApiResponse.ok(adminUserService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('admin:user:disable')")
    @Operation(summary = "禁用用户", description = "禁用指定用户账号。")
    public ApiResponse<Void> disable(@Parameter(description = "用户 ID") @PathVariable Long id) {
        adminUserService.disable(id);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/kick")
    @PreAuthorize("hasAuthority('session:kick')")
    @Operation(summary = "踢人下线", description = "撤销指定用户当前会话和刷新令牌。")
    public ApiResponse<Void> kick(@Parameter(description = "用户 ID") @PathVariable Long id) {
        adminUserService.kick(id);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('admin:user:reset-password')")
    @Operation(summary = "重置密码", description = "重置指定用户密码，并要求下次登录后修改密码。")
    public ApiResponse<UserResponse> resetPassword(@Parameter(description = "用户 ID") @PathVariable Long id, @Valid @RequestBody ResetPasswordRequest request) {
        return ApiResponse.ok(adminUserService.resetPassword(id, request));
    }
}
