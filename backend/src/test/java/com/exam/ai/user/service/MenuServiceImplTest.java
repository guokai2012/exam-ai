package com.exam.ai.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.exam.ai.common.exception.BusinessException;
import com.exam.ai.user.dto.SaveMenuRequest;
import com.exam.ai.user.entity.SysMenu;
import com.exam.ai.user.mapper.SysMenuMapper;
import com.exam.ai.user.service.impl.MenuServiceImpl;
import com.exam.ai.user.vo.ApiPathOptionResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

class MenuServiceImplTest {

    /**
     * 验证分组菜单不能保存 API 路径，防止分组被前端当作页面资源入口。
     */
    @Test
    void updateRejectsApiPathForGroupMenu() {
        SysMenuMapper menuMapper = mock(SysMenuMapper.class);
        RequestMappingHandlerMapping handlerMapping = mock(RequestMappingHandlerMapping.class);
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
     * 验证菜单 API 路径选项从 Controller 路径和 Tag 名称中生成，并将具体方法路径折叠为 API 根路径。
     *
     * @throws Exception 反射构造测试处理方法失败时抛出。
     */
    @Test
    void listApiPathOptionsScansControllerRootPaths() throws Exception {
        SysMenuMapper menuMapper = mock(SysMenuMapper.class);
        RequestMappingHandlerMapping handlerMapping = mock(RequestMappingHandlerMapping.class);
        when(handlerMapping.getHandlerMethods()).thenReturn(handlerMethods());
        MenuServiceImpl service = new MenuServiceImpl(menuMapper, handlerMapping);

        List<ApiPathOptionResponse> options = service.listApiPathOptions();

        assertThat(options).contains(
                new ApiPathOptionResponse("后台用户管理接口", "/api/admin/users"),
                new ApiPathOptionResponse("题库管理接口", "/api/questions")
        );
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
