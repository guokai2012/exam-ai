package com.exam.ai.user.service;

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
import com.exam.ai.user.dto.UserResponse;
import com.exam.ai.user.mapper.SysUserMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {

    private final SysUserMapper userMapper;
    private final SysRefreshTokenMapper refreshTokenMapper;
    private final RolePermissionService rolePermissionService;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;

    public AdminUserService(SysUserMapper userMapper, SysRefreshTokenMapper refreshTokenMapper,
                            RolePermissionService rolePermissionService, PasswordEncoder passwordEncoder,
                            StringRedisTemplate redisTemplate) {
        this.userMapper = userMapper;
        this.refreshTokenMapper = refreshTokenMapper;
        this.rolePermissionService = rolePermissionService;
        this.passwordEncoder = passwordEncoder;
        this.redisTemplate = redisTemplate;
    }

    public IPage<UserResponse> list(long page, long size, String keyword) {
        LambdaQueryWrapper<SysUser> query = new LambdaQueryWrapper<SysUser>()
                .orderByDesc(SysUser::getId);
        if (keyword != null && !keyword.isBlank()) {
            query.and(wrapper -> wrapper.like(SysUser::getUsername, keyword).or().like(SysUser::getNickname, keyword));
        }
        IPage<SysUser> result = userMapper.selectPage(Page.of(page, size), query);
        return result.convert(this::toResponse);
    }

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

    @Transactional(rollbackFor = Exception.class)
    public void disable(Long id) {
        SysUser user = requireUser(id);
        user.setStatus(UserStatus.DISABLED);
        userMapper.updateById(user);
        revokeUserSessions(id);
    }

    private SysUser requireUser(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw BusinessException.badRequest("用户不存在");
        }
        return user;
    }

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

    @Transactional(rollbackFor = Exception.class)
    public void kick(Long id) {
        requireUser(id);
        revokeUserSessions(id);
    }

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

