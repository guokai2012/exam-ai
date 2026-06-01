package com.exam.ai.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.exam.ai.auth.entity.SysRefreshToken;
import com.exam.ai.auth.dto.CurrentUserResponse;
import com.exam.ai.auth.dto.ChangePasswordRequest;
import com.exam.ai.auth.dto.LoginRequest;
import com.exam.ai.auth.dto.RegisterRequest;
import com.exam.ai.auth.dto.TokenResponse;
import com.exam.ai.auth.mapper.SysRefreshTokenMapper;
import com.exam.ai.common.exception.BusinessException;
import com.exam.ai.config.SecurityProperties;
import com.exam.ai.security.JwtService;
import com.exam.ai.security.RedisKeys;
import com.exam.ai.security.TokenHashService;
import com.exam.ai.security.UserPrincipal;
import com.exam.ai.user.entity.SysUser;
import com.exam.ai.user.entity.UserStatus;
import com.exam.ai.user.mapper.SysUserMapper;
import com.exam.ai.user.service.RolePermissionService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final String DEFAULT_STUDENT_ROLE = "STUDENT";

    private final SysUserMapper userMapper;
    private final SysRefreshTokenMapper refreshTokenMapper;
    private final RolePermissionService rolePermissionService;
    private final PasswordEncoder passwordEncoder;
    private final CaptchaService captchaService;
    private final JwtService jwtService;
    private final TokenHashService tokenHashService;
    private final StringRedisTemplate redisTemplate;
    private final SecurityProperties properties;

    public AuthService(SysUserMapper userMapper, SysRefreshTokenMapper refreshTokenMapper,
                       RolePermissionService rolePermissionService, PasswordEncoder passwordEncoder,
                       CaptchaService captchaService, JwtService jwtService, TokenHashService tokenHashService,
                       StringRedisTemplate redisTemplate, SecurityProperties properties) {
        this.userMapper = userMapper;
        this.refreshTokenMapper = refreshTokenMapper;
        this.rolePermissionService = rolePermissionService;
        this.passwordEncoder = passwordEncoder;
        this.captchaService = captchaService;
        this.jwtService = jwtService;
        this.tokenHashService = tokenHashService;
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterRequest request) {
        ensureUsernameAvailable(request.username());
        SysUser user = new SysUser();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setNickname(request.nickname());
        user.setStatus(UserStatus.ENABLED);
        user.setForcePasswordChange(false);
        user.setPasswordUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        rolePermissionService.replaceUserRoles(user.getId(), List.of(DEFAULT_STUDENT_ROLE));
    }

    @Transactional(rollbackFor = Exception.class)
    public TokenResponse login(LoginRequest request, String ip, String userAgent) {
        captchaService.verify(request.captchaId(), request.captchaCode());
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, request.username()));
        if (user == null || !Integer.valueOf(UserStatus.ENABLED).equals(user.getStatus())
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw BusinessException.unauthorized();
        }
        revokeUserActiveTokens(user.getId(), null);
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(RedisKeys.session(user.getId()), sessionId, properties.getRefreshTokenTtl());
        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);
        return issueTokenPair(user, sessionId, ip, userAgent);
    }

    @Transactional(rollbackFor = Exception.class)
    public TokenResponse refresh(String refreshToken, String ip, String userAgent) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw BusinessException.unauthorized();
        }
        String oldHash = tokenHashService.hash(refreshToken);
        String redisValue = redisTemplate.opsForValue().get(RedisKeys.refresh(oldHash));
        SysRefreshToken stored = refreshTokenMapper.selectOne(
                new LambdaQueryWrapper<SysRefreshToken>().eq(SysRefreshToken::getTokenHash, oldHash));
        if (stored == null) {
            throw BusinessException.unauthorized();
        }
        if (stored.getUsedAt() != null || stored.getRevokedAt() != null) {
            revokeUserActiveTokens(stored.getUserId(), stored.getSessionId());
            throw BusinessException.unauthorized();
        }
        if (redisValue == null || stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw BusinessException.unauthorized();
        }
        SysUser user = userMapper.selectById(stored.getUserId());
        if (user == null || !Integer.valueOf(UserStatus.ENABLED).equals(user.getStatus())) {
            throw BusinessException.unauthorized();
        }
        String currentSession = redisTemplate.opsForValue().get(RedisKeys.session(user.getId()));
        if (!stored.getSessionId().equals(currentSession)) {
            throw BusinessException.unauthorized();
        }
        String newRefreshToken = tokenHashService.randomToken();
        String newHash = tokenHashService.hash(newRefreshToken);
        stored.setUsedAt(LocalDateTime.now());
        stored.setReplacedByHash(newHash);
        refreshTokenMapper.updateById(stored);
        redisTemplate.delete(RedisKeys.refresh(oldHash));
        saveRefreshToken(user.getId(), stored.getSessionId(), newHash, ip, userAgent);
        List<String> roles = rolePermissionService.roles(user.getId());
        List<String> permissions = rolePermissionService.permissions(user.getId());
        String accessToken = jwtService.createAccessToken(user.getId(), user.getUsername(), stored.getSessionId(), roles, permissions);
        return new TokenResponse(accessToken, newRefreshToken, "Bearer", Instant.now().plus(properties.getAccessTokenTtl()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void logout(UserPrincipal principal, String refreshToken) {
        if (principal == null) {
            throw BusinessException.unauthorized();
        }
        redisTemplate.delete(RedisKeys.session(principal.userId()));
        if (refreshToken != null && !refreshToken.isBlank()) {
            String hash = tokenHashService.hash(refreshToken);
            redisTemplate.delete(RedisKeys.refresh(hash));
            refreshTokenMapper.update(null, new LambdaUpdateWrapper<SysRefreshToken>()
                    .eq(SysRefreshToken::getTokenHash, hash)
                    .isNull(SysRefreshToken::getRevokedAt)
                    .set(SysRefreshToken::getRevokedAt, LocalDateTime.now()));
        }
        revokeUserActiveTokens(principal.userId(), principal.sessionId());
    }

    public CurrentUserResponse currentUser(UserPrincipal principal) {
        SysUser user = userMapper.selectById(principal.userId());
        if (user == null || !Integer.valueOf(UserStatus.ENABLED).equals(user.getStatus())) {
            throw BusinessException.unauthorized();
        }
        return new CurrentUserResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                rolePermissionService.roles(user.getId()),
                rolePermissionService.permissions(user.getId()),
                Boolean.TRUE.equals(user.getForcePasswordChange())
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public void changePassword(UserPrincipal principal, ChangePasswordRequest request) {
        if (principal == null) {
            throw BusinessException.unauthorized();
        }
        SysUser user = userMapper.selectById(principal.userId());
        if (user == null || !Integer.valueOf(UserStatus.ENABLED).equals(user.getStatus())) {
            throw BusinessException.unauthorized();
        }
        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw BusinessException.badRequest("原密码不正确");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw BusinessException.badRequest("新密码不能与原密码相同");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setForcePasswordChange(false);
        user.setPasswordUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    private TokenResponse issueTokenPair(SysUser user, String sessionId, String ip, String userAgent) {
        List<String> roles = rolePermissionService.roles(user.getId());
        List<String> permissions = rolePermissionService.permissions(user.getId());
        String accessToken = jwtService.createAccessToken(user.getId(), user.getUsername(), sessionId, roles, permissions);
        String refreshToken = tokenHashService.randomToken();
        String refreshHash = tokenHashService.hash(refreshToken);
        saveRefreshToken(user.getId(), sessionId, refreshHash, ip, userAgent);
        return new TokenResponse(accessToken, refreshToken, "Bearer", Instant.now().plus(properties.getAccessTokenTtl()));
    }

    private void saveRefreshToken(Long userId, String sessionId, String refreshHash, String ip, String userAgent) {
        SysRefreshToken token = new SysRefreshToken();
        token.setTokenHash(refreshHash);
        token.setUserId(userId);
        token.setSessionId(sessionId);
        token.setExpiresAt(LocalDateTime.ofInstant(Instant.now().plus(properties.getRefreshTokenTtl()), ZoneId.systemDefault()));
        token.setCreatedIp(ip);
        token.setUserAgent(userAgent);
        refreshTokenMapper.insert(token);
        redisTemplate.opsForValue().set(RedisKeys.refresh(refreshHash), userId + ":" + sessionId, properties.getRefreshTokenTtl());
    }

    private void ensureUsernameAvailable(String username) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (count != null && count > 0) {
            throw BusinessException.conflict("用户名已存在");
        }
    }

    private void revokeUserActiveTokens(Long userId, String sessionId) {
        LambdaQueryWrapper<SysRefreshToken> query = new LambdaQueryWrapper<SysRefreshToken>()
                .eq(SysRefreshToken::getUserId, userId)
                .isNull(SysRefreshToken::getRevokedAt)
                .isNull(SysRefreshToken::getUsedAt);
        if (sessionId != null) {
            query.eq(SysRefreshToken::getSessionId, sessionId);
        }
        List<SysRefreshToken> activeTokens = refreshTokenMapper.selectList(query);
        for (SysRefreshToken token : activeTokens) {
            redisTemplate.delete(RedisKeys.refresh(token.getTokenHash()));
            token.setRevokedAt(LocalDateTime.now());
            refreshTokenMapper.updateById(token);
        }
    }
}

