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

/**
 * RolePermissionService 接口，定义当前业务模块对外提供的服务契约。
 */
public interface RolePermissionService {

    /**
     * 查询业务数据集合，并按调用场景组织返回结构。
     * @param userId 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public List<String> roles(Long userId);
    /**
     * 查询业务数据集合，并按调用场景组织返回结构。
     * @param userId 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public List<String> permissions(Long userId);
    /**
     * 更新业务状态，并保持相关数据的一致性。
     * @param userId 业务参数，参与当前方法的校验、查询或状态变更。
     * @param roleCodes 业务参数，参与当前方法的校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public void replaceUserRoles(Long userId, List<String> roleCodes);
}
