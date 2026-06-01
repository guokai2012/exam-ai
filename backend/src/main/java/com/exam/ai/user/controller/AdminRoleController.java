package com.exam.ai.user.controller;

import com.exam.ai.common.result.ApiResponse;
import com.exam.ai.user.vo.RoleResponse;
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

/**
 * AdminRoleController 类，承载当前分层中的业务职责。
 */
@RestController
@RequestMapping("/api/admin/roles")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "后台角色管理接口", description = "管理员维护角色并为角色分配权限")
public class AdminRoleController {

    private final AdminRoleService roleService;

    /**
     * 构造 AdminRoleController 实例并注入运行所需依赖。
     * @param roleService 业务参数，参与当前方法的校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public AdminRoleController(AdminRoleService roleService) {
        this.roleService = roleService;
    }

    /**
     * 查询业务数据集合，并按调用场景组织返回结构。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @GetMapping
    @PreAuthorize("hasAuthority('admin:role:list')")
    @Operation(summary = "角色列表", description = "查询所有角色及其已分配权限码。")
    public ApiResponse<List<RoleResponse>> list() {
        return ApiResponse.ok(roleService.list());
    }

    /**
     * 创建业务数据并完成必要的状态初始化。
     * @param request 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @PostMapping
    @PreAuthorize("hasAuthority('admin:role:create')")
    @Operation(summary = "新建角色", description = "创建角色并保存角色权限关系。")
    public ApiResponse<RoleResponse> create(@Valid @RequestBody SaveRoleRequest request) {
        return ApiResponse.ok(roleService.create(request));
    }

    /**
     * 更新业务状态，并保持相关数据的一致性。
     * @param id 业务参数，参与当前方法的校验、查询或状态变更。
     * @param request 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('admin:role:update')")
    @Operation(summary = "编辑角色", description = "更新角色名称、编码和权限关系。")
    public ApiResponse<RoleResponse> update(@Parameter(description = "角色 ID") @PathVariable Long id, @Valid @RequestBody SaveRoleRequest request) {
        return ApiResponse.ok(roleService.update(id, request));
    }

    /**
     * 删除或失效指定业务数据，并同步清理关联状态。
     * @param id 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('admin:role:delete')")
    @Operation(summary = "删除角色", description = "删除未绑定用户的角色。")
    public ApiResponse<Void> delete(@Parameter(description = "角色 ID") @PathVariable Long id) {
        roleService.delete(id);
        return ApiResponse.ok();
    }
}
