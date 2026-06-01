package com.exam.ai.user.controller;

import com.exam.ai.common.api.ApiResponse;
import com.exam.ai.user.dto.PermissionResponse;
import com.exam.ai.user.dto.PermissionScanResponse;
import com.exam.ai.user.dto.SavePermissionRequest;
import com.exam.ai.user.service.AdminPermissionService;
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
@RequestMapping("/api/admin/permissions")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "后台权限管理接口", description = "管理员查看权限树、维护动作权限并扫描 Controller 权限注解")
public class AdminPermissionController {

    private final AdminPermissionService permissionService;

    public AdminPermissionController(AdminPermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('admin:permission:list')")
    @Operation(summary = "权限树", description = "查询与菜单结构关联的权限树。")
    public ApiResponse<List<PermissionResponse>> list() {
        return ApiResponse.ok(permissionService.list());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('admin:permission:create')")
    @Operation(summary = "新建动作权限", description = "手动创建动作权限，通常挂在菜单权限节点下。")
    public ApiResponse<PermissionResponse> create(@Valid @RequestBody SavePermissionRequest request) {
        return ApiResponse.ok(permissionService.create(request));
    }

    @PostMapping("/scan")
    @PreAuthorize("hasAuthority('admin:permission:scan')")
    @Operation(summary = "扫描接口权限", description = "扫描 Controller 上的 hasAuthority/hasAnyAuthority 权限表达式，同步动作权限。")
    public ApiResponse<PermissionScanResponse> scan() {
        return ApiResponse.ok(permissionService.scanControllerPermissions());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('admin:permission:update')")
    @Operation(summary = "编辑动作权限", description = "编辑可维护的动作权限。")
    public ApiResponse<PermissionResponse> update(@Parameter(description = "权限 ID") @PathVariable Long id, @Valid @RequestBody SavePermissionRequest request) {
        return ApiResponse.ok(permissionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('admin:permission:delete')")
    @Operation(summary = "删除动作权限", description = "删除可维护的动作权限，并清理角色权限关系。")
    public ApiResponse<Void> delete(@Parameter(description = "权限 ID") @PathVariable Long id) {
        permissionService.delete(id);
        return ApiResponse.ok();
    }
}
