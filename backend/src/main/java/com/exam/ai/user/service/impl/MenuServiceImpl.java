package com.exam.ai.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.ai.common.exception.BusinessException;
import com.exam.ai.user.dto.SaveMenuRequest;
import com.exam.ai.user.entity.SysMenu;
import com.exam.ai.user.mapper.SysMenuMapper;
import com.exam.ai.user.service.MenuService;
import com.exam.ai.user.vo.ApiPathOptionResponse;
import com.exam.ai.user.vo.MenuResponse;
import com.exam.ai.util.CurrentUserUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * 菜单业务实现，负责菜单树查询、当前用户可见菜单裁剪和管理员有限字段维护。
 */
@Service
public class MenuServiceImpl implements MenuService {

    private static final long ROOT_PARENT_ID = 0L;
    private static final int ENABLED_STATUS = 1;
    private static final String API_PREFIX = "/api";
    private static final String ADMIN_API_SEGMENT = "admin";
    private static final int NORMAL_API_BASE_SEGMENTS = 3;
    private static final int ADMIN_API_BASE_SEGMENTS = 4;

    private final SysMenuMapper menuMapper;
    private final RequestMappingHandlerMapping requestMappingHandlerMapping;

    /**
     * 构造菜单业务实现并注入数据库访问组件和 Controller 映射表。
     *
     * @param menuMapper 菜单 Mapper，用于读取和更新脚本维护的菜单记录。
     * @param requestMappingHandlerMapping Spring MVC 接口映射表，用于扫描可绑定的 API 根路径。
     */
    public MenuServiceImpl(SysMenuMapper menuMapper,
                           @Qualifier("requestMappingHandlerMapping")
                           RequestMappingHandlerMapping requestMappingHandlerMapping) {
        this.menuMapper = menuMapper;
        this.requestMappingHandlerMapping = requestMappingHandlerMapping;
    }

