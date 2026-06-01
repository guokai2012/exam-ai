package com.exam.ai.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.ai.user.entity.SysRole;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * SysRoleMapper 接口，定义当前业务模块对外提供的服务契约。
 */
public interface SysRoleMapper extends BaseMapper<SysRole> {

    @Select("""
            SELECT r.*
            FROM sys_role r
            JOIN sys_user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId}
              AND r.deleted = 0
              AND ur.deleted = 0
            ORDER BY r.id
            """)
    List<SysRole> selectByUserId(@Param("userId") Long userId);
}

