package com.exam.ai.security;

import com.exam.ai.common.result.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * JsonSecurityHandlers 类，当前分层的业务组件，负责本模块对应的请求、服务或数据模型职责。
 */
@Component
public class JsonSecurityHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    /**
     * 构造 JsonSecurityHandlers 实例并注入运行所需依赖。
     * @param objectMapper 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public JsonSecurityHandlers(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param request 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param response 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param authException 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @Override
    public void commence(jakarta.servlet.http.HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        write(response, HttpStatus.UNAUTHORIZED, ApiResponse.error("UNAUTHORIZED", "认证失败"));
    }

    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param request 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param response 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param accessDeniedException 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @Override
    public void handle(jakarta.servlet.http.HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        write(response, HttpStatus.FORBIDDEN, ApiResponse.error("FORBIDDEN", "无权访问"));
    }

    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param response 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param status 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param body 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     */
    private void write(HttpServletResponse response, HttpStatus status, ApiResponse<Void> body) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), body);
    }
}
