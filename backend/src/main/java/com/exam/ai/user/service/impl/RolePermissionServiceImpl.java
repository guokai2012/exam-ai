package com.exam.ai.user.service.impl;

import com.exam.ai.user.service.RolePermissionService;
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

/**
 * RolePermissionServiceImpl 类，当前分层的业务组件，负责本模块对应的请求、服务或数据模型职责。
 */
@Service
public class RolePermissionServiceImpl implements RolePermissionService {

    private final SysRoleMapper roleMapper;
    private final SysPermissionMapper permissionMapper;
    private final SysUserRoleMapper userRoleMapper;

    /**
     * 构造 RolePermissionServiceImpl 实例并注入运行所需依赖。
     * @param roleMapper 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param permissionMapper 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param userRoleMapper 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public RolePermissionServiceImpl(SysRoleMapper roleMapper, SysPermissionMapper permissionMapper,
                                 SysUserRoleMapper userRoleMapper) {
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.userRoleMapper = userRoleMapper;
    }

    /**
     * 查询业务数据集合，并按调用场景组织返回结构。
     * @param userId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public List<String> roles(Long userId) {
        return roleMapper.selectByUserId(userId).stream().map(SysRole::getRoleCode).toList();
    }

    /**
     * 查询业务数据集合，并按调用场景组织返回结构。
     * @param userId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public List<String> permissions(Long userId) {
        return permissionMapper.selectByUserId(userId).stream().map(SysPermission::getPermissionCode).toList();
    }

    /**
     * 更新业务状态，并保持相关数据的一致性。
     * @param userId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param roleCodes 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
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

