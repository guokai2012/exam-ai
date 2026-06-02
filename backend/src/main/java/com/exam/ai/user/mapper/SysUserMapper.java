package com.exam.ai.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.ai.user.entity.SysUser;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * SysUserMapper 接口，定义当前业务模块对外提供的服务契约。
 */
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 查询拥有指定角色的有效用户。
     *
     * @param roleCode 角色编码。
     * @return 绑定该角色且未逻辑删除的用户列表。
     */
    @Select("""
            SELECT DISTINCT u.*
            FROM sys_user u
            JOIN sys_user_role ur ON ur.user_id = u.id
            JOIN sys_role r ON r.id = ur.role_id
            WHERE r.role_code = #{roleCode}
              AND u.deleted = 0
              AND ur.deleted = 0
              AND r.deleted = 0
            ORDER BY u.id
            """)
    List<SysUser> selectByRoleCode(@Param("roleCode") String roleCode);
}

