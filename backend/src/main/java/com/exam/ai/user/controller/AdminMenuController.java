package com.exam.ai.user.controller;

import com.exam.ai.common.api.ApiResponse;
import com.exam.ai.security.UserPrincipal;
import com.exam.ai.user.dto.MenuResponse;
import com.exam.ai.user.dto.SaveMenuRequest;
import com.exam.ai.user.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "后台菜单管理接口", description = "管理员维护菜单树，登录用户加载可见菜单")
public class AdminMenuController {

    private final MenuService menuService;

    public AdminMenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping("/api/menus/me")
    @Operation(summary = "当前用户菜单树", description = "按当前用户权限返回可见菜单树。")
    public ApiResponse<List<MenuResponse>> currentUserMenus(@Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(menuService.currentUserMenus(principal));
    }

    @GetMapping("/api/admin/menus")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('admin:menu:list')")
    @Operation(summary = "后台菜单树", description = "查询完整后台菜单树。")
    public ApiResponse<List<MenuResponse>> list() {
        return ApiResponse.ok(menuService.tree());
    }

    @PostMapping("/api/admin/menus")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('admin:menu:create')")
    @Operation(summary = "新建菜单", description = "创建菜单并自动同步菜单权限节点和默认查看权限。")
    public ApiResponse<MenuResponse> create(@Valid @RequestBody SaveMenuRequest request) {
        return ApiResponse.ok(menuService.create(request));
    }

    @PutMapping("/api/admin/menus/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('admin:menu:update')")
    @Operation(summary = "编辑菜单", description = "更新菜单并同步对应权限节点。")
    public ApiResponse<MenuResponse> update(@Parameter(description = "菜单 ID") @PathVariable Long id, @Valid @RequestBody SaveMenuRequest request) {
        return ApiResponse.ok(menuService.update(id, request));
    }

    @DeleteMapping("/api/admin/menus/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('admin:menu:delete')")
    @Operation(summary = "删除菜单", description = "删除没有子菜单的菜单。")
    public ApiResponse<Void> delete(@Parameter(description = "菜单 ID") @PathVariable Long id) {
        menuService.delete(id);
        return ApiResponse.ok();
    }
}
