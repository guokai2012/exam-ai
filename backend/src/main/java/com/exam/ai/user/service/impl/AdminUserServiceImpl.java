package com.exam.ai.user.service.impl;

import com.exam.ai.user.service.AdminUserService;
import com.exam.ai.user.service.RolePermissionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.ai.auth.entity.SysRefreshToken;
import com.exam.ai.auth.mapper.SysRefreshTokenMapper;
import com.exam.ai.common.exception.BusinessException;
import com.exam.ai.security.RedisKeys;
import com.exam.ai.user.entity.SysUser;
import com.exam.ai.user.entity.UserStatus;
import com.exam.ai.user.dto.AdminCreateUserRequest;
import com.exam.ai.user.dto.AdminUpdateUserRequest;
import com.exam.ai.user.dto.ResetPasswordRequest;
import com.exam.ai.user.vo.UserResponse;
import com.exam.ai.user.mapper.SysUserMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AdminUserServiceImpl 类，当前分层的业务组件，负责本模块对应的请求、服务或数据模型职责。
 */
@Service
public class AdminUserServiceImpl implements AdminUserService {

    private final SysUserMapper userMapper;
    private final SysRefreshTokenMapper refreshTokenMapper;
    private final RolePermissionService rolePermissionService;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;

    /**
     * 构造 AdminUserServiceImpl 实例并注入运行所需依赖。
     * @param userMapper 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param refreshTokenMapper 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param rolePermissionService 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param passwordEncoder 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param redisTemplate 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public AdminUserServiceImpl(SysUserMapper userMapper, SysRefreshTokenMapper refreshTokenMapper,
                            RolePermissionService rolePermissionService, PasswordEncoder passwordEncoder,
                            StringRedisTemplate redisTemplate) {
        this.userMapper = userMapper;
        this.refreshTokenMapper = refreshTokenMapper;
        this.rolePermissionService = rolePermissionService;
        this.passwordEncoder = passwordEncoder;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 查询业务数据集合，并按调用场景组织返回结构。
     * @param page 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param size 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param keyword 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public IPage<UserResponse> list(long page, long size, String keyword) {
        LambdaQueryWrapper<SysUser> query = new LambdaQueryWrapper<SysUser>()
                .orderByDesc(SysUser::getId);
        if (keyword != null && !keyword.isBlank()) {
            query.and(wrapper -> wrapper.like(SysUser::getUsername, keyword).or().like(SysUser::getNickname, keyword));
        }
        IPage<SysUser> result = userMapper.selectPage(Page.of(page, size), query);
        return result.convert(this::toResponse);
    }

    /**
     * 创建业务数据并完成必要的默认状态初始化。
     * @param request 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @Transactional(rollbackFor = Exception.class)
    public UserResponse create(AdminCreateUserRequest request) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, request.username()));
        if (count != null && count > 0) {
            throw BusinessException.conflict("用户名已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setNickname(request.nickname());
        user.setStatus(UserStatus.ENABLED);
        user.setForcePasswordChange(true);
        userMapper.insert(user);
        rolePermissionService.replaceUserRoles(user.getId(), request.roles());
        return toResponse(userMapper.selectById(user.getId()));
    }

    /**
     * 更新业务状态，并保持相关数据的一致性。
     * @param id 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param request 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @Transactional(rollbackFor = Exception.class)
    public UserResponse update(Long id, AdminUpdateUserRequest request) {
        SysUser user = requireUser(id);
        user.setNickname(request.nickname());
        user.setStatus(request.status());
        userMapper.updateById(user);
        rolePermissionService.replaceUserRoles(id, request.roles());
        if (!Integer.valueOf(UserStatus.ENABLED).equals(request.status())) {
            revokeUserSessions(id);
        }
        return toResponse(userMapper.selectById(id));
    }

    /**
     * 删除或失效指定业务数据，并同步清理关联状态。
     * @param id 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @Transactional(rollbackFor = Exception.class)
    public void disable(Long id) {
        SysUser user = requireUser(id);
        user.setStatus(UserStatus.DISABLED);
        userMapper.updateById(user);
        revokeUserSessions(id);
    }

    /**
     * 校验业务参数或业务状态，阻止非法流程继续执行。
     * @param id 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     */
    private SysUser requireUser(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw BusinessException.badRequest("用户不存在");
        }
        return user;
    }

    /**
     * 转换业务对象，生成前端返回视图或内部传输结构。
     * @param user 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     */
    private UserResponse toResponse(SysUser user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getStatus(),
                rolePermissionService.roles(user.getId()),
                Boolean.TRUE.equals(user.getForcePasswordChange()),
                user.getLastLoginAt(),
                user.getCreatedAt()
        );
    }

    /**
     * 更新业务状态，并保持相关数据的一致性。
     * @param userId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     */
    private void revokeUserSessions(Long userId) {
        redisTemplate.delete(RedisKeys.session(userId));
        List<SysRefreshToken> tokens = refreshTokenMapper.selectList(new LambdaQueryWrapper<SysRefreshToken>()
                .eq(SysRefreshToken::getUserId, userId)
                .isNull(SysRefreshToken::getRevokedAt)
                .isNull(SysRefreshToken::getUsedAt));
        for (SysRefreshToken token : tokens) {
            redisTemplate.delete(RedisKeys.refresh(token.getTokenHash()));
            token.setRevokedAt(LocalDateTime.now());
            refreshTokenMapper.updateById(token);
        }
    }

    /**
     * 删除或失效指定业务数据，并同步清理关联状态。
     * @param id 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @Transactional(rollbackFor = Exception.class)
    public void kick(Long id) {
        requireUser(id);
        revokeUserSessions(id);
    }

    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param id 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param request 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @Transactional(rollbackFor = Exception.class)
    public UserResponse resetPassword(Long id, ResetPasswordRequest request) {
        SysUser user = requireUser(id);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setForcePasswordChange(true);
        user.setPasswordUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        revokeUserSessions(id);
        return toResponse(userMapper.selectById(id));
    }
}

