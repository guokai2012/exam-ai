package com.exam.ai.security;

import jakarta.servlet.http.HttpServletRequest;

/**
 * ClientIp 类，当前分层的业务组件，负责本模块对应的请求、服务或数据模型职责。
 */
public final class ClientIp {

    private ClientIp() {
    }

    /**
     * 查询或解析业务数据，返回前端或内部流程需要的结果。
     * @param request 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public static String resolve(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
