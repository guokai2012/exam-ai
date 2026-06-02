package com.exam.ai.common.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.ai.user.entity.SysPermission;
import com.exam.ai.user.entity.SysRole;
import com.exam.ai.user.entity.SysRolePermission;
import com.exam.ai.user.mapper.SysPermissionMapper;
import com.exam.ai.user.mapper.SysRoleMapper;
import com.exam.ai.user.mapper.SysRolePermissionMapper;
import com.exam.ai.user.service.AdminPermissionService;
import com.exam.ai.util.CurrentUserUtils;
import com.exam.ai.util.SystemUserModule;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 权限启动同步器，负责应用启动时扫描 Controller 权限并补齐默认角色授权。
 */
@Component
@Order(20)
public class PermissionBootstrapRunner implements ApplicationRunner {

    private static final String ADMIN_ROLE = "ADMIN";
    private static final String TEACHER_ROLE = "TEACHER";
    private static final String STUDENT_ROLE = "STUDENT";
    private static final List<String> TEACHER_DEFAULT_PERMISSIONS = List.of(
            "auth:logout",
            "auth:me",
            "password:change",
            "question-category:list",
            "question-category:create",
            "question:list",
            "question:detail",
            "question:review",
            "document:upload",
            "document:list",
            "document:detail",
            "document:content",
            "document:analyze",
            "document:analysis-latest",
            "notification:list",
            "notification:mark-read"
    );
    private static final List<String> STUDENT_DEFAULT_PERMISSIONS = List.of(
            "auth:logout",
            "auth:me",
            "password:change"
    );

    private final AdminPermissionService permissionService;
    private final SysRoleMapper roleMapper;
    private final SysPermissionMapper permissionMapper;
    private final SysRolePermissionMapper rolePermissionMapper;

    /**
     * 构造权限启动同步器。
     *
     * @param permissionService Controller 权限扫描服务。
     * @param roleMapper 角色表访问器。
     * @param permissionMapper 权限表访问器。
     * @param rolePermissionMapper 角色权限关系访问器。
     */
    public PermissionBootstrapRunner(AdminPermissionService permissionService,
                                     SysRoleMapper roleMapper,
                                     SysPermissionMapper permissionMapper,
                                     SysRolePermissionMapper rolePermissionMapper) {
        this.permissionService = permissionService;
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.rolePermissionMapper = rolePermissionMapper;
    }

    /**
     * 应用启动后立即扫描权限，并补齐内置角色默认授权。
     *
     * @param args 应用启动参数，本方法不使用。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(ApplicationArguments args) {
        CurrentUserUtils.runAsSystem(SystemUserModule.USER, (Callable<Void>) () -> {
            permissionService.scanControllerPermissions();
            grantDefaultRolePermissions();
            return null;
        });
    }

    /**
     * 补齐默认角色授权关系。
     *
     * <p>ADMIN 角色默认拥有所有扫描出的动作权限；TEACHER/STUDENT 按内置业务清单补齐。
     * 方法只新增缺失关系，不覆盖管理员已做的其他角色授权。</p>
     */
    private void grantDefaultRolePermissions() {
        Map<String, SysRole> roles = roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                        .in(SysRole::getRoleCode, List.of(ADMIN_ROLE, TEACHER_ROLE, STUDENT_ROLE)))
                .stream()
                .collect(java.util.stream.Collectors.toMap(SysRole::getRoleCode, role -> role));
        List<SysPermission> actionPermissions = permissionMapper.selectList(new LambdaQueryWrapper<SysPermission>()
                .eq(SysPermission::getPermissionType, AdminPermissionService.TYPE_ACTION));
        Map<String, SysPermission> permissionsByCode = actionPermissions.stream()
                .collect(java.util.stream.Collectors.toMap(SysPermission::getPermissionCode, permission -> permission, (left, right) -> left));

        grantRolePermissions(roles.get(ADMIN_ROLE), actionPermissions.stream().map(SysPermission::getPermissionCode).collect(java.util.stream.Collectors.toSet()), permissionsByCode);
        grantRolePermissions(roles.get(TEACHER_ROLE), Set.copyOf(TEACHER_DEFAULT_PERMISSIONS), permissionsByCode);
        grantRolePermissions(roles.get(STUDENT_ROLE), Set.copyOf(STUDENT_DEFAULT_PERMISSIONS), permissionsByCode);
    }

    /**
     * 为指定角色补齐权限关系。
     *
     * @param role 角色实体，可能为空。
     * @param permissionCodes 需要补齐的权限码集合。
     * @param permissionsByCode 当前有效动作权限索引。
     */
    private void grantRolePermissions(SysRole role, Set<String> permissionCodes, Map<String, SysPermission> permissionsByCode) {
        if (role == null) {
            return;
        }
        for (String permissionCode : permissionCodes) {
            SysPermission permission = permissionsByCode.get(permissionCode);
            if (permission == null) {
                continue;
            }
            Long count = rolePermissionMapper.selectCount(new LambdaQueryWrapper<SysRolePermission>()
                    .eq(SysRolePermission::getRoleId, role.getId())
                    .eq(SysRolePermission::getPermissionId, permission.getId()));
            if (count == null || count == 0) {
                rolePermissionMapper.insert(new SysRolePermission(role.getId(), permission.getId()));
            }
        }
    }
}
