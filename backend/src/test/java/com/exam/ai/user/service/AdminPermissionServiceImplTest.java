package com.exam.ai.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.exam.ai.system.service.NotificationService;
import com.exam.ai.user.entity.SysMenu;
import com.exam.ai.user.entity.SysPermission;
import com.exam.ai.user.entity.SysRolePermission;
import com.exam.ai.user.mapper.SysMenuMapper;
import com.exam.ai.user.mapper.SysPermissionMapper;
import com.exam.ai.user.mapper.SysRolePermissionMapper;
import com.exam.ai.user.service.impl.AdminPermissionServiceImpl;
import com.exam.ai.user.vo.PermissionScanResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

class AdminPermissionServiceImplTest {

    /**
     * 验证 Controller 扫描会使用 Operation summary 命名权限、合并重复权限码名称、
     * 对缺少 summary 的接口使用路径兜底，并删除本次扫描不存在的历史权限。
     *
     * @throws Exception 反射查找测试 Controller 方法失败时抛出。
     */
    @Test
    void scanControllerPermissionsSynchronizesByControllerMetadata() throws Exception {
        SysPermissionMapper permissionMapper = mock(SysPermissionMapper.class);
        SysRolePermissionMapper rolePermissionMapper = mock(SysRolePermissionMapper.class);
        SysMenuMapper menuMapper = mock(SysMenuMapper.class);
        NotificationService notificationService = mock(NotificationService.class);
        RequestMappingHandlerMapping handlerMapping = mock(RequestMappingHandlerMapping.class);
        Map<Long, SysPermission> store = new LinkedHashMap<>();
        long[] nextId = {1L};

        SysPermission stale = permission("old:permission", "过期权限", AdminPermissionService.TYPE_ACTION);
        stale.setId(99L);
        stale.setSystemGenerated(1);
        stale.setLastScannedAt(LocalDateTime.now().minusDays(1));
        store.put(stale.getId(), stale);

        when(permissionMapper.selectList(ArgumentMatchers.<Wrapper<SysPermission>>any())).thenAnswer(invocation -> List.copyOf(store.values()));
        when(permissionMapper.selectById(any(Serializable.class))).thenAnswer(invocation -> store.get(((Number) invocation.getArgument(0)).longValue()));
        when(permissionMapper.insert(any(SysPermission.class))).thenAnswer(invocation -> {
            SysPermission permission = invocation.getArgument(0);
            permission.setId(nextId[0]++);
            store.put(permission.getId(), permission);
            return 1;
        });
        when(permissionMapper.updateById(any(SysPermission.class))).thenAnswer(invocation -> {
            SysPermission permission = invocation.getArgument(0);
            store.put(permission.getId(), permission);
            return 1;
        });
        when(permissionMapper.deleteById(any(Serializable.class))).thenAnswer(invocation -> {
            store.remove(((Number) invocation.getArgument(0)).longValue());
            return 1;
        });
        when(rolePermissionMapper.delete(ArgumentMatchers.<Wrapper<SysRolePermission>>any())).thenReturn(1);
        when(menuMapper.selectList(ArgumentMatchers.<Wrapper<SysMenu>>any())).thenReturn(List.of(
                menu(1L, "测试菜单", "/test/one", "/api/test", 10),
                menu(2L, "测试副菜单", "/test/two", "/api/test", 20),
                menu(3L, "测试菜单", "/test/three", "/api/test", 30),
                menu(4L, "测试分组", null, "/api/test", 40)
        ));
        when(handlerMapping.getHandlerMethods()).thenReturn(handlerMethods());

        AdminPermissionServiceImpl service = new AdminPermissionServiceImpl(
                permissionMapper,
                rolePermissionMapper,
                menuMapper,
                notificationService,
                handlerMapping
        );

        PermissionScanResponse response = service.scanControllerPermissions();

        assertThat(response.created()).isEqualTo(4);
        assertThat(response.deleted()).isEqualTo(1);
        assertThat(store.values()).extracting(SysPermission::getPermissionCode)
                .anyMatch(code -> code.toString().startsWith("__controller:"))
                .contains("test:dup", "test:missing", "test:summary");
        assertThat(permissionByCode(store, controllerGroupCode("/api/test")).getPermissionName()).isEqualTo("测试菜单/测试副菜单");
        assertThat(permissionByCode(store, "test:summary").getPermissionName()).isEqualTo("测试摘要");
        assertThat(permissionByCode(store, "test:missing").getPermissionName()).isEqualTo("GET /api/test/missing");
        assertThat(permissionByCode(store, "test:dup").getPermissionName()).isEqualTo("重复一/重复二");
        verify(notificationService).createForRole(
                eq("ADMIN"),
                eq("权限扫描缺少接口摘要"),
                org.mockito.ArgumentMatchers.contains("GET /api/test/missing"),
                eq(NotificationService.TYPE_PERMISSION_SCAN_WARNING),
                eq(NotificationService.BUSINESS_PERMISSION_SCAN),
                eq(null)
        );
        verifyNoMoreInteractions(notificationService);
    }

