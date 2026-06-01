package com.exam.ai.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.ai.user.entity.SysPermission;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * SysPermissionMapper 接口，定义当前业务模块对外提供的服务契约。
 */
public interface SysPermissionMapper extends BaseMapper<SysPermission> {

    @Select("""
            SELECT DISTINCT p.*
            FROM sys_permission p
            JOIN sys_role_permission rp ON rp.permission_id = p.id
            JOIN sys_user_role ur ON ur.role_id = rp.role_id
            WHERE ur.user_id = #{userId}
              AND p.deleted = 0
              AND rp.deleted = 0
              AND ur.deleted = 0
            ORDER BY p.id
            """)
    List<SysPermission> selectByUserId(@Param("userId") Long userId);
}

