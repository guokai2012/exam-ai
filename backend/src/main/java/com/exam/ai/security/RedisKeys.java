package com.exam.ai.security;

public final class RedisKeys {

    private RedisKeys() {
    }

    public static String captcha(String captchaId) {
        return "captcha:" + captchaId;
    }

    public static String session(Long userId) {
        return "auth:session:" + userId;
    }

    public static String refresh(String tokenHash) {
        return "auth:refresh:" + tokenHash;
    }

    public static String rate(String type, String discriminator) {
        return "rate:" + type + ":" + discriminator;
    }
}
