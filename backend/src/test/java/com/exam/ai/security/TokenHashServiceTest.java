package com.exam.ai.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.exam.ai.common.config.SecurityProperties;
import org.junit.jupiter.api.Test;

class TokenHashServiceTest {

    @Test
    void createsRandomTokensAndStableHashes() {
        SecurityProperties properties = new SecurityProperties();
        properties.setRefreshTokenPepper("pepper");
        TokenHashService tokenHashService = new TokenHashService(properties);

        String first = tokenHashService.randomToken();
        String second = tokenHashService.randomToken();

        assertThat(first).isNotEqualTo(second);
        assertThat(tokenHashService.hash(first)).isEqualTo(tokenHashService.hash(first));
        assertThat(tokenHashService.hash(first)).isNotEqualTo(tokenHashService.hash(second));
    }
}
