package com.exam.ai.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.exam.ai.system.service.NotificationService;
import com.exam.ai.user.entity.SysMenu;
import com.exam.ai.user.entity.SysPermission;
import com.exam.ai.user.entity.SysRolePermission;
import com.exam.ai.user.mapper.SysMenuMapper;
import com.exam.ai.user.mapper.SysPermissionMapper;
import com.exam.ai.user.mapper.SysRolePermissionMapper;
import com.exam.ai.user.service.AdminPermissionService;
import com.exam.ai.user.vo.PermissionResponse;
import com.exam.ai.user.vo.PermissionScanResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * 后台权限管理服务实现，负责从 Controller 权限表达式扫描并全量同步权限数据。
 *
 * <p>权限表的业务权限只允许由扫描生成。扫描过程中发现缺少接口摘要或权限码重复时，
 * 不阻断同步流程，而是通过站内通知提醒管理员修正 Controller 注解。</p>
 */
@Service
public class AdminPermissionServiceImpl implements AdminPermissionService {

    public static final String TYPE_GROUP = AdminPermissionService.TYPE_GROUP;
    public static final String TYPE_ACTION = AdminPermissionService.TYPE_ACTION;

    private static final String ADMIN_ROLE_CODE = "ADMIN";
    private static final String CONTROLLER_GROUP_PREFIX = "__controller:";
    private static final String FALLBACK_GROUP_CODE = "__controller:uncategorized";
    private static final String FALLBACK_GROUP_NAME = "未归类接口";
    private static final String MISSING_SUMMARY_TITLE = "权限扫描缺少接口摘要";
    private static final String DUPLICATE_CODE_TITLE = "权限扫描发现重复权限码";
    private static final String NAME_SEPARATOR = "/";
    private static final String ENDPOINT_SEPARATOR = "，";
    private static final int PERMISSION_NAME_MAX_LENGTH = 512;
    private static final int GROUP_SORT_STEP = 10;
    private static final int ACTION_SORT_STEP = 10;
    private static final Pattern HAS_AUTHORITY = Pattern.compile("hasAuthority\\(\\s*['\"]([^'\"]+)['\"]\\s*\\)");
    private static final Pattern HAS_ANY_AUTHORITY = Pattern.compile("hasAnyAuthority\\(([^)]*)\\)");
    private static final Pattern QUOTED_VALUE = Pattern.compile("['\"]([^'\"]+)['\"]");

    private final SysPermissionMapper permissionMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysMenuMapper menuMapper;
    private final NotificationService notificationService;
    private final RequestMappingHandlerMapping handlerMapping;

    /**
     * 构造权限扫描服务，并注入数据库访问器、通知服务和 Spring MVC 路由映射。
     *
     * @param permissionMapper 权限表访问器。
     * @param rolePermissionMapper 角色权限关系访问器，用于删除过期权限时清理授权关系。
     * @param menuMapper 菜单表访问器，用于按 api_path 反查权限分组展示名称。
     * @param notificationService 站内通知服务，用于推送扫描告警给管理员。
     * @param handlerMapping Spring MVC Controller 路由映射。
     */
    public AdminPermissionServiceImpl(SysPermissionMapper permissionMapper,
                                      SysRolePermissionMapper rolePermissionMapper,
                                      SysMenuMapper menuMapper,
                                      NotificationService notificationService,
                                      @Qualifier("requestMappingHandlerMapping")
                                      RequestMappingHandlerMapping handlerMapping) {
        this.permissionMapper = permissionMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.menuMapper = menuMapper;
        this.notificationService = notificationService;
        this.handlerMapping = handlerMapping;
    }

    /**
     * 查询当前扫描生成的权限树。
     *
     * @return 按 Controller 分组组织的权限树。
     */
    public List<PermissionResponse> list() {
        return toTree(permissionMapper.selectList(new LambdaQueryWrapper<SysPermission>()
                .orderByAsc(SysPermission::getSortOrder)
                .orderByAsc(SysPermission::getId)));
    }