    /**
     * 验证菜单 api_path 尚未绑定 Controller 根路径时，扫描分组名称会回退到 Controller Tag。
     *
     * @throws Exception 反射查找测试 Controller 方法失败时抛出。
     */
    @Test
    void scanControllerPermissionsFallsBackToControllerTagWhenMenuMissing() throws Exception {
        SysPermissionMapper permissionMapper = mock(SysPermissionMapper.class);
        SysRolePermissionMapper rolePermissionMapper = mock(SysRolePermissionMapper.class);
        SysMenuMapper menuMapper = mock(SysMenuMapper.class);
        NotificationService notificationService = mock(NotificationService.class);
        RequestMappingHandlerMapping handlerMapping = mock(RequestMappingHandlerMapping.class);
        Map<Long, SysPermission> store = new LinkedHashMap<>();
        long[] nextId = {1L};

        when(permissionMapper.selectList(ArgumentMatchers.<Wrapper<SysPermission>>any())).thenAnswer(invocation -> List.copyOf(store.values()));
        when(permissionMapper.selectById(any(Serializable.class))).thenAnswer(invocation -> store.get(((Number) invocation.getArgument(0)).longValue()));
        when(permissionMapper.insert(any(SysPermission.class))).thenAnswer(invocation -> {
            SysPermission permission = invocation.getArgument(0);
            permission.setId(nextId[0]++);
            store.put(permission.getId(), permission);
            return 1;
        });
        when(permissionMapper.updateById(any(SysPermission.class))).thenAnswer(invocation -> {
            SysPermission permission = invocation.getArgument(0);
            store.put(permission.getId(), permission);
            return 1;
        });
        when(menuMapper.selectList(ArgumentMatchers.<Wrapper<SysMenu>>any())).thenReturn(List.of());
        when(handlerMapping.getHandlerMethods()).thenReturn(Map.of(mapping("/api/test/summary"), handler(new DemoController(), "summary")));

        AdminPermissionServiceImpl service = new AdminPermissionServiceImpl(
                permissionMapper,
                rolePermissionMapper,
                menuMapper,
                notificationService,
                handlerMapping
        );

        PermissionScanResponse response = service.scanControllerPermissions();

        assertThat(response.created()).isEqualTo(2);
        assertThat(permissionByCode(store, controllerGroupCode("/api/test")).getPermissionName()).isEqualTo("测试接口");
    }

    /**
     * 验证权限分组编码基于 Controller 根路径保持稳定，菜单后续补齐时只更新名称不重建分组。
     *
     * @throws Exception 反射查找测试 Controller 方法失败时抛出。
     */
    @Test
    void scanControllerPermissionsUpdatesExistingPathGroupName() throws Exception {
        SysPermissionMapper permissionMapper = mock(SysPermissionMapper.class);
        SysRolePermissionMapper rolePermissionMapper = mock(SysRolePermissionMapper.class);
        SysMenuMapper menuMapper = mock(SysMenuMapper.class);
        NotificationService notificationService = mock(NotificationService.class);
        RequestMappingHandlerMapping handlerMapping = mock(RequestMappingHandlerMapping.class);
        Map<Long, SysPermission> store = new LinkedHashMap<>();

        SysPermission group = permission(controllerGroupCode("/api/test"), "测试接口", AdminPermissionService.TYPE_GROUP);
        group.setId(1L);
        store.put(group.getId(), group);
        SysPermission action = permission("test:summary", "测试摘要", AdminPermissionService.TYPE_ACTION);
        action.setId(2L);
        action.setParentId(group.getId());
        store.put(action.getId(), action);

        when(permissionMapper.selectList(ArgumentMatchers.<Wrapper<SysPermission>>any())).thenAnswer(invocation -> List.copyOf(store.values()));
        when(permissionMapper.updateById(any(SysPermission.class))).thenAnswer(invocation -> {
            SysPermission permission = invocation.getArgument(0);
            store.put(permission.getId(), permission);
            return 1;
        });
        when(menuMapper.selectList(ArgumentMatchers.<Wrapper<SysMenu>>any())).thenReturn(List.of(menu(1L, "菜单补齐名称", "/test/summary", "/api/test", 10)));
        when(handlerMapping.getHandlerMethods()).thenReturn(Map.of(mapping("/api/test/summary"), handler(new DemoController(), "summary")));

        AdminPermissionServiceImpl service = new AdminPermissionServiceImpl(
                permissionMapper,
                rolePermissionMapper,
                menuMapper,
                notificationService,
                handlerMapping
        );

        PermissionScanResponse response = service.scanControllerPermissions();

        assertThat(response.created()).isZero();
        assertThat(response.updated()).isEqualTo(2);
        assertThat(permissionByCode(store, controllerGroupCode("/api/test")).getId()).isEqualTo(1L);
        assertThat(permissionByCode(store, controllerGroupCode("/api/test")).getPermissionName()).isEqualTo("菜单补齐名称");
    }

