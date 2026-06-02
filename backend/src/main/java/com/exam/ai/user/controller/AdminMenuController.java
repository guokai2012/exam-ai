package com.exam.ai.user.controller;

import com.exam.ai.common.result.ApiResponse;
import com.exam.ai.user.dto.SaveMenuRequest;
import com.exam.ai.user.dto.SyncMenuRequest;
import com.exam.ai.user.service.MenuService;
import com.exam.ai.user.service.MenuScanTokenService;
import com.exam.ai.user.vo.ApiPathOptionResponse;
import com.exam.ai.user.vo.MenuScanTokenResponse;
import com.exam.ai.user.vo.MenuResponse;
import com.exam.ai.user.vo.MenuSyncResponse;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台菜单管理 Controller，负责管理员维护菜单树、查询 API 路径候选项和删除菜单节点。
 */
@RestController
@RequestMapping("/api/admin/menus")
@Tag(name = "后台菜单管理接口", description = "管理员维护菜单树")
public class AdminMenuController {

    private final MenuService menuService;
    private final MenuScanTokenService menuScanTokenService;

    /**
     * 构造 AdminMenuController 实例并注入运行所需依赖。
     * @param menuService 菜单业务服务。
     * @param menuScanTokenService 菜单扫描临时 Token 服务。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public AdminMenuController(MenuService menuService, MenuScanTokenService menuScanTokenService) {
        this.menuService = menuService;
        this.menuScanTokenService = menuScanTokenService;
    }

    /**
     * 查询业务数据集合，并按调用场景组织返回结构。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @GetMapping
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
    @GetMapping("/api-path-options")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('admin:menu:update')")
    @Operation(summary = "菜单 API 路径选项", description = "扫描 Controller 的 API 根路径和接口标签，供菜单绑定页面主资源路径。")
    public ApiResponse<List<ApiPathOptionResponse>> apiPathOptions() {
        return ApiResponse.ok(menuService.listApiPathOptions());
    }

    /**
     * 获取菜单扫描临时 Token，同一用户会话 5 分钟内最多获取一次。
     *
     * @return 菜单扫描临时 Token 和过期时间。
     * @throws com.exam.ai.common.exception.BusinessException 当用户不是 ADMIN、无扫描权限或触发限流时抛出。
     */
    @PostMapping("/scan-token")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('admin:menu:scan')")
    @Operation(summary = "获取菜单扫描临时 Token", description = "签发菜单扫描同步专用短时 Token，同一用户会话 5 分钟内最多获取一次。")
    public ApiResponse<MenuScanTokenResponse> scanToken() {
        return ApiResponse.ok(menuScanTokenService.issueToken());
    }

    /**
     * 使用前端路由扫描结果同步菜单数据。
     *
     * @param token 菜单扫描临时 Token，请求头名称为 X-Menu-Scan-Token。
     * @param request 前端扫描得到的菜单树。
     * @return 菜单同步结果。
     * @throws com.exam.ai.common.exception.BusinessException 当临时 Token 无效、过期、重复使用或菜单结构非法时抛出。
     */
    @PostMapping("/scan-sync")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('admin:menu:scan')")
    @Operation(summary = "扫描同步菜单", description = "校验短时 Token 后，同步前端 Router 扫描得到的菜单数据。")
    public ApiResponse<MenuSyncResponse> scanSync(
            @RequestHeader(name = MenuScanTokenService.TOKEN_HEADER, required = false) String token,
            @Valid @RequestBody SyncMenuRequest request) {
        menuScanTokenService.consumeToken(token);
        return ApiResponse.ok(menuService.syncScannedMenus(request));
    }

    /**
     * 创建菜单节点；菜单不再维护前端组件标识。
     *
     * @param request 菜单创建请求。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当父菜单不存在、path 重复或分组字段非法时抛出。
     */
    @PostMapping
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
    @PutMapping("/{id}")
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
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('admin:menu:delete')")
    @Operation(summary = "删除菜单", description = "删除没有子菜单的菜单；存在子节点时拒绝删除。")
    public ApiResponse<Void> delete(@Parameter(description = "菜单 ID") @PathVariable Long id) {
        menuService.delete(id);
        return ApiResponse.ok();
    }
}
