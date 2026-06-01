package com.exam.ai.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.exam.ai.common.exception.BusinessException;
import com.exam.ai.user.entity.SysPermission;
import com.exam.ai.user.entity.SysRole;
import com.exam.ai.user.entity.SysUserRole;
import com.exam.ai.user.mapper.SysPermissionMapper;
import com.exam.ai.user.mapper.SysRoleMapper;
import com.exam.ai.user.mapper.SysUserRoleMapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RolePermissionService {

    private final SysRoleMapper roleMapper;
    private final SysPermissionMapper permissionMapper;
    private final SysUserRoleMapper userRoleMapper;

    public RolePermissionService(SysRoleMapper roleMapper, SysPermissionMapper permissionMapper,
                                 SysUserRoleMapper userRoleMapper) {
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.userRoleMapper = userRoleMapper;
    }

    public List<String> roles(Long userId) {
        return roleMapper.selectByUserId(userId).stream().map(SysRole::getRoleCode).toList();
    }

    public List<String> permissions(Long userId) {
        return permissionMapper.selectByUserId(userId).stream().map(SysPermission::getPermissionCode).toList();
    }

    public void replaceUserRoles(Long userId, List<String> roleCodes) {
        userRoleMapper.delete(new LambdaUpdateWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        for (String roleCode : roleCodes.stream().distinct().toList()) {
            SysRole role = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, roleCode));
            if (role == null) {
                throw BusinessException.badRequest("角色不存在");
            }
            userRoleMapper.insert(new SysUserRole(userId, role.getId()));
        }
    }
}