    /**
     * 创建测试权限实体。
     *
     * @param code 权限码。
     * @param name 权限名称。
     * @param type 权限类型。
     * @return 权限实体。
     */
    private SysPermission permission(String code, String name, String type) {
        SysPermission permission = new SysPermission();
        permission.setPermissionCode(code);
        permission.setPermissionName(name);
        permission.setPermissionType(type);
        return permission;
    }

    /**
     * 创建测试菜单实体。
     *
     * @param id 菜单 ID。
     * @param name 菜单名称。
     * @param path 前端页面路径。
     * @param apiPath 页面主资源 API 根路径。
     * @param sortOrder 排序值。
     * @return 菜单实体。
     */
    private SysMenu menu(Long id, String name, String path, String apiPath, Integer sortOrder) {
        SysMenu menu = new SysMenu();
        menu.setId(id);
        menu.setMenuName(name);
        menu.setPath(path);
        menu.setApiPath(apiPath);
        menu.setSortOrder(sortOrder);
        return menu;
    }

    /**
     * 生成测试中预期的 Controller 分组内部权限码。
     *
     * @param basePath Controller 类级 RequestMapping 根路径。
     * @return 分组权限码。
     */
    private String controllerGroupCode(String basePath) {
        return "__controller:" + Integer.toHexString(basePath.hashCode());
    }

    /**
     * 按权限码读取内存权限实体。
     *
     * @param store 内存权限表。
     * @param code 权限码。
     * @return 匹配的权限实体。
     */
    private SysPermission permissionByCode(Map<Long, SysPermission> store, String code) {
        return store.values().stream()
                .filter(permission -> code.equals(permission.getPermissionCode()))
                .findFirst()
                .orElseThrow();
    }

    /**
     * 构造测试用 Spring MVC 路由映射。
     *
     * @return 路由映射与处理方法集合。
     * @throws Exception 反射查找测试 Controller 方法失败时抛出。
     */
    private Map<RequestMappingInfo, HandlerMethod> handlerMethods() throws Exception {
        DemoController controller = new DemoController();
        Map<RequestMappingInfo, HandlerMethod> methods = new LinkedHashMap<>();
        methods.put(mapping("/api/test/dup-a"), handler(controller, "duplicateOne"));
        methods.put(mapping("/api/test/dup-b"), handler(controller, "duplicateTwo"));
        methods.put(mapping("/api/test/missing"), handler(controller, "missingSummary"));
        methods.put(mapping("/api/test/summary"), handler(controller, "summary"));
        return methods;
    }

    /**
     * 创建 GET 路由映射。
     *
     * @param path 接口路径。
     * @return 路由映射。
     */
    private RequestMappingInfo mapping(String path) {
        return RequestMappingInfo.paths(path).methods(RequestMethod.GET).build();
    }

    /**
     * 创建测试处理方法。
     *
     * @param controller 测试 Controller 实例。
     * @param methodName 方法名。
     * @return Spring MVC 处理方法。
     * @throws Exception 反射查找方法失败时抛出。
     */
    private HandlerMethod handler(DemoController controller, String methodName) throws Exception {
        Method method = DemoController.class.getDeclaredMethod(methodName);
        return new HandlerMethod(controller, method);
    }

    @RequestMapping("/api/test")
    @Tag(name = "测试接口")
    private static class DemoController {

        /**
         * 测试标准 summary 权限扫描。
         */
        @GetMapping("/summary")
        @PreAuthorize("hasAuthority('test:summary')")
        @Operation(summary = "测试摘要")
        void summary() {
        }

        /**
         * 测试缺少 summary 时使用路径兜底。
         */
        @GetMapping("/missing")
        @PreAuthorize("hasAuthority('test:missing')")
        void missingSummary() {
        }

        /**
         * 测试重复权限码第一个名称片段。
         */
        @GetMapping("/dup-a")
        @PreAuthorize("hasAuthority('test:dup')")
        @Operation(summary = "重复一")
        void duplicateOne() {
        }

        /**
         * 测试重复权限码第二个名称片段。
         */
        @GetMapping("/dup-b")
        @PreAuthorize("hasAuthority('test:dup')")
        @Operation(summary = "重复二")
        void duplicateTwo() {
        }
    }
}
