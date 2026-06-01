package com.exam.ai.document.entity;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DocumentStatus 类，承载当前分层中的业务职责。
 */
@Schema(description = "文档状态常量")
public final class DocumentStatus {

    public static final String UPLOADED = "UPLOADED";
    public static final String PARSING = "PARSING";
    public static final String PARSE_PARTIAL_FAILED = "PARSE_PARTIAL_FAILED";
    public static final String PARSE_FAILED = "PARSE_FAILED";
    public static final String PENDING_CONFIRMATION = "PENDING_CONFIRMATION";
    public static final String CONFIRMED = "CONFIRMED";

    private DocumentStatus() {
    }
}

