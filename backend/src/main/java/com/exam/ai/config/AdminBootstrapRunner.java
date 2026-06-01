package com.exam.ai.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.ai.user.entity.SysUser;
import com.exam.ai.user.entity.UserStatus;
import com.exam.ai.user.mapper.SysUserMapper;
import com.exam.ai.user.service.RolePermissionService;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private final SecurityProperties properties;
    private final SysUserMapper userMapper;
    private final RolePermissionService rolePermissionService;
    private final PasswordEncoder passwordEncoder;

    public AdminBootstrapRunner(SecurityProperties properties, SysUserMapper userMapper,
                                RolePermissionService rolePermissionService, PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.userMapper = userMapper;
        this.rolePermissionService = rolePermissionService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(ApplicationArguments args) {
        SecurityProperties.BootstrapAdmin admin = properties.getBootstrapAdmin();
        if (admin == null || admin.getPassword() == null || admin.getPassword().isBlank()) {
            return;
        }
        Long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, admin.getUsername()));
        if (count != null && count > 0) {
            return;
        }
        SysUser user = new SysUser();
        user.setUsername(admin.getUsername());
        user.setPasswordHash(passwordEncoder.encode(admin.getPassword()));
        user.setNickname(admin.getNickname());
        user.setStatus(UserStatus.ENABLED);
        userMapper.insert(user);
        rolePermissionService.replaceUserRoles(user.getId(), List.of("ADMIN"));
    }
}

