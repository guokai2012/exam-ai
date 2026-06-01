package com.exam.ai.document.entity;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AnalysisChunkStatus 类，当前分层的业务组件，负责本模块对应的请求、服务或数据模型职责。
 */
@Schema(description = "文档分析分块状态常量")
public final class AnalysisChunkStatus {

    public static final String PENDING = "PENDING";
    public static final String PROCESSING = "PROCESSING";
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED";

    private AnalysisChunkStatus() {
    }
}

