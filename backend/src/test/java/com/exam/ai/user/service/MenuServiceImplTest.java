package com.exam.ai.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.exam.ai.common.exception.BusinessException;
import com.exam.ai.user.dto.SaveMenuRequest;
import com.exam.ai.user.dto.SyncMenuItemRequest;
import com.exam.ai.user.dto.SyncMenuRequest;
import com.exam.ai.user.entity.SysMenu;
import com.exam.ai.user.mapper.SysMenuMapper;
import com.exam.ai.user.service.impl.MenuServiceImpl;
import com.exam.ai.user.vo.ApiPathOptionResponse;
import com.exam.ai.user.vo.MenuSyncResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

class MenuServiceImplTest {

    private RequestMappingHandlerMapping handlerMapping;

    /**
     * 初始化测试用 MVC 映射依赖，菜单服务单元测试不启动 Spring 容器。
     */
    @BeforeEach
    void setUp() {
        handlerMapping = mock(RequestMappingHandlerMapping.class);
    }

    /**
     * 验证新增叶子菜单会保存页面路径、API 路径和权限码。
     */
    @Test
    void createLeafMenuPersistsRouteApiPathAndPermissionCode() {
        SysMenuMapper menuMapper = mock(SysMenuMapper.class);
        when(menuMapper.selectCount(ArgumentMatchers.<Wrapper<SysMenu>>any())).thenReturn(0L);
        when(menuMapper.insert(any(SysMenu.class))).thenAnswer(invocation -> {
            SysMenu menu = invocation.getArgument(0);
            menu.setId(10L);
            return 1;
        });
        when(menuMapper.selectById(10L)).thenAnswer(invocation -> {
            SysMenu menu = new SysMenu();
            menu.setId(10L);
            menu.setMenuName("扩展菜单");
            menu.setPath("/custom");
            menu.setApiPath("/api/custom");
            menu.setPermissionCode("custom:list");
            return menu;
        });
        MenuServiceImpl service = new MenuServiceImpl(menuMapper, handlerMapping);

        SaveMenuRequest request = SaveMenuRequest.builder()
                .menuName("扩展菜单")
                .path("/custom")
                .apiPath("/api/custom")
                .permissionCode("custom:list")
                .icon("Menu")
                .sortOrder(10)
                .status(1)
                .build();

        service.create(request);

        ArgumentCaptor<SysMenu> captor = ArgumentCaptor.forClass(SysMenu.class);
        verify(menuMapper).insert(captor.capture());
        assertThat(captor.getValue().getPath()).isEqualTo("/custom");
        assertThat(captor.getValue().getApiPath()).isEqualTo("/api/custom");
        assertThat(captor.getValue().getPermissionCode()).isEqualTo("custom:list");
    }

