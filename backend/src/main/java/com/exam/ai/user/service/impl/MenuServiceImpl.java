package com.exam.ai.user.service.impl;

import com.exam.ai.user.service.AdminPermissionService;
import com.exam.ai.user.service.MenuService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.ai.common.exception.BusinessException;
import com.exam.ai.security.UserPrincipal;
import com.exam.ai.user.entity.SysMenu;
import com.exam.ai.user.vo.MenuResponse;
import com.exam.ai.user.dto.SaveMenuRequest;
import com.exam.ai.user.mapper.SysMenuMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * MenuServiceImpl 类，承载当前分层中的业务职责。
 */
@Service
public class MenuServiceImpl implements MenuService {

    private static final String MENU_GROUP_COMPONENT = "MenuGroup";

    private final SysMenuMapper menuMapper;
    private final AdminPermissionService permissionService;

    /**
     * 构造 MenuServiceImpl 实例并注入运行所需依赖。
     * @param menuMapper 业务参数，参与当前方法的校验、查询或状态变更。
     * @param permissionService 业务参数，参与当前方法的校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public MenuServiceImpl(SysMenuMapper menuMapper, AdminPermissionService permissionService) {
        this.menuMapper = menuMapper;
        this.permissionService = permissionService;
    }

    /**
     * 查询业务数据集合，并按调用场景组织返回结构。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public List<MenuResponse> tree() {
        return toTree(menuMapper.selectList(new LambdaQueryWrapper<SysMenu>().orderByAsc(SysMenu::getSortOrder).orderByAsc(SysMenu::getId)));
    }

    /**
     * 查询或解析业务数据，返回前端或内部流程需要的结果。
     * @param principal 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public List<MenuResponse> currentUserMenus(UserPrincipal principal) {
        Set<String> permissions = Set.copyOf(principal.permissions());
        List<SysMenu> menus = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getStatus, 1)
                .orderByAsc(SysMenu::getSortOrder)
                .orderByAsc(SysMenu::getId))
                .stream()
                .filter(menu -> menu.getPermissionCode() == null || permissions.contains(menu.getPermissionCode()))
                .toList();
        return pruneEmptyGroups(toTree(menus));
    }

    /**
     * 创建业务数据并完成必要的状态初始化。
     * @param request 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @Transactional(rollbackFor = Exception.class)
    public MenuResponse create(SaveMenuRequest request) {
        SysMenu menu = new SysMenu();
        fill(menu, request);
        menuMapper.insert(menu);
        if (!MENU_GROUP_COMPONENT.equals(menu.getComponent()) && !hasText(menu.getPermissionCode())) {
            menu.setPermissionCode(permissionService.generatedViewCode(menu));
            menuMapper.updateById(menu);
        }
        permissionService.syncMenuPermission(menu);
        return toResponse(menuMapper.selectById(menu.getId()), List.of());
    }

    /**
     * 更新业务状态，并保持相关数据的一致性。
     * @param id 业务参数，参与当前方法的校验、查询或状态变更。
     * @param request 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @Transactional(rollbackFor = Exception.class)
    public MenuResponse update(Long id, SaveMenuRequest request) {
        SysMenu menu = requireMenu(id);
        fill(menu, request);
        if (!MENU_GROUP_COMPONENT.equals(menu.getComponent()) && !hasText(menu.getPermissionCode())) {
            menu.setPermissionCode(permissionService.generatedViewCode(menu));
        }
        menuMapper.updateById(menu);
        permissionService.syncMenuPermission(menu);
        return toResponse(menuMapper.selectById(id), List.of());
    }

    /**
     * 删除或失效指定业务数据，并同步清理关联状态。
     * @param id 业务参数，参与当前方法的校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
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
     * 更新业务状态，并保持相关数据的一致性。
     * @param menu 业务参数，参与当前方法的校验、查询或状态变更。
     * @param request 业务参数，参与当前方法的校验、查询或状态变更。
     */
    private void fill(SysMenu menu, SaveMenuRequest request) {
        menu.setParentId(request.parentId());
        menu.setMenuName(request.menuName());
        menu.setPath(request.path());
        menu.setComponent(request.component());
        menu.setIcon(request.icon());
        menu.setSortOrder(request.sortOrder());
        menu.setStatus(request.status());
        menu.setPermissionCode(request.permissionCode());
    }

    /**
     * 执行当前业务步骤，维护调用方需要的处理结果。
     * @param value 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * 校验业务参数或状态，阻止非法流程继续执行。
     * @param id 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     */
    private SysMenu requireMenu(Long id) {
        SysMenu menu = menuMapper.selectById(id);
        if (menu == null) {
            throw BusinessException.badRequest("菜单不存在");
        }
        return menu;
    }

    /**
     * 转换业务对象，生成前端返回视图或内部传输结构。
     * @param menus 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     */
    private List<MenuResponse> toTree(List<SysMenu> menus) {
        Map<Long, List<SysMenu>> children = menus.stream().collect(Collectors.groupingBy(menu -> menu.getParentId() == null ? 0L : menu.getParentId()));
        return buildTree(children, 0L);
    }

    /**
     * 转换业务对象，生成前端返回视图或内部传输结构。
     * @param M 业务参数，参与当前方法的校验、查询或状态变更。
     * @param children 业务参数，参与当前方法的校验、查询或状态变更。
     * @param parentId 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     */
    private List<MenuResponse> buildTree(Map<Long, List<SysMenu>> children, Long parentId) {
        return children.getOrDefault(parentId, List.of()).stream()
                .sorted(Comparator.comparing(SysMenu::getSortOrder).thenComparing(SysMenu::getId))
                .map(menu -> toResponse(menu, buildTree(children, menu.getId())))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * 转换业务对象，生成前端返回视图或内部传输结构。
     * @param menu 业务参数，参与当前方法的校验、查询或状态变更。
     * @param children 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     */
    private MenuResponse toResponse(SysMenu menu, List<MenuResponse> children) {
        return new MenuResponse(menu.getId(), menu.getParentId(), menu.getMenuName(), menu.getPath(), menu.getComponent(),
                menu.getIcon(), menu.getSortOrder(), menu.getStatus(), menu.getPermissionCode(), children);
    }

    /**
     * 执行当前业务步骤，维护调用方需要的处理结果。
     * @param menus 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     */
    private List<MenuResponse> pruneEmptyGroups(List<MenuResponse> menus) {
        return menus.stream()
                .map(menu -> new MenuResponse(menu.id(), menu.parentId(), menu.menuName(), menu.path(), menu.component(),
                        menu.icon(), menu.sortOrder(), menu.status(), menu.permissionCode(), pruneEmptyGroups(menu.children())))
                .filter(menu -> !MENU_GROUP_COMPONENT.equals(menu.component()) || !menu.children().isEmpty())
                .collect(Collectors.toCollection(ArrayList::new));
    }
}

