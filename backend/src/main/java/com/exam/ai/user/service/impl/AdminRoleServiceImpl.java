package com.exam.ai.user.service.impl;

import com.exam.ai.user.service.AdminPermissionService;
import com.exam.ai.user.service.AdminRoleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.exam.ai.common.exception.BusinessException;
import com.exam.ai.user.entity.SysPermission;
import com.exam.ai.user.entity.SysRole;
import com.exam.ai.user.entity.SysRolePermission;
import com.exam.ai.user.entity.SysUserRole;
import com.exam.ai.user.vo.RoleResponse;
import com.exam.ai.user.dto.SaveRoleRequest;
import com.exam.ai.user.mapper.SysPermissionMapper;
import com.exam.ai.user.mapper.SysRoleMapper;
import com.exam.ai.user.mapper.SysRolePermissionMapper;
import com.exam.ai.user.mapper.SysUserRoleMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AdminRoleServiceImpl 类，承载当前分层中的业务职责。
 */
@Service
public class AdminRoleServiceImpl implements AdminRoleService {

    private final SysRoleMapper roleMapper;
    private final SysPermissionMapper permissionMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysUserRoleMapper userRoleMapper;

    /**
     * 构造 AdminRoleServiceImpl 实例并注入运行所需依赖。
     * @param roleMapper 业务参数，参与当前方法的校验、查询或状态变更。
     * @param permissionMapper 业务参数，参与当前方法的校验、查询或状态变更。
     * @param rolePermissionMapper 业务参数，参与当前方法的校验、查询或状态变更。
     * @param userRoleMapper 业务参数，参与当前方法的校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public AdminRoleServiceImpl(SysRoleMapper roleMapper, SysPermissionMapper permissionMapper,
                            SysRolePermissionMapper rolePermissionMapper,
                            SysUserRoleMapper userRoleMapper) {
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.userRoleMapper = userRoleMapper;
    }

    /**
     * 查询业务数据集合，并按调用场景组织返回结构。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public List<RoleResponse> list() {
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getId))
                .stream().map(this::toResponse).toList();
    }

    /**
     * 创建业务数据并完成必要的状态初始化。
     * @param request 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @Transactional(rollbackFor = Exception.class)
    public RoleResponse create(SaveRoleRequest request) {
        ensureCodeAvailable(request.roleCode(), null);
        SysRole role = new SysRole();
        role.setRoleCode(request.roleCode());
        role.setRoleName(request.roleName());
        roleMapper.insert(role);
        replaceRolePermissions(role.getId(), request.permissions());
        return toResponse(roleMapper.selectById(role.getId()));
    }

    /**
     * 更新业务状态，并保持相关数据的一致性。
     * @param id 业务参数，参与当前方法的校验、查询或状态变更。
     * @param request 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @Transactional(rollbackFor = Exception.class)
    public RoleResponse update(Long id, SaveRoleRequest request) {
        SysRole role = requireRole(id);
        ensureCodeAvailable(request.roleCode(), id);
        role.setRoleCode(request.roleCode());
        role.setRoleName(request.roleName());
        roleMapper.updateById(role);
        replaceRolePermissions(id, request.permissions());
        return toResponse(roleMapper.selectById(id));
    }

    /**
     * 删除或失效指定业务数据，并同步清理关联状态。
     * @param id 业务参数，参与当前方法的校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireRole(id);
        Long userCount = userRoleMapper.selectCount(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, id));
        if (userCount != null && userCount > 0) {
            throw BusinessException.conflict("角色已绑定用户，不能删除");
        }
        rolePermissionMapper.delete(new LambdaUpdateWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, id));
        roleMapper.deleteById(id);
    }

    /**
     * 更新业务状态，并保持相关数据的一致性。
     * @param roleId 业务参数，参与当前方法的校验、查询或状态变更。
     * @param permissionCodes 业务参数，参与当前方法的校验、查询或状态变更。
     */
    private void replaceRolePermissions(Long roleId, List<String> permissionCodes) {
        rolePermissionMapper.delete(new LambdaUpdateWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, roleId));
        for (String code : permissionCodes.stream().distinct().toList()) {
            SysPermission permission = permissionMapper.selectOne(new LambdaQueryWrapper<SysPermission>().eq(SysPermission::getPermissionCode, code));
            if (permission == null) {
                throw BusinessException.badRequest("权限不存在");
            }
            if (code.startsWith("__")
                    || AdminPermissionService.TYPE_GROUP.equals(permission.getPermissionType())
                    || AdminPermissionService.TYPE_MENU.equals(permission.getPermissionType())) {
                throw BusinessException.badRequest("权限不能分配给角色");
            }
            rolePermissionMapper.insert(new SysRolePermission(roleId, permission.getId()));
        }
    }

    /**
     * 校验业务参数或状态，阻止非法流程继续执行。
     * @param code 业务参数，参与当前方法的校验、查询或状态变更。
     * @param excludeId 业务参数，参与当前方法的校验、查询或状态变更。
     */
    private void ensureCodeAvailable(String code, Long excludeId) {
        LambdaQueryWrapper<SysRole> query = new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, code);
        if (excludeId != null) {
            query.ne(SysRole::getId, excludeId);
        }
        Long count = roleMapper.selectCount(query);
        if (count != null && count > 0) {
            throw BusinessException.conflict("角色编码已存在");
        }
    }

    /**
     * 校验业务参数或状态，阻止非法流程继续执行。
     * @param id 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     */
    private SysRole requireRole(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw BusinessException.badRequest("角色不存在");
        }
        return role;
    }

    /**
     * 转换业务对象，生成前端返回视图或内部传输结构。
     * @param role 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     */
    private RoleResponse toResponse(SysRole role) {
        List<String> permissions = permissionMapper.selectList(new LambdaQueryWrapper<SysPermission>()
                        .inSql(SysPermission::getId, "SELECT permission_id FROM sys_role_permission WHERE role_id = " + role.getId())
                        .orderByAsc(SysPermission::getId))
                .stream().map(SysPermission::getPermissionCode).toList();
        return new RoleResponse(role.getId(), role.getRoleCode(), role.getRoleName(), permissions);
    }
}

