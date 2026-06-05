package com.exam.ai.document.service;

import com.exam.ai.document.dto.AiPageAnalysisResult;
import java.util.List;

/**
 * 文档级题目合并客户端。
 *
 * <p>该接口接收所有页级分析片段，由 AI 按页码顺序合并跨页题干、答案和解析，
 * 输出可入库的完整题目 JSON。</p>
 */
public interface DocumentQuestionAssemblerClient {

    /**
     * 调用文档级合并模型，将页级片段整理成完整题目。
     *
     * @param pages 按页码升序排列的页级分析结果。
     * @return 模型返回的完整题目 JSON。
     * @throws com.exam.ai.common.exception.BusinessException 当模型配置缺失、调用失败或响应为空时抛出。
     */
    String assembleQuestions(List<AiPageAnalysisResult> pages);
}
