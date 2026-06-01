package com.exam.ai.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.ai.common.exception.BusinessException;
import com.exam.ai.security.UserPrincipal;
import com.exam.ai.user.entity.SysMenu;
import com.exam.ai.user.dto.MenuResponse;
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

@Service
public class MenuService {

    private static final String MENU_GROUP_COMPONENT = "MenuGroup";

    private final SysMenuMapper menuMapper;
    private final AdminPermissionService permissionService;

    public MenuService(SysMenuMapper menuMapper, AdminPermissionService permissionService) {
        this.menuMapper = menuMapper;
        this.permissionService = permissionService;
    }

    public List<MenuResponse> tree() {
        return toTree(menuMapper.selectList(new LambdaQueryWrapper<SysMenu>().orderByAsc(SysMenu::getSortOrder).orderByAsc(SysMenu::getId)));
    }

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

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireMenu(id);
        Long children = menuMapper.selectCount(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, id));
        if (children != null && children > 0) {
            throw BusinessException.badRequest("存在子菜单，不能删除");
        }
        menuMapper.deleteById(id);
    }

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

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private SysMenu requireMenu(Long id) {
        SysMenu menu = menuMapper.selectById(id);
        if (menu == null) {
            throw BusinessException.badRequest("菜单不存在");
        }
        return menu;
    }

    private List<MenuResponse> toTree(List<SysMenu> menus) {
        Map<Long, List<SysMenu>> children = menus.stream().collect(Collectors.groupingBy(menu -> menu.getParentId() == null ? 0L : menu.getParentId()));
        return buildTree(children, 0L);
    }

    private List<MenuResponse> buildTree(Map<Long, List<SysMenu>> children, Long parentId) {
        return children.getOrDefault(parentId, List.of()).stream()
                .sorted(Comparator.comparing(SysMenu::getSortOrder).thenComparing(SysMenu::getId))
                .map(menu -> toResponse(menu, buildTree(children, menu.getId())))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private MenuResponse toResponse(SysMenu menu, List<MenuResponse> children) {
        return new MenuResponse(menu.getId(), menu.getParentId(), menu.getMenuName(), menu.getPath(), menu.getComponent(),
                menu.getIcon(), menu.getSortOrder(), menu.getStatus(), menu.getPermissionCode(), children);
    }

    private List<MenuResponse> pruneEmptyGroups(List<MenuResponse> menus) {
        return menus.stream()
                .map(menu -> new MenuResponse(menu.id(), menu.parentId(), menu.menuName(), menu.path(), menu.component(),
                        menu.icon(), menu.sortOrder(), menu.status(), menu.permissionCode(), pruneEmptyGroups(menu.children())))
                .filter(menu -> !MENU_GROUP_COMPONENT.equals(menu.component()) || !menu.children().isEmpty())
                .collect(Collectors.toCollection(ArrayList::new));
    }
}

