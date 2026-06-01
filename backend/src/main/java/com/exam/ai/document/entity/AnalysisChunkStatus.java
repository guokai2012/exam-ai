package com.exam.ai.document.entity;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AnalysisChunkStatus 类，承载当前分层中的业务职责。
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

