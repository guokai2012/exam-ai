package com.exam.ai.user.controller;

import com.exam.ai.common.result.ApiResponse;
import com.exam.ai.user.service.AdminPermissionService;
import com.exam.ai.user.vo.PermissionResponse;
import com.exam.ai.user.vo.PermissionScanResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台权限管理接口，只允许管理员查看扫描权限树并触发 Controller 权限扫描。
 */
@RestController
@RequestMapping("/api/admin/permissions")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "后台权限管理接口", description = "管理员查看 Controller 扫描生成的权限树")
public class AdminPermissionController {

    private final AdminPermissionService permissionService;

    /**
     * 创建权限管理 Controller，并注入扫描权限服务。
     *
     * @param permissionService Controller 扫描权限服务。
     */
    public AdminPermissionController(AdminPermissionService permissionService) {
        this.permissionService = permissionService;
    }

    /**
     * 查询扫描生成的权限树。
     *
     * @return 统一响应封装的权限树数据。
     */
    @GetMapping
    @PreAuthorize("hasAuthority('admin:permission:list')")
    @Operation(summary = "权限树", description = "查询由 Controller 权限表达式扫描生成的权限树。")
    public ApiResponse<List<PermissionResponse>> list() {
        return ApiResponse.ok(permissionService.list());
    }

    /**
     * 触发 Controller 权限扫描，并将扫描结果全量同步到权限表。
     *
     * @return 统一响应封装的扫描统计信息。
     */
    @PostMapping("/scan")
    @PreAuthorize("hasAuthority('admin:permission:scan')")
    @Operation(summary = "扫描接口权限", description = "扫描 Controller 的 hasAuthority/hasAnyAuthority 表达式并全量更新权限数据。")
    public ApiResponse<PermissionScanResponse> scan() {
        return ApiResponse.ok(permissionService.scanControllerPermissions());
    }
}
