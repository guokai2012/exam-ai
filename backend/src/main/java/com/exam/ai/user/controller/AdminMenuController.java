package com.exam.ai.user.controller;

import com.exam.ai.common.result.ApiResponse;
import com.exam.ai.user.vo.ApiPathOptionResponse;
import com.exam.ai.user.vo.MenuResponse;
import com.exam.ai.user.dto.SaveMenuRequest;
import com.exam.ai.user.service.MenuService;
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
 * AdminMenuController 类，当前分层的业务组件，负责本模块对应的请求、服务或数据模型职责。
 */
@RestController
@RequestMapping("/api")
@Tag(name = "后台菜单管理接口", description = "管理员维护菜单树，登录用户加载可见菜单")
public class AdminMenuController {

    private final MenuService menuService;

    /**
     * 构造 AdminMenuController 实例并注入运行所需依赖。
     * @param menuService 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public AdminMenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    /**
     * 查询或解析业务数据，返回前端或内部流程需要的结果。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @GetMapping("/menus/me")
    @Operation(summary = "当前用户菜单树", description = "按当前用户权限返回可见菜单树。")
    public ApiResponse<List<MenuResponse>> currentUserMenus() {
        return ApiResponse.ok(menuService.currentUserMenus());
    }

    /**
     * 查询业务数据集合，并按调用场景组织返回结构。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @GetMapping("/admin/menus")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('admin:menu:list')")
    @Operation(summary = "后台菜单树", description = "查询完整后台菜单树。")
    public ApiResponse<List<MenuResponse>> list() {
        return ApiResponse.ok(menuService.tree());
    }

    /**
     * 查询菜单可绑定的 API 根路径选项。
     *
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @GetMapping("/admin/menus/api-path-options")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('admin:menu:update')")
    @Operation(summary = "菜单 API 路径选项", description = "扫描 Controller 的 API 根路径和接口标签，供菜单绑定页面主资源路径。")
    public ApiResponse<List<ApiPathOptionResponse>> apiPathOptions() {
        return ApiResponse.ok(menuService.listApiPathOptions());
    }

    /**
     * 创建菜单节点；菜单不再维护前端组件标识。
     *
     * @param request 菜单创建请求。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当父菜单不存在、path 重复或分组字段非法时抛出。
     */
    @PostMapping("/admin/menus")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('admin:menu:create')")
    @Operation(summary = "新建菜单", description = "创建菜单节点；path 为空表示分组菜单，分组菜单不能设置 API 路径。")
    public ApiResponse<MenuResponse> create(@Valid @RequestBody SaveMenuRequest request) {
        return ApiResponse.ok(menuService.create(request));
    }

    /**
     * 更新业务状态，并保持相关数据的一致性。
     * @param id 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param request 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @PutMapping("/admin/menus/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('admin:menu:update')")
    @Operation(summary = "编辑菜单", description = "只允许更新菜单名称、图标、排序、状态和页面主资源 API 路径。")
    public ApiResponse<MenuResponse> update(@Parameter(description = "菜单 ID") @PathVariable Long id, @Valid @RequestBody SaveMenuRequest request) {
        return ApiResponse.ok(menuService.update(id, request));
    }

    /**
     * 删除没有子节点的菜单。
     *
     * @param id 菜单 ID。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当菜单不存在或仍存在子菜单时抛出。
     */
    @DeleteMapping("/admin/menus/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('admin:menu:delete')")
    @Operation(summary = "删除菜单", description = "删除没有子菜单的菜单；存在子节点时拒绝删除。")
    public ApiResponse<Void> delete(@Parameter(description = "菜单 ID") @PathVariable Long id) {
        menuService.delete(id);
        return ApiResponse.ok();
    }
}
