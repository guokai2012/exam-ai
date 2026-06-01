package com.exam.ai.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.ai.user.entity.SysRole;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface SysRoleMapper extends BaseMapper<SysRole> {

    @Select("""
            SELECT r.*
            FROM sys_role r
            JOIN sys_user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId}
            ORDER BY r.id
            """)
    List<SysRole> selectByUserId(@Param("userId") Long userId);
}

