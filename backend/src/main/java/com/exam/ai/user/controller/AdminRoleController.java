package com.exam.ai.user.controller;

import com.exam.ai.common.api.ApiResponse;
import com.exam.ai.user.dto.RoleResponse;
import com.exam.ai.user.dto.SaveRoleRequest;
import com.exam.ai.user.service.AdminRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/roles")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "后台角色管理接口", description = "管理员维护角色并为角色分配权限")
public class AdminRoleController {

    private final AdminRoleService roleService;

    public AdminRoleController(AdminRoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('admin:role:list')")
    @Operation(summary = "角色列表", description = "查询所有角色及其已分配权限码。")
    public ApiResponse<List<RoleResponse>> list() {
        return ApiResponse.ok(roleService.list());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('admin:role:create')")
    @Operation(summary = "新建角色", description = "创建角色并保存角色权限关系。")
    public ApiResponse<RoleResponse> create(@Valid @RequestBody SaveRoleRequest request) {
        return ApiResponse.ok(roleService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('admin:role:update')")
    @Operation(summary = "编辑角色", description = "更新角色名称、编码和权限关系。")
    public ApiResponse<RoleResponse> update(@Parameter(description = "角色 ID") @PathVariable Long id, @Valid @RequestBody SaveRoleRequest request) {
        return ApiResponse.ok(roleService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('admin:role:delete')")
    @Operation(summary = "删除角色", description = "删除未绑定用户的角色。")
    public ApiResponse<Void> delete(@Parameter(description = "角色 ID") @PathVariable Long id) {
        roleService.delete(id);
        return ApiResponse.ok();
    }
}
