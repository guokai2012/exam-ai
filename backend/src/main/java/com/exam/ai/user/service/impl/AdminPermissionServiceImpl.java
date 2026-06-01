package com.exam.ai.user.service.impl;

import com.exam.ai.user.service.AdminPermissionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.exam.ai.common.exception.BusinessException;
import com.exam.ai.user.entity.SysMenu;
import com.exam.ai.user.entity.SysPermission;
import com.exam.ai.user.entity.SysRolePermission;
import com.exam.ai.user.vo.PermissionResponse;
import com.exam.ai.user.vo.PermissionScanResponse;
import com.exam.ai.user.dto.SavePermissionRequest;
import com.exam.ai.user.mapper.SysMenuMapper;
import com.exam.ai.user.mapper.SysPermissionMapper;
import com.exam.ai.user.mapper.SysRolePermissionMapper;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * 后台权限管理服务实现，负责权限树查询、动作权限维护、Controller 权限扫描和菜单权限节点同步。
 *
 * <p>权限树由分组节点、菜单节点、菜单查看权限和接口动作权限组成。菜单节点由菜单数据同步生成，
 * 接口动作权限可由扫描任务生成，也允许管理员手动维护未扫描到的特殊权限。</p>
 */
@Service
public class AdminPermissionServiceImpl implements AdminPermissionService {

    public static final String TYPE_GROUP = "GROUP";
    public static final String TYPE_MENU = "MENU";
    public static final String TYPE_VIEW = "VIEW";
    public static final String TYPE_ACTION = "ACTION";

    private static final String MENU_GROUP_COMPONENT = "MenuGroup";
    private static final String UNCATEGORIZED_CODE = "__uncategorized";
    private static final int DEFAULT_ACTION_SORT_ORDER = 100;
    private static final int DEFAULT_VIEW_SORT_ORDER = 0;
    private static final int UNCATEGORIZED_SORT_ORDER = 9999;
    private static final Pattern HAS_AUTHORITY = Pattern.compile("hasAuthority\\(\\s*['\"]([^'\"]+)['\"]\\s*\\)");
    private static final Pattern HAS_ANY_AUTHORITY = Pattern.compile("hasAnyAuthority\\(([^)]*)\\)");
    private static final Pattern QUOTED_VALUE = Pattern.compile("['\"]([^'\"]+)['\"]");

    private final SysPermissionMapper permissionMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysMenuMapper menuMapper;
    private final RequestMappingHandlerMapping handlerMapping;

    /**
     * 构造权限管理服务。
     *
     * @param permissionMapper 权限表访问器。
     * @param rolePermissionMapper 角色权限关系访问器，用于删除权限时清理授权关系。
     * @param menuMapper 菜单表访问器，用于把菜单结构同步为权限树节点。
     * @param handlerMapping Spring MVC 路由映射，用于扫描 Controller 上的权限注解。
     */
    public AdminPermissionServiceImpl(SysPermissionMapper permissionMapper,
                                  SysRolePermissionMapper rolePermissionMapper,
                                  SysMenuMapper menuMapper,
                                  RequestMappingHandlerMapping handlerMapping) {
        this.permissionMapper = permissionMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.menuMapper = menuMapper;
        this.handlerMapping = handlerMapping;
    }

    /**
     * 查询完整权限树。
     *
     * @return 按菜单、分组和动作权限组织后的权限树。
     */
    public List<PermissionResponse> list() {
        // 未归类节点兜底承载无法匹配菜单的动作权限，保证权限树不会出现孤儿节点。
        ensureUncategorizedRoot();
        return toTree(permissionMapper.selectList(new LambdaQueryWrapper<SysPermission>()
                .orderByAsc(SysPermission::getSortOrder)
                .orderByAsc(SysPermission::getId)));
    }