    /**
     * 扫描 Controller 权限表达式，并用扫描结果全量覆盖权限表。
     *
     * @return 本次扫描新增、更新和删除的权限数量。
     */
    @Transactional(rollbackFor = Exception.class)
    public PermissionScanResponse scanControllerPermissions() {
        ScanResult scanResult = scanEndpointPermissions();
        Map<String, SysPermission> existingByCode = permissionMapper.selectList(new LambdaQueryWrapper<SysPermission>())
                .stream()
                .collect(Collectors.toMap(SysPermission::getPermissionCode, permission -> permission, (left, right) -> left));
        LocalDateTime now = LocalDateTime.now();
        int created = 0;
        int updated = 0;

        Map<String, SysPermission> groupByCode = new LinkedHashMap<>();
        for (ScannedGroup group : scanResult.groups().values()) {
            SaveResult result = saveScannedGroup(group, existingByCode.get(group.groupCode()), now);
            groupByCode.put(group.groupCode(), result.permission());
            created += result.created();
            updated += result.updated();
        }

        for (ScannedPermission scannedPermission : scanResult.permissions().values()) {
            SysPermission parent = groupByCode.get(scannedPermission.groupCode());
            SaveResult result = saveScannedAction(scannedPermission, existingByCode.get(scannedPermission.permissionCode()), parent.getId(), now);
            created += result.created();
            updated += result.updated();
        }

        Set<String> targetCodes = new LinkedHashSet<>();
        targetCodes.addAll(scanResult.groups().keySet());
        targetCodes.addAll(scanResult.permissions().keySet());
        List<SysPermission> stalePermissions = existingByCode.values().stream()
                .filter(permission -> !targetCodes.contains(permission.getPermissionCode()))
                .toList();
        stalePermissions.forEach(this::deletePermission);

        notifyScanWarnings(scanResult);
        return new PermissionScanResponse(created, updated, stalePermissions.size());
    }

    /**
     * 保存扫描生成的 Controller 分组节点。
     *
     * @param group 扫描得到的分组定义。
     * @param existing 已存在的同编码权限记录。
     * @param now 本次扫描时间。
     * @return 本次保存动作和保存后的权限记录。
     */
    private SaveResult saveScannedGroup(ScannedGroup group, SysPermission existing, LocalDateTime now) {
        SysPermission permission = existing == null ? new SysPermission() : existing;
        permission.setParentId(null);
        permission.setMenuId(null);
        permission.setPermissionCode(group.groupCode());
        permission.setPermissionName(group.groupName());
        permission.setPermissionType(TYPE_GROUP);
        permission.setSortOrder(group.sortOrder());
        permission.setSystemGenerated(1);
        permission.setLastScannedAt(now);
        if (existing == null) {
            permissionMapper.insert(permission);
            return new SaveResult(permissionMapper.selectById(permission.getId()), 1, 0);
        }
        permissionMapper.updateById(permission);
        return new SaveResult(permission, 0, 1);
    }

    /**
     * 保存扫描生成的接口动作权限。
     *
     * @param scannedPermission 扫描得到的接口权限。
     * @param existing 已存在的同编码权限记录。
     * @param parentId Controller 分组权限 ID。
     * @param now 本次扫描时间。
     * @return 本次保存动作和保存后的权限记录。
     */
    private SaveResult saveScannedAction(ScannedPermission scannedPermission, SysPermission existing, Long parentId, LocalDateTime now) {
        SysPermission permission = existing == null ? new SysPermission() : existing;
        permission.setParentId(parentId);
        permission.setMenuId(null);
        permission.setPermissionCode(scannedPermission.permissionCode());
        permission.setPermissionName(scannedPermission.displayName());
        permission.setPermissionType(TYPE_ACTION);
        permission.setSortOrder(scannedPermission.sortOrder());
        permission.setSystemGenerated(1);
        permission.setLastScannedAt(now);
        if (existing == null) {
            permissionMapper.insert(permission);
            return new SaveResult(permissionMapper.selectById(permission.getId()), 1, 0);
        }
        permissionMapper.updateById(permission);
        return new SaveResult(permission, 0, 1);
    }

