package com.exam.ai.common.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.ai.user.entity.SysUser;
import com.exam.ai.user.entity.UserStatus;
import com.exam.ai.user.mapper.SysUserMapper;
import com.exam.ai.user.service.RolePermissionService;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * AdminBootstrapRunner 类，当前分层的业务组件，负责本模块对应的请求、服务或数据模型职责。
 */
@Component
@Order(10)
public class AdminBootstrapRunner implements ApplicationRunner {

    private final SecurityProperties properties;
    private final SysUserMapper userMapper;
    private final RolePermissionService rolePermissionService;
    private final PasswordEncoder passwordEncoder;

    /**
     * 构造 AdminBootstrapRunner 实例并注入运行所需依赖。
     * @param properties 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param userMapper 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param rolePermissionService 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param passwordEncoder 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public AdminBootstrapRunner(SecurityProperties properties, SysUserMapper userMapper,
                                RolePermissionService rolePermissionService, PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.userMapper = userMapper;
        this.rolePermissionService = rolePermissionService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param args 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
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

