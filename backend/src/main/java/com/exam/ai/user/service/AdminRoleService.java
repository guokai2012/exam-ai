package com.exam.ai.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.exam.ai.common.exception.BusinessException;
import com.exam.ai.user.entity.SysPermission;
import com.exam.ai.user.entity.SysRole;
import com.exam.ai.user.entity.SysRolePermission;
import com.exam.ai.user.entity.SysUserRole;
import com.exam.ai.user.dto.RoleResponse;
import com.exam.ai.user.dto.SaveRoleRequest;
import com.exam.ai.user.mapper.SysPermissionMapper;
import com.exam.ai.user.mapper.SysRoleMapper;
import com.exam.ai.user.mapper.SysRolePermissionMapper;
import com.exam.ai.user.mapper.SysUserRoleMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminRoleService {

    private final SysRoleMapper roleMapper;
    private final SysPermissionMapper permissionMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysUserRoleMapper userRoleMapper;

    public AdminRoleService(SysRoleMapper roleMapper, SysPermissionMapper permissionMapper,
                            SysRolePermissionMapper rolePermissionMapper,
                            SysUserRoleMapper userRoleMapper) {
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.userRoleMapper = userRoleMapper;
    }

    public List<RoleResponse> list() {
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getId))
                .stream().map(this::toResponse).toList();
    }

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

    private SysRole requireRole(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw BusinessException.badRequest("角色不存在");
        }
        return role;
    }

    private RoleResponse toResponse(SysRole role) {
        List<String> permissions = permissionMapper.selectList(new LambdaQueryWrapper<SysPermission>()
                        .inSql(SysPermission::getId, "SELECT permission_id FROM sys_role_permission WHERE role_id = " + role.getId())
                        .orderByAsc(SysPermission::getId))
                .stream().map(SysPermission::getPermissionCode).toList();
        return new RoleResponse(role.getId(), role.getRoleCode(), role.getRoleName(), permissions);
    }
}