    /**
     * 手动创建动作权限。
     *
     * @param request 权限保存请求，包含父节点、权限码、权限名称和排序值。
     * @return 新建后的动作权限视图对象。
     * @throws BusinessException 当权限码已存在或父节点不是有效权限节点时抛出。
     */
    @Transactional(rollbackFor = Exception.class)
    public PermissionResponse create(SavePermissionRequest request) {
        ensureCodeAvailable(request.permissionCode(), null);
        SysPermission permission = new SysPermission();
        permission.setParentId(resolveParentId(request.parentId()));
        permission.setPermissionCode(request.permissionCode());
        permission.setPermissionName(request.permissionName());
        permission.setPermissionType(TYPE_ACTION);
        permission.setSortOrder(request.sortOrder() == null ? DEFAULT_ACTION_SORT_ORDER : request.sortOrder());
        permission.setSystemGenerated(0);
        permissionMapper.insert(permission);
        return toResponse(permissionMapper.selectById(permission.getId()), List.of());
    }

    /**
     * 编辑可维护的动作权限。
     *
     * @param id 权限 ID。
     * @param request 权限保存请求，包含新的父节点、权限码、权限名称和排序值。
     * @return 更新后的动作权限视图对象。
     * @throws BusinessException 当权限不存在、系统生成节点被编辑、权限码重复或父节点非法时抛出。
     */
    @Transactional(rollbackFor = Exception.class)
    public PermissionResponse update(Long id, SavePermissionRequest request) {
        SysPermission permission = requirePermission(id);
        if (TYPE_GROUP.equals(permission.getPermissionType()) || TYPE_MENU.equals(permission.getPermissionType())) {
            throw BusinessException.badRequest("菜单权限节点不能手动编辑");
        }
        ensureCodeAvailable(request.permissionCode(), id);
        permission.setParentId(resolveParentId(request.parentId()));
        permission.setPermissionCode(request.permissionCode());
        permission.setPermissionName(request.permissionName());
        permission.setSortOrder(request.sortOrder() == null ? permission.getSortOrder() : request.sortOrder());
        permissionMapper.updateById(permission);
        return toResponse(permissionMapper.selectById(id), List.of());
    }

    /**
     * 删除可维护的动作权限，并同步清理角色授权关系。
     *
     * @param id 权限 ID。
     * @throws BusinessException 当权限不存在、权限为系统生成节点或仍存在子权限时抛出。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysPermission permission = requirePermission(id);
        if (TYPE_GROUP.equals(permission.getPermissionType()) || TYPE_MENU.equals(permission.getPermissionType())
                || TYPE_VIEW.equals(permission.getPermissionType())) {
            throw BusinessException.badRequest("系统生成权限不能手动删除");
        }
        Long children = permissionMapper.selectCount(new LambdaQueryWrapper<SysPermission>().eq(SysPermission::getParentId, id));
        if (children != null && children > 0) {
            throw BusinessException.conflict("存在子权限，不能删除");
        }
        deletePermission(permission);
    }

    /**
     * 扫描所有 Controller 上的权限表达式并同步动作权限。
     *
     * @return 本次扫描新增、更新和删除的动作权限数量。
     */
    @Transactional(rollbackFor = Exception.class)
    public PermissionScanResponse scanControllerPermissions() {
        ensureUncategorizedRoot();
        List<SysMenu> menus = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .orderByAsc(SysMenu::getSortOrder)
                .orderByAsc(SysMenu::getId));
        Map<String, EndpointPermission> scanned = scanEndpointPermissions();
        Map<String, SysPermission> existingByCode = permissionMapper.selectList(new LambdaQueryWrapper<SysPermission>())
                .stream()
                .collect(Collectors.toMap(SysPermission::getPermissionCode, permission -> permission, (left, right) -> left));
        LocalDateTime now = LocalDateTime.now();
        int created = 0;
        int updated = 0;

