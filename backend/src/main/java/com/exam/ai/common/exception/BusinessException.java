package com.exam.ai.common.exception;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    public BusinessException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public static BusinessException badRequest(String message) {
        return new BusinessException("BAD_REQUEST", message, HttpStatus.BAD_REQUEST);
    }

    public static BusinessException unauthorized() {
        return new BusinessException("UNAUTHORIZED", "认证失败", HttpStatus.UNAUTHORIZED);
    }

    public static BusinessException forbidden() {
        return new BusinessException("FORBIDDEN", "无权访问", HttpStatus.FORBIDDEN);
    }

    public static BusinessException tooManyRequests() {
        return new BusinessException("TOO_MANY_REQUESTS", "请求过于频繁，请稍后再试", HttpStatus.TOO_MANY_REQUESTS);
    }

    public static BusinessException conflict(String message) {
        return new BusinessException("CONFLICT", message, HttpStatus.CONFLICT);
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
