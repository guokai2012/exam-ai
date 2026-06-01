package com.exam.ai.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.exam.ai.config.SecurityProperties;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    @Test
    void createsAndParsesAccessToken() {
        SecurityProperties properties = new SecurityProperties();
        properties.setJwtSecret("test-secret-test-secret-test-secret-32bytes");
        properties.setAccessTokenTtl(Duration.ofMinutes(30));
        JwtService jwtService = new JwtService(properties);

        String token = jwtService.createAccessToken(1L, "alice", "session-1", List.of("ADMIN"), List.of("user:read"));
        JwtClaims claims = jwtService.parse(token);

        assertThat(claims.userId()).isEqualTo(1L);
        assertThat(claims.username()).isEqualTo("alice");
        assertThat(claims.sessionId()).isEqualTo("session-1");
        assertThat(claims.roles()).containsExactly("ADMIN");
        assertThat(claims.permissions()).containsExactly("user:read");
    }
}