        for (EndpointPermission endpointPermission : scanned.values()) {
            SysPermission existing = existingByCode.get(endpointPermission.permissionCode());
            SysMenu menu = bestMenuForEndpoint(endpointPermission.path(), menus);
            Long parentId = menu == null ? ensureUncategorizedRoot().getId() : ensureMenuPermissionNode(menu).getId();
            if (existing == null) {
                // 新扫描到的动作权限挂到最匹配的菜单节点；无法匹配时挂到未归类节点，方便管理员后续整理。
                SysPermission permission = new SysPermission();
                permission.setParentId(parentId);
                permission.setMenuId(menu == null ? null : menu.getId());
                permission.setPermissionCode(endpointPermission.permissionCode());
                permission.setPermissionName(endpointPermission.displayName());
                permission.setPermissionType(TYPE_ACTION);
                permission.setSortOrder(DEFAULT_ACTION_SORT_ORDER);
                permission.setSystemGenerated(1);
                permission.setLastScannedAt(now);
                permissionMapper.insert(permission);
                created++;
            } else {
                // 菜单查看权限不能被接口扫描覆盖为动作权限，只刷新动作权限的归属和扫描时间。
                boolean isView = TYPE_VIEW.equals(existing.getPermissionType());
                existing.setParentId(isView ? existing.getParentId() : parentId);
                existing.setMenuId(isView ? existing.getMenuId() : menu == null ? null : menu.getId());
                existing.setPermissionType(isView ? TYPE_VIEW : TYPE_ACTION);
                existing.setSystemGenerated(1);
                existing.setLastScannedAt(now);
                permissionMapper.updateById(existing);
                updated++;
            }
        }

