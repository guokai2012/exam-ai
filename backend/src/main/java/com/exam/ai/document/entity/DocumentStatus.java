package com.exam.ai.document.entity;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DocumentStatus 类，当前分层的业务组件，负责本模块对应的请求、服务或数据模型职责。
 */
@Schema(description = "文档状态常量")
public final class DocumentStatus {

    public static final String UPLOADED = "UPLOADED";
    public static final String PAGE_RENDERING = "PAGE_RENDERING";
    public static final String PAGE_READY = "PAGE_READY";
    public static final String PAGE_RENDER_FAILED = "PAGE_RENDER_FAILED";
    public static final String PARSING = "PARSING";
    public static final String AI_PARSE_FAILED_REVIEW = "AI_PARSE_FAILED_REVIEW";
    public static final String AI_PARSE_COMPLETE = "AI_PARSE_COMPLETE";
    public static final String RAW_JSON_PROCESSING = "RAW_JSON_PROCESSING";
    public static final String PARSE_FAILED = "PARSE_FAILED";
    public static final String PENDING_CONFIRMATION = "PENDING_CONFIRMATION";
    public static final String CONFIRMED = "CONFIRMED";

    private DocumentStatus() {
    }
}