    /**
     * 扫描所有 MVC Controller 方法，提取权限码、接口摘要、Controller 分组和告警信息。
     *
     * @return 扫描结果，包含目标权限集合和需要通知管理员的告警。
     */
    private ScanResult scanEndpointPermissions() {
        Map<String, ScannedGroup> groups = new LinkedHashMap<>();
        Map<String, ScannedPermission> permissions = new LinkedHashMap<>();
        List<String> missingSummaries = new ArrayList<>();
        List<String> duplicateCodes = new ArrayList<>();
        Map<String, String> groupNamesByApiPath = menuGroupNamesByApiPath();
        List<Map.Entry<RequestMappingInfo, HandlerMethod>> endpoints = handlerMapping.getHandlerMethods().entrySet().stream()
                .sorted(Comparator.comparing(entry -> endpointKey(entry.getKey(), entry.getValue())))
                .toList();

        for (Map.Entry<RequestMappingInfo, HandlerMethod> endpoint : endpoints) {
            HandlerMethod handlerMethod = endpoint.getValue();
            Set<String> authorityCodes = extractAuthorityCodes(handlerMethod);
            if (authorityCodes.isEmpty()) {
                continue;
            }
            List<String> paths = paths(endpoint.getKey());
            List<String> methods = methods(endpoint.getKey());
            String endpointDescription = methods.get(0) + " " + paths.get(0);
            String controllerBasePath = controllerBasePath(handlerMethod);
            String groupName = resolveGroupName(handlerMethod, controllerBasePath, groupNamesByApiPath);
            String groupCode = groupCode(controllerBasePath, groupName);
            ScannedGroup group = groups.computeIfAbsent(groupCode,
                    code -> new ScannedGroup(code, groupName, (groups.size() + 1) * GROUP_SORT_STEP));
            String displayName = resolveDisplayName(handlerMethod, endpointDescription, missingSummaries);

            for (String authorityCode : authorityCodes) {
                ScannedPermission permission = permissions.get(authorityCode);
                if (permission == null) {
                    permissions.put(authorityCode, new ScannedPermission(
                            authorityCode,
                            group.groupCode(),
                            (permissions.size() + 1) * ACTION_SORT_STEP,
                            displayName,
                            endpointDescription
                    ));
                    continue;
                }
                permission.addDisplayName(displayName);
                permission.addEndpoint(endpointDescription);
                duplicateCodes.add(authorityCode + " -> " + permission.displayName() + " -> " + permission.endpoints());
            }
        }

        return new ScanResult(groups, permissions, missingSummaries, duplicateCodes);
    }