        Set<String> currentCodes = scanned.keySet();
        // 上次扫描生成但本次不再出现在 Controller 的动作权限视为过期，删除时同步清理角色授权关系。
        List<SysPermission> stalePermissions = permissionMapper.selectList(new LambdaQueryWrapper<SysPermission>()
                        .eq(SysPermission::getPermissionType, TYPE_ACTION)
                        .eq(SysPermission::getSystemGenerated, 1)
                        .isNotNull(SysPermission::getLastScannedAt))
                .stream()
                .filter(permission -> !currentCodes.contains(permission.getPermissionCode()))
                .toList();
        stalePermissions.forEach(this::deletePermission);
        return new PermissionScanResponse(created, updated, stalePermissions.size());
    }

    /**
     * 将菜单记录同步为权限树中的菜单节点和查看权限。
     *
     * @param menu 菜单记录，包含路径、组件、权限码和父菜单关系。
     * @throws BusinessException 当菜单绑定的权限码与已有权限冲突时抛出。
     */
    @Transactional(rollbackFor = Exception.class)
    public void syncMenuPermission(SysMenu menu) {
        SysPermission menuNode = ensureMenuPermissionNode(menu);
        if (!MENU_GROUP_COMPONENT.equals(menu.getComponent()) && hasText(menu.getPermissionCode())) {
            SysPermission viewPermission = permissionMapper.selectOne(new LambdaQueryWrapper<SysPermission>()
                    .eq(SysPermission::getMenuId, menu.getId())
                    .eq(SysPermission::getPermissionType, TYPE_VIEW)
                    .last("LIMIT 1"));
            if (viewPermission == null) {
                viewPermission = permissionMapper.selectOne(new LambdaQueryWrapper<SysPermission>()
                        .eq(SysPermission::getPermissionCode, menu.getPermissionCode())
                        .last("LIMIT 1"));
            }
            if (viewPermission == null) {
                viewPermission = new SysPermission();
                viewPermission.setPermissionCode(menu.getPermissionCode());
                viewPermission.setPermissionName("查看");
                viewPermission.setSystemGenerated(1);
            } else if (!Objects.equals(viewPermission.getPermissionCode(), menu.getPermissionCode())) {
                ensureCodeAvailable(menu.getPermissionCode(), viewPermission.getId());
                viewPermission.setPermissionCode(menu.getPermissionCode());
            }
            viewPermission.setParentId(menuNode.getId());
            viewPermission.setMenuId(menu.getId());
            viewPermission.setPermissionType(TYPE_VIEW);
            viewPermission.setSortOrder(DEFAULT_VIEW_SORT_ORDER);
            viewPermission.setSystemGenerated(1);
            if (viewPermission.getId() == null) {
                permissionMapper.insert(viewPermission);
            } else {
                permissionMapper.updateById(viewPermission);
            }
        }
    }

    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param menu 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public String generatedViewCode(SysMenu menu) {
        String base = menu.getPath() == null ? "" : menu.getPath().trim();
        base = base.replaceFirst("^/+", "").replaceAll("[^A-Za-z0-9]+", ":").replaceAll("^:+|:+$", "");
        if (!hasText(base)) {
            base = "menu:" + menu.getId();
        }
        return base.toLowerCase() + ":view";
    }

    /**
     * 更新业务状态，并保持相关数据的一致性。
     * @return 封装后的业务处理结果。
     */
    private Map<String, EndpointPermission> scanEndpointPermissions() {
        Map<String, EndpointPermission> permissions = new LinkedHashMap<>();
        handlerMapping.getHandlerMethods().forEach((mappingInfo, handlerMethod) -> {
            Set<String> authorityCodes = extractAuthorityCodes(handlerMethod);
            if (authorityCodes.isEmpty()) {
                return;
            }
            List<String> paths = paths(mappingInfo);
            List<String> methods = methods(mappingInfo);
            for (String authorityCode : authorityCodes) {
                permissions.putIfAbsent(authorityCode, new EndpointPermission(
                        authorityCode,
                        methods.get(0),
                        paths.get(0)
                ));
            }
        });
        return permissions;
    }

    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param handlerMethod 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     */
    private Set<String> extractAuthorityCodes(HandlerMethod handlerMethod) {
        Set<String> codes = new LinkedHashSet<>();
        Method method = handlerMethod.getMethod();
        PreAuthorize methodAuthorize = AnnotatedElementUtils.findMergedAnnotation(method, PreAuthorize.class);
        PreAuthorize classAuthorize = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), PreAuthorize.class);
        if (classAuthorize != null) {
            collectAuthorityCodes(classAuthorize.value(), codes);
        }
        if (methodAuthorize != null) {
            collectAuthorityCodes(methodAuthorize.value(), codes);
        }
        return codes;
    }

    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param expression 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param codes 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     */
    private void collectAuthorityCodes(String expression, Set<String> codes) {
        Matcher authorityMatcher = HAS_AUTHORITY.matcher(expression);
        while (authorityMatcher.find()) {
            codes.add(authorityMatcher.group(1));
        }
        Matcher anyAuthorityMatcher = HAS_ANY_AUTHORITY.matcher(expression);
        while (anyAuthorityMatcher.find()) {
            Matcher quotedMatcher = QUOTED_VALUE.matcher(anyAuthorityMatcher.group(1));
            while (quotedMatcher.find()) {
                codes.add(quotedMatcher.group(1));
            }
        }
    }

    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param mappingInfo 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     */
    private List<String> paths(RequestMappingInfo mappingInfo) {
        Set<String> values = new LinkedHashSet<>();
        if (mappingInfo.getPathPatternsCondition() != null) {
            values.addAll(mappingInfo.getPathPatternsCondition().getPatternValues());
        }
        if (mappingInfo.getPatternsCondition() != null) {
            values.addAll(mappingInfo.getPatternsCondition().getPatterns());
        }
        if (values.isEmpty()) {
            values.add("/");
        }
        return values.stream().sorted().toList();
    }

    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param mappingInfo 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     */
    private List<String> methods(RequestMappingInfo mappingInfo) {
        Set<RequestMethod> requestMethods = mappingInfo.getMethodsCondition().getMethods();
        if (requestMethods.isEmpty()) {
            return List.of("ANY");
        }
        return requestMethods.stream().map(RequestMethod::name).sorted().toList();
    }

    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param endpointPath 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param menus 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     */
    private SysMenu bestMenuForEndpoint(String endpointPath, List<SysMenu> menus) {
        String normalizedEndpoint = normalizeApiPath(endpointPath);
        return menus.stream()
                .filter(menu -> !MENU_GROUP_COMPONENT.equals(menu.getComponent()))
                .filter(menu -> hasText(menu.getPath()))
                .filter(menu -> pathMatches(normalizedEndpoint, menu.getPath()))
                .max(Comparator.comparingInt(menu -> menu.getPath().length()))
                .orElse(null);
    }

    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param endpointPath 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param menuPath 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     */
    private boolean pathMatches(String endpointPath, String menuPath) {
        return endpointPath.equals(menuPath)
                || endpointPath.startsWith(menuPath + "/")
                || menuPath.startsWith(endpointPath + "/");
    }

    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param endpointPath 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     */
    private String normalizeApiPath(String endpointPath) {
        String normalized = endpointPath.replaceAll("\\{[^/]+}", "").replaceAll("/+", "/");
        if (normalized.startsWith("/api/")) {
            normalized = normalized.substring(4);
        } else if (normalized.equals("/api")) {
            normalized = "/";
        }
        return normalized.replaceAll("/+$", "");
    }

    /**
     * 校验业务参数或业务状态，阻止非法流程继续执行。
     * @param menu 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     */
    private SysPermission ensureMenuPermissionNode(SysMenu menu) {
        SysPermission permission = permissionMapper.selectOne(new LambdaQueryWrapper<SysPermission>()
                .eq(SysPermission::getPermissionCode, menuNodeCode(menu.getId()))
                .last("LIMIT 1"));
        if (permission == null) {
            permission = new SysPermission();
            permission.setPermissionCode(menuNodeCode(menu.getId()));
            permission.setSystemGenerated(1);
        }
        permission.setParentId(parentMenuPermissionId(menu.getParentId()));
        permission.setMenuId(menu.getId());
        permission.setPermissionName(menu.getMenuName());
        permission.setPermissionType(MENU_GROUP_COMPONENT.equals(menu.getComponent()) ? TYPE_GROUP : TYPE_MENU);
        permission.setSortOrder(menu.getSortOrder());
        permission.setSystemGenerated(1);
        if (permission.getId() == null) {
            permissionMapper.insert(permission);
            return permissionMapper.selectById(permission.getId());
        }
        permissionMapper.updateById(permission);
        return permission;
    }

    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param parentMenuId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     */
    private Long parentMenuPermissionId(Long parentMenuId) {
        if (parentMenuId == null) {
            return null;
        }
        SysPermission parent = permissionMapper.selectOne(new LambdaQueryWrapper<SysPermission>()
                .eq(SysPermission::getPermissionCode, menuNodeCode(parentMenuId))
                .last("LIMIT 1"));
        return parent == null ? null : parent.getId();
    }

    /**
     * 校验业务参数或业务状态，阻止非法流程继续执行。
     * @return 封装后的业务处理结果。
     */
    private SysPermission ensureUncategorizedRoot() {
        SysPermission permission = permissionMapper.selectOne(new LambdaQueryWrapper<SysPermission>()
                .eq(SysPermission::getPermissionCode, UNCATEGORIZED_CODE)
                .last("LIMIT 1"));
        if (permission != null) {
            return permission;
        }
        permission = new SysPermission();
        permission.setPermissionCode(UNCATEGORIZED_CODE);
        permission.setPermissionName("未归类权限");
        permission.setPermissionType(TYPE_GROUP);
        permission.setSortOrder(UNCATEGORIZED_SORT_ORDER);
        permission.setSystemGenerated(1);
        permissionMapper.insert(permission);
        return permissionMapper.selectById(permission.getId());
    }

    /**
     * 查询或解析业务数据，返回前端或内部流程需要的结果。
     * @param parentId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     */
    private Long resolveParentId(Long parentId) {
        if (parentId != null) {
            requirePermission(parentId);
            return parentId;
        }
        return ensureUncategorizedRoot().getId();
    }

    /**
     * 转换业务对象，生成前端返回视图或内部传输结构。
     * @param permissions 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     */
    private List<PermissionResponse> toTree(List<SysPermission> permissions) {
        Map<Long, List<SysPermission>> children = permissions.stream()
                .collect(Collectors.groupingBy(permission -> permission.getParentId() == null ? 0L : permission.getParentId()));
        return buildTree(children, 0L);
    }

    /**
     * 转换业务对象，生成前端返回视图或内部传输结构。
     * @param M 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param children 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param parentId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     */
    private List<PermissionResponse> buildTree(Map<Long, List<SysPermission>> children, Long parentId) {
        return children.getOrDefault(parentId, List.of()).stream()
                .sorted(Comparator.comparing((SysPermission permission) -> permission.getSortOrder() == null ? 0 : permission.getSortOrder())
                        .thenComparing(SysPermission::getId))
                .map(permission -> toResponse(permission, buildTree(children, permission.getId())))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * 转换业务对象，生成前端返回视图或内部传输结构。
     * @param permission 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param children 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     */
    private PermissionResponse toResponse(SysPermission permission, List<PermissionResponse> children) {
        return new PermissionResponse(
                permission.getId(),
                permission.getParentId(),
                permission.getMenuId(),
                permission.getPermissionCode(),
                permission.getPermissionName(),
                permission.getPermissionType(),
                permission.getSortOrder(),
                Objects.equals(permission.getSystemGenerated(), 1),
                assignable(permission),
                children
        );
    }

    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param permission 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     */
    private boolean assignable(SysPermission permission) {
        return hasText(permission.getPermissionCode())
                && !permission.getPermissionCode().startsWith("__")
                && !TYPE_GROUP.equals(permission.getPermissionType())
                && !TYPE_MENU.equals(permission.getPermissionType());
    }

    /**
     * 删除或失效指定业务数据，并同步清理关联状态。
     * @param permission 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     */
    private void deletePermission(SysPermission permission) {
        rolePermissionMapper.delete(new LambdaUpdateWrapper<SysRolePermission>().eq(SysRolePermission::getPermissionId, permission.getId()));
        permissionMapper.deleteById(permission.getId());
    }

    /**
     * 校验业务参数或业务状态，阻止非法流程继续执行。
     * @param code 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param excludeId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     */
    private void ensureCodeAvailable(String code, Long excludeId) {
        LambdaQueryWrapper<SysPermission> query = new LambdaQueryWrapper<SysPermission>().eq(SysPermission::getPermissionCode, code);
        if (excludeId != null) {
            query.ne(SysPermission::getId, excludeId);
        }
        Long count = permissionMapper.selectCount(query);
        if (count != null && count > 0) {
            throw BusinessException.conflict("权限码已存在");
        }
    }

    /**
     * 校验业务参数或业务状态，阻止非法流程继续执行。
     * @param id 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     */
    private SysPermission requirePermission(Long id) {
        SysPermission permission = permissionMapper.selectById(id);
        if (permission == null) {
            throw BusinessException.badRequest("权限不存在");
        }
        return permission;
    }

    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param menuId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     */
    private String menuNodeCode(Long menuId) {
        return "__menu:" + menuId;
    }

    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param value 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * EndpointPermission 不可变业务数据记录，用于接口入参、接口返回或服务间传输。
     * @param permissionCode 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param method 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param path 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     */
    private record EndpointPermission(String permissionCode, String method, String path) {
        String displayName() {
            return method + " " + path;
        }
    }
}

