package com.exam.ai.auth.service.impl;

import com.exam.ai.auth.service.AuthService;
import com.exam.ai.auth.service.CaptchaService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.exam.ai.auth.entity.SysRefreshToken;
import com.exam.ai.auth.vo.CurrentUserResponse;
import com.exam.ai.auth.dto.ChangePasswordRequest;
import com.exam.ai.auth.dto.LoginRequest;
import com.exam.ai.auth.dto.RegisterRequest;
import com.exam.ai.auth.vo.TokenResponse;
import com.exam.ai.auth.mapper.SysRefreshTokenMapper;
import com.exam.ai.common.exception.BusinessException;
import com.exam.ai.common.config.SecurityProperties;
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

/**
 * 认证业务实现类，负责用户注册、登录、刷新令牌、退出登录和当前用户信息组装。
 *
 * <p>该实现统一维护用户会话 Redis Key、刷新令牌数据库记录和 JWT 访问令牌，保证同一用户登录、
 * 刷新和退出时的令牌状态一致。</p>
 */
@Service
public class AuthServiceImpl implements AuthService {

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

    /**
     * 构造 AuthServiceImpl 实例并注入运行所需依赖。
     * @param userMapper 业务参数，参与当前方法的校验、查询或状态变更。
     * @param refreshTokenMapper 业务参数，参与当前方法的校验、查询或状态变更。
     * @param rolePermissionService 业务参数，参与当前方法的校验、查询或状态变更。
     * @param passwordEncoder 业务参数，参与当前方法的校验、查询或状态变更。
     * @param captchaService 业务参数，参与当前方法的校验、查询或状态变更。
     * @param jwtService 业务参数，参与当前方法的校验、查询或状态变更。
     * @param tokenHashService 业务参数，参与当前方法的校验、查询或状态变更。
     * @param redisTemplate 业务参数，参与当前方法的校验、查询或状态变更。
     * @param properties 业务参数，参与当前方法的校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public AuthServiceImpl(SysUserMapper userMapper, SysRefreshTokenMapper refreshTokenMapper,
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

    /**
     * 注册普通学生账号，并为新用户绑定默认学生角色。
     * @param request 业务参数，参与当前方法的校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterRequest request) {
        ensureUsernameAvailable(request.username());
        // 注册入口只保存密码哈希，禁止落库或回传明文密码。
        SysUser user = new SysUser();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setNickname(request.nickname());
        user.setStatus(UserStatus.ENABLED);
        user.setForcePasswordChange(false);
        user.setPasswordUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        // 新注册用户必须拥有基础角色，否则登录后无法获得菜单和接口权限。
        rolePermissionService.replaceUserRoles(user.getId(), List.of(DEFAULT_STUDENT_ROLE));
    }

    /**
     * 校验验证码和账号密码，签发新的访问令牌与刷新令牌。
     * @param request 业务参数，参与当前方法的校验、查询或状态变更。
     * @param ip 业务参数，参与当前方法的校验、查询或状态变更。
     * @param userAgent 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @Transactional(rollbackFor = Exception.class)
    public TokenResponse login(LoginRequest request, String ip, String userAgent) {
        captchaService.verify(request.captchaId(), request.captchaCode());
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, request.username()));
        if (user == null || !Integer.valueOf(UserStatus.ENABLED).equals(user.getStatus())
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw BusinessException.unauthorized();
        }
        // 单用户只保留一个活跃会话，避免旧刷新令牌继续换发访问令牌。
        revokeUserActiveTokens(user.getId(), null);
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(RedisKeys.session(user.getId()), sessionId, properties.getRefreshTokenTtl());
        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);
        return issueTokenPair(user, sessionId, ip, userAgent);
    }

    /**
     * 校验旧刷新令牌并进行令牌轮换，防止刷新令牌被重放使用。
     * @param refreshToken 业务参数，参与当前方法的校验、查询或状态变更。
     * @param ip 业务参数，参与当前方法的校验、查询或状态变更。
     * @param userAgent 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
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
            // 已使用或已吊销的刷新令牌再次出现，按疑似重放处理并清理当前会话。
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
        // 旧刷新令牌只允许使用一次，换发时记录替代哈希便于审计追踪。
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

    /**
     * 退出登录并清理当前会话下的刷新令牌与 Redis 缓存。
     * @param principal 业务参数，参与当前方法的校验、查询或状态变更。
     * @param refreshToken 业务参数，参与当前方法的校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
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

    /**
     * 查询或解析业务数据，返回前端或内部流程需要的结果。
     * @param principal 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
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

    /**
     * 更新业务状态，并保持相关数据的一致性。
     * @param principal 业务参数，参与当前方法的校验、查询或状态变更。
     * @param request 业务参数，参与当前方法的校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
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

    /**
     * 创建业务数据并完成必要的状态初始化。
     * @param user 业务参数，参与当前方法的校验、查询或状态变更。
     * @param sessionId 业务参数，参与当前方法的校验、查询或状态变更。
     * @param ip 业务参数，参与当前方法的校验、查询或状态变更。
     * @param userAgent 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     */
    private TokenResponse issueTokenPair(SysUser user, String sessionId, String ip, String userAgent) {
        List<String> roles = rolePermissionService.roles(user.getId());
        List<String> permissions = rolePermissionService.permissions(user.getId());
        // JWT 中写入角色和权限快照，后续请求无需每次重新查询权限表。
        String accessToken = jwtService.createAccessToken(user.getId(), user.getUsername(), sessionId, roles, permissions);
        String refreshToken = tokenHashService.randomToken();
        String refreshHash = tokenHashService.hash(refreshToken);
        saveRefreshToken(user.getId(), sessionId, refreshHash, ip, userAgent);
        return new TokenResponse(accessToken, refreshToken, "Bearer", Instant.now().plus(properties.getAccessTokenTtl()));
    }

    /**
     * 创建业务数据并完成必要的状态初始化。
     * @param userId 业务参数，参与当前方法的校验、查询或状态变更。
     * @param sessionId 业务参数，参与当前方法的校验、查询或状态变更。
     * @param refreshHash 业务参数，参与当前方法的校验、查询或状态变更。
     * @param ip 业务参数，参与当前方法的校验、查询或状态变更。
     * @param userAgent 业务参数，参与当前方法的校验、查询或状态变更。
     */
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

    /**
     * 校验业务参数或状态，阻止非法流程继续执行。
     * @param username 业务参数，参与当前方法的校验、查询或状态变更。
     */
    private void ensureUsernameAvailable(String username) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (count != null && count > 0) {
            throw BusinessException.conflict("用户名已存在");
        }
    }

    /**
     * 更新业务状态，并保持相关数据的一致性。
     * @param userId 业务参数，参与当前方法的校验、查询或状态变更。
     * @param sessionId 业务参数，参与当前方法的校验、查询或状态变更。
     */
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
            // 逐个删除对应 Redis Key，确保数据库吊销状态与缓存状态保持一致。
            redisTemplate.delete(RedisKeys.refresh(token.getTokenHash()));
            token.setRevokedAt(LocalDateTime.now());
            refreshTokenMapper.updateById(token);
        }
    }
}

