package com.exam.ai.document.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "文档分析状态常量")
public final class AnalysisStatus {

    public static final String PROCESSING = "PROCESSING";
    public static final String SUCCESS = "SUCCESS";
    public static final String PARTIAL_FAILED = "PARTIAL_FAILED";
    public static final String FAILED = "FAILED";

    private AnalysisStatus() {
    }
}

