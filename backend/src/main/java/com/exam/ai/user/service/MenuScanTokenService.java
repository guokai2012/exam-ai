package com.exam.ai.user.service;

import com.exam.ai.common.exception.BusinessException;
import com.exam.ai.security.RedisKeys;
import com.exam.ai.security.TokenHashService;
import com.exam.ai.security.UserPrincipal;
import com.exam.ai.system.service.SystemConfigService;
import com.exam.ai.user.vo.MenuScanTokenResponse;
import com.exam.ai.util.CurrentUserUtils;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 菜单扫描临时 Token 服务，负责签发、限流和一次性校验菜单同步授权。
 */
@Service
public class MenuScanTokenService {

    public static final String TOKEN_HEADER = "X-Menu-Scan-Token";
    private static final String ADMIN_ROLE = "ADMIN";
    private static final String LIMIT_VALUE = "1";
    private static final Duration TOKEN_ISSUE_LIMIT_WINDOW = Duration.ofMinutes(5);
    private static final String TOKEN_LIMIT_MESSAGE = "菜单扫描操作过于频繁，请 5 分钟后再试";
    private static final String TOKEN_INVALID_MESSAGE = "菜单扫描临时 Token 无效或已过期";

    private final StringRedisTemplate redisTemplate;
    private final TokenHashService tokenHashService;
    private final SystemConfigService systemConfigService;

    /**
     * 构造菜单扫描临时 Token 服务。
     *
     * @param redisTemplate Redis 字符串访问器，用于保存限流键和临时 Token 哈希。
     * @param tokenHashService Token 随机值生成与哈希服务。
     * @param systemConfigService 系统配置服务，用于读取临时 Token 有效期。
     */
    public MenuScanTokenService(StringRedisTemplate redisTemplate,
                                TokenHashService tokenHashService,
                                SystemConfigService systemConfigService) {
        this.redisTemplate = redisTemplate;
        this.tokenHashService = tokenHashService;
        this.systemConfigService = systemConfigService;
    }

    /**
     * 为当前 ADMIN 会话签发菜单扫描临时 Token。
     *
     * @return 菜单扫描临时 Token 和过期时间。
     * @throws BusinessException 当前用户不是 ADMIN，或同一会话 5 分钟内已签发过 Token 时抛出。
     */
    public MenuScanTokenResponse issueToken() {
        UserPrincipal principal = requireAdminPrincipal();
        String limitKey = RedisKeys.menuScanTokenLimit(principal.userId(), principal.sessionId());
        Boolean allowed = redisTemplate.opsForValue().setIfAbsent(limitKey, LIMIT_VALUE, TOKEN_ISSUE_LIMIT_WINDOW);
        if (!Boolean.TRUE.equals(allowed)) {
            throw BusinessException.tooManyRequests(TOKEN_LIMIT_MESSAGE);
        }

        int ttlSeconds = systemConfigService.menuScanTokenTtlSeconds();
        String token = tokenHashService.randomToken();
        String tokenHash = tokenHashService.hash(token);
        Instant expiresAt = Instant.now().plusSeconds(ttlSeconds);
        redisTemplate.opsForValue().set(RedisKeys.menuScanToken(tokenHash), ownerValue(principal), Duration.ofSeconds(ttlSeconds));
        return new MenuScanTokenResponse(token, expiresAt);
    }

    /**
     * 校验并消费当前 ADMIN 会话的菜单扫描临时 Token。
     *
     * @param token 前端通过请求头传入的菜单扫描临时 Token。
     * @throws BusinessException 当前用户不是 ADMIN、Token 缺失、过期、伪造或重复使用时抛出。
     */
    public void consumeToken(String token) {
        UserPrincipal principal = requireAdminPrincipal();
        if (!StringUtils.hasText(token)) {
            throw BusinessException.forbidden();
        }
        String tokenKey = RedisKeys.menuScanToken(tokenHashService.hash(token));
        String storedOwner = redisTemplate.opsForValue().get(tokenKey);
        if (!Objects.equals(storedOwner, ownerValue(principal))) {
            throw BusinessException.badRequest(TOKEN_INVALID_MESSAGE);
        }
        redisTemplate.delete(tokenKey);
    }

    /**
     * 获取当前 ADMIN 用户上下文，服务层再次校验角色以防止接口权限配置误改。
     *
     * @return 当前 ADMIN 用户上下文。
     * @throws BusinessException 当前用户不是 ADMIN 时抛出无权限异常。
     */
    private UserPrincipal requireAdminPrincipal() {
        UserPrincipal principal = CurrentUserUtils.requireCurrentUser();
        if (principal.roles().stream().noneMatch(ADMIN_ROLE::equals)) {
            throw BusinessException.forbidden();
        }
        return principal;
    }

    /**
     * 生成 Token 所属用户会话标识，确保临时 Token 不能跨用户或跨会话使用。
     *
     * @param principal 当前用户上下文。
     * @return 用户会话标识。
     */
    private String ownerValue(UserPrincipal principal) {
        return principal.userId() + ":" + principal.sessionId();
    }
}