    /**
     * 验证分组菜单不能保存 API 路径，防止分组被前端当作页面资源入口。
     */
    @Test
    void updateRejectsApiPathForGroupMenu() {
        SysMenuMapper menuMapper = mock(SysMenuMapper.class);
        SysMenu groupMenu = new SysMenu();
        groupMenu.setId(1L);
        groupMenu.setMenuName("系统管理");
        groupMenu.setPath(null);
        when(menuMapper.selectById(1L)).thenReturn(groupMenu);
        MenuServiceImpl service = new MenuServiceImpl(menuMapper, handlerMapping);

        SaveMenuRequest request = SaveMenuRequest.builder()
                .menuName("系统管理")
                .icon("Setting")
                .sortOrder(10)
                .status(1)
                .apiPath("/api/admin/users")
                .build();

        assertThatThrownBy(() -> service.update(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("分组菜单不能设置 API 路径");
    }

    /**
     * 验证存在子菜单时拒绝删除父菜单，避免产生孤儿菜单。
     */
    @Test
    void deleteRejectsMenuWithChildren() {
        SysMenuMapper menuMapper = mock(SysMenuMapper.class);
        SysMenu parent = new SysMenu();
        parent.setId(1L);
        parent.setMenuName("父菜单");
        when(menuMapper.selectById(1L)).thenReturn(parent);
        when(menuMapper.selectCount(ArgumentMatchers.<Wrapper<SysMenu>>any())).thenReturn(1L);
        MenuServiceImpl service = new MenuServiceImpl(menuMapper, handlerMapping);

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("存在子菜单，不能删除");
    }

    /**
     * 验证菜单 API 路径选项从 Controller 路径和 Tag 名称中生成，并将具体方法路径折叠为 API 根路径。
     *
     * @throws Exception 反射构造测试处理方法失败时抛出。
     */
    @Test
    void listApiPathOptionsScansControllerRootPaths() throws Exception {
        SysMenuMapper menuMapper = mock(SysMenuMapper.class);
        when(handlerMapping.getHandlerMethods()).thenReturn(handlerMethods());
        MenuServiceImpl service = new MenuServiceImpl(menuMapper, handlerMapping);

        List<ApiPathOptionResponse> options = service.listApiPathOptions();

        assertThat(options).contains(
                new ApiPathOptionResponse("后台用户管理接口", "/api/admin/users"),
                new ApiPathOptionResponse("题库管理接口", "/api/questions")
        );
    }

    /**
     * 验证扫描同步只补齐空白开发字段，不覆盖管理员维护的展示字段。
     */
    @Test
    void syncScannedMenusFillsMissingFieldsWithoutOverwritingAdminFields() {
        SysMenuMapper menuMapper = mock(SysMenuMapper.class);
        Map<Long, SysMenu> store = new LinkedHashMap<>();
        SysMenu existing = menu(1L, "管理员改名", "/documents", null, null, "CustomIcon", 99, 0);
        store.put(existing.getId(), existing);
        when(menuMapper.selectList(ArgumentMatchers.<Wrapper<SysMenu>>any())).thenAnswer(invocation -> List.copyOf(store.values()));
        when(menuMapper.updateById(any(SysMenu.class))).thenAnswer(invocation -> {
            SysMenu menu = invocation.getArgument(0);
            store.put(menu.getId(), menu);
            return 1;
        });
        MenuServiceImpl service = new MenuServiceImpl(menuMapper, handlerMapping);

        MenuSyncResponse response = service.syncScannedMenus(new SyncMenuRequest(List.of(new SyncMenuItemRequest(
                "menu:/documents",
                "我的文档",
                "/documents",
                "/api/documents",
                "Document",
                10,
                1,
                "document:list",
                List.of()
        ))));

        assertThat(response.updated()).isEqualTo(1);
        assertThat(existing.getMenuName()).isEqualTo("管理员改名");
        assertThat(existing.getIcon()).isEqualTo("CustomIcon");
        assertThat(existing.getSortOrder()).isEqualTo(99);
        assertThat(existing.getStatus()).isZero();
        assertThat(existing.getMenuKey()).isEqualTo("menu:/documents");
        assertThat(existing.getApiPath()).isEqualTo("/api/documents");
        assertThat(existing.getPermissionCode()).isEqualTo("document:list");
    }

    /**
     * 验证扫描同步会创建缺失分组和子菜单，并清空分组菜单的 API 路径与权限码。
     */
    @Test
    void syncScannedMenusCreatesMissingGroupAndChildren() {
        SysMenuMapper menuMapper = mock(SysMenuMapper.class);
        Map<Long, SysMenu> store = new LinkedHashMap<>();
        long[] nextId = {1L};
        when(menuMapper.selectList(ArgumentMatchers.<Wrapper<SysMenu>>any())).thenAnswer(invocation -> List.copyOf(store.values()));
        when(menuMapper.insert(any(SysMenu.class))).thenAnswer(invocation -> {
            SysMenu menu = invocation.getArgument(0);
            menu.setId(nextId[0]++);
            store.put(menu.getId(), menu);
            return 1;
        });
        MenuServiceImpl service = new MenuServiceImpl(menuMapper, handlerMapping);

        MenuSyncResponse response = service.syncScannedMenus(new SyncMenuRequest(List.of(new SyncMenuItemRequest(
                "group:/questions",
                "题库管理",
                null,
                "/api/questions",
                "Collection",
                20,
                1,
                "question:list",
                List.of(new SyncMenuItemRequest(
                        "menu:/questions/available",
                        "可用题",
                        "/questions/available",
                        "/api/questions",
                        "Collection",
                        10,
                        1,
                        "question:list",
                        List.of()
                ))
        ))));

        assertThat(response.created()).isEqualTo(2);
        SysMenu group = store.get(1L);
        SysMenu child = store.get(2L);
        assertThat(group.getPath()).isNull();
        assertThat(group.getApiPath()).isNull();
        assertThat(group.getPermissionCode()).isNull();
        assertThat(child.getParentId()).isEqualTo(group.getId());
        assertThat(child.getPath()).isEqualTo("/questions/available");
    }

    /**
     * 构造测试用 Spring MVC 路由映射。
     *
     * @return 路由映射与处理方法集合。
     * @throws Exception 反射查找测试 Controller 方法失败时抛出。
     */
    private Map<RequestMappingInfo, HandlerMethod> handlerMethods() throws Exception {
        UserController userController = new UserController();
        QuestionController questionController = new QuestionController();
        Map<RequestMappingInfo, HandlerMethod> methods = new LinkedHashMap<>();
        methods.put(mapping("/api/admin/users/{id}/roles"), handler(userController, "roles"));
        methods.put(mapping("/api/questions/{id}/review"), handler(questionController, "review"));
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
    private HandlerMethod handler(Object controller, String methodName) throws Exception {
        Method method = controller.getClass().getDeclaredMethod(methodName);
        return new HandlerMethod(controller, method);
    }

    /**
     * 创建测试菜单实体。
     *
     * @param id 菜单 ID。
     * @param name 菜单名称。
     * @param path 页面路径。
     * @param apiPath API 根路径。
     * @param permissionCode 权限码。
     * @param icon 图标。
     * @param sortOrder 排序值。
     * @param status 状态。
     * @return 菜单实体。
     */
    private SysMenu menu(Long id, String name, String path, String apiPath, String permissionCode, String icon, Integer sortOrder, Integer status) {
        SysMenu menu = new SysMenu();
        menu.setId(id);
        menu.setMenuName(name);
        menu.setPath(path);
        menu.setApiPath(apiPath);
        menu.setPermissionCode(permissionCode);
        menu.setIcon(icon);
        menu.setSortOrder(sortOrder);
        menu.setStatus(status);
        return menu;
    }

    @Tag(name = "后台用户管理接口")
    private static class UserController {

        /**
         * 测试后台用户接口路径折叠。
         */
        @GetMapping("/api/admin/users/{id}/roles")
        void roles() {
        }
    }

    @Tag(name = "题库管理接口")
    private static class QuestionController {

        /**
         * 测试题库接口路径折叠。
         */
        @GetMapping("/api/questions/{id}/review")
        void review() {
        }
    }
}