    /**
     * 查询完整后台菜单树，供管理员查看脚本维护的菜单结构。
     *
     * @return 完整菜单树，按排序值和菜单 ID 稳定排序。
     */
    @Override
    public List<MenuResponse> tree() {
        List<SysMenu> menus = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .orderByAsc(SysMenu::getSortOrder)
                .orderByAsc(SysMenu::getId));
        return toTree(menus);
    }

    /**
     * 查询当前登录用户可见菜单树。
     *
     * @return 当前用户可见菜单树；分组菜单仅在存在可见子菜单时返回。
     * @throws BusinessException 当前线程不存在登录用户上下文时抛出未登录异常。
     */
    @Override
    public List<MenuResponse> currentUserMenus() {
        Set<String> permissions = Set.copyOf(CurrentUserUtils.requireCurrentUser().permissions());
        List<SysMenu> activeMenus = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getStatus, ENABLED_STATUS)
                .orderByAsc(SysMenu::getSortOrder)
                .orderByAsc(SysMenu::getId));

        // 先按完整层级建树，再递归裁剪叶子权限，避免叶子有权限但父分组被提前过滤。
        return filterVisibleMenus(toTree(activeMenus), permissions);
    }

    /**
     * 创建菜单节点。
     *
     * @param request 创建请求；path 为空时创建分组菜单。
     * @return 创建后的菜单节点。
     * @throws BusinessException 父菜单不存在、path 重复或分组菜单传入 API 路径时抛出。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MenuResponse create(SaveMenuRequest request) {
        SysMenu menu = new SysMenu();
        fillCreateFields(menu, request);
        menuMapper.insert(menu);
        return toResponse(menuMapper.selectById(menu.getId()), List.of());
    }

    /**
     * 更新管理员允许维护的菜单展示字段。
     *
     * @param id 菜单 ID，必须是已存在的脚本维护菜单。
     * @param request 更新请求，只允许修改名称、图标、排序、状态和页面 API 根路径。
     * @return 更新后的菜单节点。
     * @throws BusinessException 菜单不存在，或分组菜单被设置 API 路径时抛出。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MenuResponse update(Long id, SaveMenuRequest request) {
        SysMenu menu = requireMenu(id);
        if (isGroup(menu) && StringUtils.hasText(request.apiPath())) {
            throw BusinessException.badRequest("分组菜单不能设置 API 路径");
        }

        // 菜单结构、路由路径和权限码由迁移脚本维护，后台仅调整运营展示字段。
        menu.setMenuName(request.menuName());
        menu.setIcon(normalizeBlank(request.icon()));
        menu.setSortOrder(request.sortOrder());
        menu.setStatus(request.status());
        menu.setApiPath(isGroup(menu) ? null : normalizeBlank(request.apiPath()));
        menuMapper.updateById(menu);
        return toResponse(menuMapper.selectById(id), List.of());
    }

    /**
     * 扫描 Controller 映射，生成菜单可绑定的 API 根路径选项。
     *
     * @return 去重后的 API 根路径选项，名称优先使用 Controller 的 {@link Tag#name()}。
     */
    @Override
    public List<ApiPathOptionResponse> listApiPathOptions() {
        Map<String, String> options = new TreeMap<>();
        requestMappingHandlerMapping.getHandlerMethods().forEach((mappingInfo, handlerMethod) -> {
            String tagName = resolveTagName(handlerMethod);
            for (String path : extractApiBasePaths(mappingInfo)) {
                options.merge(path, tagName, this::mergeLabel);
            }
        });
        return options.entrySet().stream()
                .map(entry -> new ApiPathOptionResponse(entry.getValue(), entry.getKey()))
                .toList();
    }

    /**
     * 删除菜单节点，存在子菜单时拒绝删除。
     *
     * @param id 菜单 ID。
     * @throws BusinessException 菜单不存在或仍存在子菜单时抛出。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireMenu(id);
        Long children = menuMapper.selectCount(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, id));
        if (children != null && children > 0) {
            throw BusinessException.badRequest("存在子菜单，不能删除");
        }
        menuMapper.deleteById(id);
    }

    /**
     * 填充新增菜单字段，并执行分组菜单和叶子菜单的业务约束。
     *
     * @param menu 待创建的菜单实体。
     * @param request 菜单创建请求。
     * @throws BusinessException 父菜单不存在、path 重复或分组菜单字段非法时抛出。
     */
    private void fillCreateFields(SysMenu menu, SaveMenuRequest request) {
        Long parentId = request.parentId();
        if (parentId != null) {
            requireMenu(parentId);
        }
        String path = normalizeBlank(request.path());
        validatePathAvailable(path);

        boolean group = !StringUtils.hasText(path);
        if (group && StringUtils.hasText(request.apiPath())) {
            throw BusinessException.badRequest("分组菜单不能设置 API 路径");
        }

        // 新增菜单不维护前端组件标识；分组菜单以 path 为空作为唯一判定规则。
        menu.setParentId(parentId);
        menu.setMenuName(request.menuName());
        menu.setPath(path);
        menu.setIcon(normalizeBlank(request.icon()));
        menu.setSortOrder(request.sortOrder());
        menu.setStatus(request.status());
        menu.setApiPath(group ? null : normalizeBlank(request.apiPath()));
        menu.setPermissionCode(group ? null : normalizeBlank(request.permissionCode()));
    }

    /**
     * 校验叶子菜单页面路径未被未删除菜单占用。
     *
     * @param path 页面路径；为空时表示分组菜单，不做唯一性校验。
     * @throws BusinessException 路径已存在时抛出。
     */
    private void validatePathAvailable(String path) {
        if (!StringUtils.hasText(path)) {
            return;
        }
        Long count = menuMapper.selectCount(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getPath, path));
        if (count != null && count > 0) {
            throw BusinessException.badRequest("菜单路径已存在");
        }
    }

    /**
     * 根据菜单 ID 查询菜单，屏蔽不存在菜单继续进入更新流程。
     *
     * @param id 菜单 ID。
     * @return 数据库中的菜单实体。
     * @throws BusinessException 菜单不存在时抛出。
     */
    private SysMenu requireMenu(Long id) {
        SysMenu menu = menuMapper.selectById(id);
        if (menu == null) {
            throw BusinessException.badRequest("菜单不存在");
        }
        return menu;
    }

    /**
     * 将平铺菜单转换为树形菜单。
     *
     * @param menus 平铺菜单列表。
     * @return 树形菜单列表。
     */
    private List<MenuResponse> toTree(List<SysMenu> menus) {
        Map<Long, List<SysMenu>> children = menus.stream()
                .collect(Collectors.groupingBy(menu -> menu.getParentId() == null ? ROOT_PARENT_ID : menu.getParentId()));
        return buildTree(children, ROOT_PARENT_ID);
    }

    /**
     * 递归构建指定父菜单下的子树。
     *
     * @param children 按父菜单 ID 分组的菜单集合。
     * @param parentId 当前父菜单 ID。
     * @return 当前父菜单下的菜单节点。
     */
    private List<MenuResponse> buildTree(Map<Long, List<SysMenu>> children, Long parentId) {
        return children.getOrDefault(parentId, List.of()).stream()
                .sorted(Comparator.comparing(SysMenu::getSortOrder).thenComparing(SysMenu::getId))
                .map(menu -> toResponse(menu, buildTree(children, menu.getId())))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * 将菜单实体转换为前端菜单节点。
     *
     * @param menu 菜单实体。
     * @param children 已转换完成的子菜单。
     * @return 前端菜单节点。
     */
    private MenuResponse toResponse(SysMenu menu, List<MenuResponse> children) {
        return new MenuResponse(menu.getId(), menu.getParentId(), menu.getMenuName(), menu.getPath(), menu.getApiPath(),
                menu.getIcon(), menu.getSortOrder(), menu.getStatus(), menu.getPermissionCode(), children);
    }

    /**
     * 递归裁剪当前用户不可访问的菜单。
     *
     * @param menus 候选菜单树。
     * @param permissions 当前用户权限码集合。
     * @return 当前用户可见菜单树。
     */
    private List<MenuResponse> filterVisibleMenus(List<MenuResponse> menus, Set<String> permissions) {
        return menus.stream()
                .map(menu -> new MenuResponse(menu.id(), menu.parentId(), menu.menuName(), menu.path(), menu.apiPath(),
                        menu.icon(), menu.sortOrder(), menu.status(), menu.permissionCode(),
                        filterVisibleMenus(menu.children(), permissions)))
                .filter(menu -> isGroup(menu) ? !menu.children().isEmpty() : canAccessLeaf(menu, permissions))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * 判断叶子菜单是否满足当前用户权限。
     *
     * @param menu 叶子菜单节点。
     * @param permissions 当前用户权限码集合。
     * @return 无权限码或用户拥有权限码时返回 true。
     */
    private boolean canAccessLeaf(MenuResponse menu, Set<String> permissions) {
        return !StringUtils.hasText(menu.permissionCode()) || permissions.contains(menu.permissionCode());
    }

    /**
     * 判断实体菜单是否为分组菜单。
     *
     * @param menu 菜单实体。
     * @return {@code path} 为空时返回 true。
     */
    private boolean isGroup(SysMenu menu) {
        return !StringUtils.hasText(menu.getPath());
    }

    /**
     * 判断前端菜单节点是否为分组菜单。
     *
     * @param menu 前端菜单节点。
     * @return {@code path} 为空时返回 true。
     */
    private boolean isGroup(MenuResponse menu) {
        return !StringUtils.hasText(menu.path());
    }

    /**
     * 读取 Controller 的中文名称，缺失 {@code @Tag} 时回退到类名，保证下拉选项始终可展示。
     *
     * @param handlerMethod Spring MVC 处理方法。
     * @return Controller 中文名称或类名。
     */
    private String resolveTagName(HandlerMethod handlerMethod) {
        Tag tag = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), Tag.class);
        return tag == null || !StringUtils.hasText(tag.name()) ? handlerMethod.getBeanType().getSimpleName() : tag.name();
    }

    /**
     * 从接口映射信息中提取页面级 API 根路径。
     *
     * @param mappingInfo Spring MVC 映射信息。
     * @return 去重后的 API 根路径集合。
     */
    private Set<String> extractApiBasePaths(RequestMappingInfo mappingInfo) {
        Set<String> patterns = new LinkedHashSet<>();
        if (mappingInfo.getPathPatternsCondition() != null) {
            patterns.addAll(mappingInfo.getPathPatternsCondition().getPatternValues());
        }
        if (mappingInfo.getPatternsCondition() != null) {
            patterns.addAll(mappingInfo.getPatternsCondition().getPatterns());
        }
        return patterns.stream()
                .filter(path -> path.startsWith(API_PREFIX))
                .map(this::toApiBasePath)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 将具体接口路径折叠为菜单可绑定的 API 根路径。
     *
     * @param rawPath 具体接口路径，例如 {@code /api/questions/{id}/review}。
     * @return 页面主资源根路径，例如 {@code /api/questions}。
     */
    private String toApiBasePath(String rawPath) {
        String path = rawPath.endsWith("/") ? rawPath.substring(0, rawPath.length() - 1) : rawPath;
        String[] segments = path.split("/");
        int segmentCount = ADMIN_API_SEGMENT.equals(segments.length > 2 ? segments[2] : null)
                ? ADMIN_API_BASE_SEGMENTS
                : NORMAL_API_BASE_SEGMENTS;
        if (segments.length < segmentCount) {
            return path;
        }
        List<String> baseSegments = new ArrayList<>();
        for (int i = 1; i < segmentCount; i++) {
            baseSegments.add(segments[i]);
        }
        return "/" + String.join("/", baseSegments);
    }

    /**
     * 合并多个 Controller 对同一 API 根路径的展示名称，避免重复标签丢失。
     *
     * @param existing 已有标签。
     * @param incoming 新扫描到的标签。
     * @return 去重后的合并标签。
     */
    private String mergeLabel(String existing, String incoming) {
        if (!StringUtils.hasText(incoming) || existing.contains(incoming)) {
            return existing;
        }
        return existing + "/" + incoming;
    }

    /**
     * 将空白字符串标准化为空值，避免数据库保存无意义空格。
     *
     * @param value 待标准化字符串。
     * @return 非空文本或 {@code null}。
     */
    private String normalizeBlank(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
