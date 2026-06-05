package com.exam.ai.document.service;

import java.nio.file.Path;

/**
 * PDF 页面图片视觉识别客户端。
 *
 * <p>该接口屏蔽具体 OpenAI-compatible 服务商差异，业务层只关心单页图片识别后的原始
 * JSON 文本。识别结果必须先保存到页 chunk 的 {@code rawJson}，后续由定时任务统一处理。</p>
 */
public interface DocumentVisionRecognitionClient {

    /**
     * 调用多模态模型识别单页 PDF 图片。
     *
     * @param pageImagePath PDF 页渲染后的图片本地路径。
     * @param pageNo 页码，从 1 开始。
     * @return 模型返回的原始 JSON 字符串。
     * @throws com.exam.ai.common.exception.BusinessException 当模型配置缺失、调用失败或响应为空时抛出。
     */
    String recognizePage(Path pageImagePath, Integer pageNo);
}
