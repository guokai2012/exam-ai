package com.exam.ai.security;

import com.exam.ai.common.config.SecurityProperties;
import com.exam.ai.user.entity.SysUser;
import com.exam.ai.user.entity.UserStatus;
import com.exam.ai.user.mapper.SysUserMapper;
import com.exam.ai.user.service.RolePermissionService;
import com.exam.ai.util.CurrentUserUtils;
import jakarta.servlet.FilterChain;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * JWT 认证过滤器测试，覆盖访问令牌角色快照与数据库实时角色不一致时的上下文绑定行为。
 */
class JwtAuthenticationFilterTest {

    @AfterEach
    void tearDown() {
        CurrentUserUtils.clear();
        SecurityContextHolder.clearContext();
    }

    /**
     * 认证时应使用数据库实时角色权限，避免前端 /me 显示 ADMIN 但后端上下文仍使用旧 TEACHER 快照。
     *
     * @throws Exception 过滤器执行链异常时由测试框架报告失败。
     */
    @Test
    @SuppressWarnings("unchecked")
    void authenticateUsesLatestRolesAndPermissionsFromDatabase() throws Exception {
        SecurityPropertiesBuilder propertiesBuilder = new SecurityPropertiesBuilder();
        JwtService jwtService = new JwtService(propertiesBuilder.properties());
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        SysUserMapper userMapper = mock(SysUserMapper.class);
        RolePermissionService rolePermissionService = mock(RolePermissionService.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, redisTemplate, userMapper, rolePermissionService);

        String sessionId = "session-admin";
        String token = jwtService.createAccessToken(1L, "admin", sessionId, List.of("TEACHER"), List.of("question:list"));
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("admin");
        user.setStatus(UserStatus.ENABLED);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(RedisKeys.session(1L))).thenReturn(sessionId);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(rolePermissionService.roles(1L)).thenReturn(List.of("ADMIN"));
        when(rolePermissionService.permissions(1L)).thenReturn(List.of("admin:user:list"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        doAnswer(invocation -> {
            UserPrincipal principal = CurrentUserUtils.requireCurrentUser();
            assertThat(principal.roles()).containsExactly("ADMIN");
            assertThat(principal.permissions()).containsExactly("admin:user:list");
            assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                    .extracting(Object::toString)
                    .contains("ROLE_ADMIN", "admin:user:list");
            return null;
        }).when(chain).doFilter(any(), any());

        filter.doFilter(request, response, chain);
    }

    /**
     * 测试专用安全配置构造器，用固定密钥和过期时间生成可解析的访问令牌。
     */
    private static final class SecurityPropertiesBuilder {

        private static final String JWT_SECRET = "0123456789012345678901234567890123456789012345678901234567890123";

        /**
         * 创建 JWT 测试配置。
         *
         * @return 已填充密钥和访问令牌有效期的安全配置。
         */
        private SecurityProperties properties() {
            SecurityProperties properties = new SecurityProperties();
            properties.setJwtSecret(JWT_SECRET);
            properties.setAccessTokenTtl(Duration.ofMinutes(30));
            return properties;
        }
    }
}
