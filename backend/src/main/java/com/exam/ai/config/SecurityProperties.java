package com.exam.ai.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    private String jwtSecret;
    private Duration accessTokenTtl = Duration.ofMinutes(30);
    private Duration refreshTokenTtl = Duration.ofDays(14);
    private Duration captchaTtl = Duration.ofMinutes(5);
    private String refreshTokenPepper;
    private String refreshCookieName = "refresh_token";
    private boolean refreshCookieSecure = false;
    private BootstrapAdmin bootstrapAdmin = new BootstrapAdmin();
    private Map<String, RateLimit> rateLimits = new HashMap<>();

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public Duration getAccessTokenTtl() {
        return accessTokenTtl;
    }

    public void setAccessTokenTtl(Duration accessTokenTtl) {
        this.accessTokenTtl = accessTokenTtl;
    }

    public Duration getRefreshTokenTtl() {
        return refreshTokenTtl;
    }

    public void setRefreshTokenTtl(Duration refreshTokenTtl) {
        this.refreshTokenTtl = refreshTokenTtl;
    }

    public Duration getCaptchaTtl() {
        return captchaTtl;
    }

    public void setCaptchaTtl(Duration captchaTtl) {
        this.captchaTtl = captchaTtl;
    }

    public String getRefreshTokenPepper() {
        return refreshTokenPepper;
    }

    public void setRefreshTokenPepper(String refreshTokenPepper) {
        this.refreshTokenPepper = refreshTokenPepper;
    }

    public String getRefreshCookieName() {
        return refreshCookieName;
    }

    public void setRefreshCookieName(String refreshCookieName) {
        this.refreshCookieName = refreshCookieName;
    }

    public boolean isRefreshCookieSecure() {
        return refreshCookieSecure;
    }

    public void setRefreshCookieSecure(boolean refreshCookieSecure) {
        this.refreshCookieSecure = refreshCookieSecure;
    }

    public BootstrapAdmin getBootstrapAdmin() {
        return bootstrapAdmin;
    }

    public void setBootstrapAdmin(BootstrapAdmin bootstrapAdmin) {
        this.bootstrapAdmin = bootstrapAdmin;
    }

    public Map<String, RateLimit> getRateLimits() {
        return rateLimits;
    }

    public void setRateLimits(Map<String, RateLimit> rateLimits) {
        this.rateLimits = rateLimits;
    }

    public RateLimit rateLimit(String name, int defaultLimit, Duration defaultWindow) {
        return rateLimits.getOrDefault(name, new RateLimit(defaultLimit, defaultWindow));
    }

    public record RateLimit(int limit, Duration window) {
    }

    public static class BootstrapAdmin {
        private String username = "admin";
        private String password = "";
        private String nickname = "管理员";

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }
    }
}
