package com.exam.ai.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.exam.ai.common.exception.BusinessException;
import com.exam.ai.security.RedisKeys;
import com.exam.ai.security.TokenHashService;
import com.exam.ai.security.UserPrincipal;
import com.exam.ai.system.service.SystemConfigService;
import com.exam.ai.user.vo.MenuScanTokenResponse;
import com.exam.ai.util.CurrentUserUtils;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class MenuScanTokenServiceTest {

    /**
     * 清理当前用户上下文，避免测试之间互相污染。
     */
    @AfterEach
    void tearDown() {
        CurrentUserUtils.clear();
    }

    /**
     * 验证 ADMIN 首次获取菜单扫描 Token 成功，并按配置 TTL 写入 Redis。
     */
    @Test
    void issueTokenForAdminStoresHashWithConfiguredTtl() {
        Dependencies dependencies = dependencies();
        CurrentUserUtils.setCurrentUser(adminPrincipal("session-a"));
        when(dependencies.valueOperations.setIfAbsent(eq(RedisKeys.menuScanTokenLimit(1L, "session-a")), eq("1"), eq(Duration.ofMinutes(5))))
                .thenReturn(true);
        when(dependencies.systemConfigService.menuScanTokenTtlSeconds()).thenReturn(30);
        when(dependencies.tokenHashService.randomToken()).thenReturn("raw-token");
        when(dependencies.tokenHashService.hash("raw-token")).thenReturn("token-hash");

        MenuScanTokenResponse response = dependencies.service.issueToken();

        assertThat(response.token()).isEqualTo("raw-token");
        verify(dependencies.valueOperations).set(RedisKeys.menuScanToken("token-hash"), "1:session-a", Duration.ofSeconds(30));
    }

    /**
     * 验证同一用户会话 5 分钟内重复获取菜单扫描 Token 会被限流。
     */
    @Test
    void issueTokenRejectsRepeatedRequestInLimitWindow() {
        Dependencies dependencies = dependencies();
        CurrentUserUtils.setCurrentUser(adminPrincipal("session-a"));
        when(dependencies.valueOperations.setIfAbsent(any(), any(), any(Duration.class))).thenReturn(false);

        assertThatThrownBy(() -> dependencies.service.issueToken())
                .isInstanceOf(BusinessException.class)
                .hasMessage("菜单扫描操作过于频繁，请 5 分钟后再试");
    }

    /**
     * 验证非 ADMIN 用户即使拥有普通登录上下文也不能签发菜单扫描 Token。
     */
    @Test
    void issueTokenRejectsNonAdminUser() {
        Dependencies dependencies = dependencies();
        CurrentUserUtils.setCurrentUser(new UserPrincipal(2L, "teacher", "session-b", List.of("TEACHER"), List.of("admin:menu:scan")));

        assertThatThrownBy(() -> dependencies.service.issueToken())
                .isInstanceOf(BusinessException.class)
                .hasMessage("无权访问");
    }

    /**
     * 验证同步接口消费 Token 后会删除 Redis Key，确保 Token 一次性使用。
     */
    @Test
    void consumeTokenDeletesValidToken() {
        Dependencies dependencies = dependencies();
        CurrentUserUtils.setCurrentUser(adminPrincipal("session-a"));
        when(dependencies.tokenHashService.hash("raw-token")).thenReturn("token-hash");
        when(dependencies.valueOperations.get(RedisKeys.menuScanToken("token-hash"))).thenReturn("1:session-a");

        dependencies.service.consumeToken("raw-token");

        verify(dependencies.redisTemplate).delete(RedisKeys.menuScanToken("token-hash"));
    }

    /**
     * 验证错误、过期或跨会话 Token 无法通过校验。
     */
    @Test
    void consumeTokenRejectsInvalidOwner() {
        Dependencies dependencies = dependencies();
        CurrentUserUtils.setCurrentUser(adminPrincipal("session-a"));
        when(dependencies.tokenHashService.hash("raw-token")).thenReturn("token-hash");
        when(dependencies.valueOperations.get(RedisKeys.menuScanToken("token-hash"))).thenReturn("1:other-session");

        assertThatThrownBy(() -> dependencies.service.consumeToken("raw-token"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("菜单扫描临时 Token 无效或已过期");
    }

    /**
     * 创建 ADMIN 当前用户。
     *
     * @param sessionId 当前会话 ID。
     * @return ADMIN 用户上下文。
     */
    private UserPrincipal adminPrincipal(String sessionId) {
        return new UserPrincipal(1L, "admin", sessionId, List.of("ADMIN"), List.of("admin:menu:scan"));
    }

    /**
     * 创建菜单扫描 Token 服务测试依赖。
     *
     * @return 测试依赖集合。
     */
    private Dependencies dependencies() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        TokenHashService tokenHashService = mock(TokenHashService.class);
        SystemConfigService systemConfigService = mock(SystemConfigService.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        return new Dependencies(
                redisTemplate,
                valueOperations,
                tokenHashService,
                systemConfigService,
                new MenuScanTokenService(redisTemplate, tokenHashService, systemConfigService)
        );
    }

    /**
     * 菜单扫描 Token 服务测试依赖集合。
     *
     * @param redisTemplate Redis 模板。
     * @param valueOperations Redis 字符串操作对象。
     * @param tokenHashService Token 哈希服务。
     * @param systemConfigService 系统配置服务。
     * @param service 被测服务。
     */
    private record Dependencies(
            StringRedisTemplate redisTemplate,
            ValueOperations<String, String> valueOperations,
            TokenHashService tokenHashService,
            SystemConfigService systemConfigService,
            MenuScanTokenService service
    ) {
    }
}