    /**
     * 从接口方法解析权限显示名称，缺少 OpenAPI summary 时使用方法和路径兜底。
     *
     * @param handlerMethod Spring MVC 处理方法。
     * @param endpointDescription HTTP 方法和路径描述。
     * @param missingSummaries 缺少 summary 的告警收集器。
     * @return 权限名称片段。
     */
    private String resolveDisplayName(HandlerMethod handlerMethod, String endpointDescription, List<String> missingSummaries) {
        Operation operation = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), Operation.class);
        if (operation != null && hasText(operation.summary())) {
            return operation.summary().trim();
        }
        missingSummaries.add(endpointDescription + " -> " + handlerMethod.getBeanType().getSimpleName() + "#" + handlerMethod.getMethod().getName());
        return endpointDescription;
    }

    /**
     * 从菜单 api_path 或 Controller 的 OpenAPI Tag 解析权限树分组名称。
     *
     * @param handlerMethod Spring MVC 处理方法。
     * @param controllerBasePath Controller 类级 RequestMapping 根路径。
     * @param groupNamesByApiPath 菜单 api_path 与菜单名称映射。
     * @return 分组名称；菜单和 Tag 均缺失时返回未归类接口。
     */
    private String resolveGroupName(HandlerMethod handlerMethod, String controllerBasePath, Map<String, String> groupNamesByApiPath) {
        // 菜单是后台展示入口的权威名称来源，权限树优先跟随菜单名称展示。
        if (hasText(controllerBasePath) && hasText(groupNamesByApiPath.get(controllerBasePath))) {
            return groupNamesByApiPath.get(controllerBasePath);
        }
        Tag methodTag = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), Tag.class);
        if (methodTag != null && hasText(methodTag.name())) {
            return methodTag.name().trim();
        }
        Tag classTag = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), Tag.class);
        if (classTag != null && hasText(classTag.name())) {
            return classTag.name().trim();
        }
        return FALLBACK_GROUP_NAME;
    }

    /**
     * 生成扫描分组权限码，优先使用 Controller 根路径确保菜单名称补齐后仍更新同一分组。
     *
     * @param controllerBasePath Controller 类级 RequestMapping 根路径。
     * @param groupName Controller 分组名称。
     * @return 稳定的内部权限码。
     */
    private String groupCode(String controllerBasePath, String groupName) {
        if (hasText(controllerBasePath)) {
            return CONTROLLER_GROUP_PREFIX + Integer.toHexString(controllerBasePath.hashCode());
        }
        if (!hasText(groupName)) {
            return FALLBACK_GROUP_CODE;
        }
        return CONTROLLER_GROUP_PREFIX + Integer.toHexString(groupName.hashCode());
    }

    /**
     * 查询菜单并按 api_path 构建权限分组展示名称索引。
     *
     * @return api_path 到菜单名称的映射；同一路径多个菜单名称会按菜单排序用斜杠合并。
     */
    private Map<String, String> menuGroupNamesByApiPath() {
        List<SysMenu> menus = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .orderByAsc(SysMenu::getSortOrder)
                .orderByAsc(SysMenu::getId));
        Map<String, LinkedHashSet<String>> namesByApiPath = new LinkedHashMap<>();
        for (SysMenu menu : menus) {
            // path 为空表示纯分组菜单，不能作为接口权限分组的直接命名来源。
            if (!hasText(menu.getPath()) || !hasText(menu.getApiPath()) || !hasText(menu.getMenuName())) {
                continue;
            }
            namesByApiPath.computeIfAbsent(menu.getApiPath().trim(), key -> new LinkedHashSet<>())
                    .add(menu.getMenuName().trim());
        }
        Map<String, String> result = new LinkedHashMap<>();
        namesByApiPath.forEach((apiPath, names) -> result.put(apiPath, String.join(NAME_SEPARATOR, names)));
        return result;
    }

    /**
     * 解析 Controller 类级 RequestMapping 根路径，用作菜单 api_path 反查参数和稳定分组编码来源。
     *
     * @param handlerMethod Spring MVC 处理方法。
     * @return Controller 根路径；缺失时返回 null。
     */
    private String controllerBasePath(HandlerMethod handlerMethod) {
        RequestMapping requestMapping = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), RequestMapping.class);
        if (requestMapping == null) {
            return null;
        }
        List<String> values = new ArrayList<>();
        for (String path : requestMapping.path()) {
            if (hasText(path)) {
                values.add(path.trim());
            }
        }
        for (String value : requestMapping.value()) {
            if (hasText(value)) {
                values.add(value.trim());
            }
        }
        return values.stream().sorted().findFirst().orElse(null);
    }

    /**
     * 向管理员发送本次扫描发现的非阻断告警。
     *
     * @param scanResult 本次扫描结果。
     */
    private void notifyScanWarnings(ScanResult scanResult) {
        if (!scanResult.missingSummaries().isEmpty()) {
            notificationService.createForRole(
                    ADMIN_ROLE_CODE,
                    MISSING_SUMMARY_TITLE,
                    String.join(System.lineSeparator(), scanResult.missingSummaries()),
                    NotificationService.TYPE_PERMISSION_SCAN_WARNING,
                    NotificationService.BUSINESS_PERMISSION_SCAN,
                    null
            );
        }
        if (!scanResult.duplicateCodes().isEmpty()) {
            notificationService.createForRole(
                    ADMIN_ROLE_CODE,
                    DUPLICATE_CODE_TITLE,
                    String.join(System.lineSeparator(), scanResult.duplicateCodes()),
                    NotificationService.TYPE_PERMISSION_SCAN_WARNING,
                    NotificationService.BUSINESS_PERMISSION_SCAN,
                    null
            );
        }
    }

    /**
     * 提取方法和类级别权限表达式中的 authority 权限码。
     *
     * @param handlerMethod Spring MVC 处理方法。
     * @return 按表达式顺序去重后的权限码集合。
     */
    private Set<String> extractAuthorityCodes(HandlerMethod handlerMethod) {
        Set<String> codes = new LinkedHashSet<>();
        Method method = handlerMethod.getMethod();
        PreAuthorize classAuthorize = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), PreAuthorize.class);
        PreAuthorize methodAuthorize = AnnotatedElementUtils.findMergedAnnotation(method, PreAuthorize.class);
        if (classAuthorize != null) {
            collectAuthorityCodes(classAuthorize.value(), codes);
        }
        if (methodAuthorize != null) {
            collectAuthorityCodes(methodAuthorize.value(), codes);
        }
        return codes;
    }

    /**
     * 从 Spring Security 表达式中解析 hasAuthority 和 hasAnyAuthority 权限码。
     *
     * @param expression 权限表达式。
     * @param codes 权限码收集器。
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
     * 解析 RequestMapping 的路径集合。
     *
     * @param mappingInfo Spring MVC 请求映射信息。
     * @return 排序后的路径集合，缺失时使用根路径。
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
     * 解析 RequestMapping 的 HTTP 方法集合。
     *
     * @param mappingInfo Spring MVC 请求映射信息。
     * @return 排序后的 HTTP 方法集合，缺失时使用 ANY。
     */
    private List<String> methods(RequestMappingInfo mappingInfo) {
        Set<RequestMethod> requestMethods = mappingInfo.getMethodsCondition().getMethods();
        if (requestMethods.isEmpty()) {
            return List.of("ANY");
        }
        return requestMethods.stream().map(RequestMethod::name).sorted().toList();
    }

    /**
     * 生成用于扫描排序的接口稳定标识。
     *
     * @param mappingInfo Spring MVC 请求映射信息。
     * @param handlerMethod Spring MVC 处理方法。
     * @return 排序标识。
     */
    private String endpointKey(RequestMappingInfo mappingInfo, HandlerMethod handlerMethod) {
        return String.join(",", paths(mappingInfo)) + "#" + String.join(",", methods(mappingInfo))
                + "#" + handlerMethod.getBeanType().getName() + "#" + handlerMethod.getMethod().getName();
    }

    /**
     * 删除不再由 Controller 扫描产生的权限，并同步清理角色授权关系。
     *
     * @param permission 待删除权限。
     */
    private void deletePermission(SysPermission permission) {
        rolePermissionMapper.delete(new LambdaUpdateWrapper<SysRolePermission>().eq(SysRolePermission::getPermissionId, permission.getId()));
        permissionMapper.deleteById(permission.getId());
    }

    /**
     * 将权限列表转换为前端需要的树结构。
     *
     * @param permissions 权限实体列表。
     * @return 权限树。
     */
    private List<PermissionResponse> toTree(List<SysPermission> permissions) {
        Map<Long, List<SysPermission>> children = permissions.stream()
                .collect(Collectors.groupingBy(permission -> permission.getParentId() == null ? 0L : permission.getParentId()));
        return buildTree(children, 0L);
    }

    /**
     * 递归构建指定父节点下的权限树。
     *
     * @param children 父子权限索引。
     * @param parentId 当前父节点 ID。
     * @return 当前父节点下的子权限列表。
     */
    private List<PermissionResponse> buildTree(Map<Long, List<SysPermission>> children, Long parentId) {
        return children.getOrDefault(parentId, List.of()).stream()
                .sorted(Comparator.comparing((SysPermission permission) -> permission.getSortOrder() == null ? 0 : permission.getSortOrder())
                        .thenComparing(SysPermission::getId))
                .map(permission -> toResponse(permission, buildTree(children, permission.getId())))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * 将权限实体转换为前端 VO。
     *
     * @param permission 权限实体。
     * @param children 已构建好的子权限。
     * @return 权限响应对象。
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
     * 判断权限节点是否可分配给角色。
     *
     * @param permission 权限实体。
     * @return 动作权限返回 true，内部分组返回 false。
     */
    private boolean assignable(SysPermission permission) {
        return hasText(permission.getPermissionCode())
                && !permission.getPermissionCode().startsWith("__")
                && TYPE_ACTION.equals(permission.getPermissionType());
    }

    /**
     * 判断字符串是否包含非空白字符。
     *
     * @param value 待判断字符串。
     * @return 有有效字符时返回 true。
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * 本次扫描到的 Controller 分组。
     *
     * @param groupCode 内部分组权限码。
     * @param groupName Controller 分组展示名称。
     * @param sortOrder 排序值。
     */
    private record ScannedGroup(String groupCode, String groupName, Integer sortOrder) {
    }

    /**
     * 本次扫描到的接口权限，支持重复权限码的名称和接口路径聚合。
     */
    private static final class ScannedPermission {

        private final String permissionCode;
        private final String groupCode;
        private final Integer sortOrder;
        private final LinkedHashSet<String> displayNames = new LinkedHashSet<>();
        private final LinkedHashSet<String> endpoints = new LinkedHashSet<>();

        /**
         * 创建扫描权限对象。
         *
         * @param permissionCode 权限码。
         * @param groupCode 所属 Controller 分组编码。
         * @param sortOrder 排序值。
         * @param displayName 权限名称片段。
         * @param endpoint 接口路径片段。
         */
        private ScannedPermission(String permissionCode, String groupCode, Integer sortOrder, String displayName, String endpoint) {
            this.permissionCode = permissionCode;
            this.groupCode = groupCode;
            this.sortOrder = sortOrder;
            addDisplayName(displayName);
            addEndpoint(endpoint);
        }

        /**
         * 追加去重后的权限名称片段。
         *
         * @param displayName 权限名称片段。
         */
        private void addDisplayName(String displayName) {
            displayNames.add(displayName);
        }

        /**
         * 追加去重后的接口路径片段。
         *
         * @param endpoint 接口路径片段。
         */
        private void addEndpoint(String endpoint) {
            endpoints.add(endpoint);
        }

        /**
         * 返回权限码。
         *
         * @return 权限码。
         */
        private String permissionCode() {
            return permissionCode;
        }

        /**
         * 返回所属 Controller 分组编码。
         *
         * @return 分组编码。
         */
        private String groupCode() {
            return groupCode;
        }

        /**
         * 返回排序值。
         *
         * @return 排序值。
         */
        private Integer sortOrder() {
            return sortOrder;
        }

        /**
         * 使用斜杠合并重复权限码对应的多个接口摘要。
         *
         * @return 合并后的权限名称。
         */
        private String displayName() {
            String displayName = String.join(NAME_SEPARATOR, displayNames);
            return displayName.length() > PERMISSION_NAME_MAX_LENGTH
                    ? displayName.substring(0, PERMISSION_NAME_MAX_LENGTH)
                    : displayName;
        }

        /**
         * 使用中文逗号合并重复权限码对应的接口路径。
         *
         * @return 合并后的接口路径描述。
         */
        private String endpoints() {
            return String.join(ENDPOINT_SEPARATOR, endpoints);
        }
    }

    /**
     * Controller 权限扫描结果。
     *
     * @param groups 扫描生成的 Controller 分组。
     * @param permissions 扫描生成的动作权限。
     * @param missingSummaries 缺少 Operation summary 的告警。
     * @param duplicateCodes 重复权限码告警。
     */
    private record ScanResult(
            Map<String, ScannedGroup> groups,
            Map<String, ScannedPermission> permissions,
            List<String> missingSummaries,
            List<String> duplicateCodes
    ) {
    }

    /**
     * 权限保存结果。
     *
     * @param permission 保存后的权限实体。
     * @param created 新增数量。
     * @param updated 更新数量。
     */
    private record SaveResult(SysPermission permission, int created, int updated) {
    }
}
