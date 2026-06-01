package com.exam.ai.user.controller;

import com.exam.ai.common.result.ApiResponse;
import com.exam.ai.user.vo.PermissionResponse;
import com.exam.ai.user.vo.PermissionScanResponse;
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

/**
 * AdminPermissionController 类，当前分层的业务组件，负责本模块对应的请求、服务或数据模型职责。
 */
@RestController
@RequestMapping("/api/admin/permissions")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "后台权限管理接口", description = "管理员查看权限树、维护动作权限并扫描 Controller 权限注解")
public class AdminPermissionController {

    private final AdminPermissionService permissionService;

    /**
     * 构造 AdminPermissionController 实例并注入运行所需依赖。
     * @param permissionService 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public AdminPermissionController(AdminPermissionService permissionService) {
        this.permissionService = permissionService;
    }

    /**
     * 查询业务数据集合，并按调用场景组织返回结构。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @GetMapping
    @PreAuthorize("hasAuthority('admin:permission:list')")
    @Operation(summary = "权限树", description = "查询与菜单结构关联的权限树。")
    public ApiResponse<List<PermissionResponse>> list() {
        return ApiResponse.ok(permissionService.list());
    }

    /**
     * 创建业务数据并完成必要的默认状态初始化。
     * @param request 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @PostMapping
    @PreAuthorize("hasAuthority('admin:permission:create')")
    @Operation(summary = "新建动作权限", description = "手动创建动作权限，通常挂在菜单权限节点下。")
    public ApiResponse<PermissionResponse> create(@Valid @RequestBody SavePermissionRequest request) {
        return ApiResponse.ok(permissionService.create(request));
    }

    /**
     * 更新业务状态，并保持相关数据的一致性。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @PostMapping("/scan")
    @PreAuthorize("hasAuthority('admin:permission:scan')")
    @Operation(summary = "扫描接口权限", description = "扫描 Controller 上的 hasAuthority/hasAnyAuthority 权限表达式，同步动作权限。")
    public ApiResponse<PermissionScanResponse> scan() {
        return ApiResponse.ok(permissionService.scanControllerPermissions());
    }

    /**
     * 更新业务状态，并保持相关数据的一致性。
     * @param id 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param request 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('admin:permission:update')")
    @Operation(summary = "编辑动作权限", description = "编辑可维护的动作权限。")
    public ApiResponse<PermissionResponse> update(@Parameter(description = "权限 ID") @PathVariable Long id, @Valid @RequestBody SavePermissionRequest request) {
        return ApiResponse.ok(permissionService.update(id, request));
    }

    /**
     * 删除或失效指定业务数据，并同步清理关联状态。
     * @param id 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('admin:permission:delete')")
    @Operation(summary = "删除动作权限", description = "删除可维护的动作权限，并清理角色权限关系。")
    public ApiResponse<Void> delete(@Parameter(description = "权限 ID") @PathVariable Long id) {
        permissionService.delete(id);
        return ApiResponse.ok();
    }
}
