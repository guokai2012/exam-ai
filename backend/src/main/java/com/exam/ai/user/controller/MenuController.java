package com.exam.ai.user.controller;

import com.exam.ai.common.result.ApiResponse;
import com.exam.ai.user.service.MenuService;
import com.exam.ai.user.vo.MenuResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录用户菜单 Controller，负责返回当前用户可访问的菜单树。
 */
@RestController
@RequestMapping("/api/menus")
@Tag(name = "菜单接口", description = "登录用户加载可见菜单")
public class MenuController {

    private final MenuService menuService;

    /**
     * 构造登录用户菜单 Controller。
     *
     * @param menuService 菜单业务服务，用于按当前用户权限生成菜单树。
     */
    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    /**
     * 查询当前登录用户可见菜单树。
     *
     * @return 当前用户可见菜单树。
     */
    @GetMapping("/me")
    @Operation(summary = "当前用户菜单树", description = "按当前用户权限返回可见菜单树。")
    public ApiResponse<List<MenuResponse>> currentUserMenus() {
        return ApiResponse.ok(menuService.currentUserMenus());
    }
}
