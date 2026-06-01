package com.exam.ai.document.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.ai.common.exception.BusinessException;
import com.exam.ai.common.config.AiAnalysisProperties;
import com.exam.ai.document.entity.AnalysisChunkStatus;
import com.exam.ai.document.entity.AnalysisStatus;
import com.exam.ai.document.entity.DocumentStatus;
import com.exam.ai.document.entity.ExamDocument;
import com.exam.ai.document.entity.ExamDocumentAnalysis;
import com.exam.ai.document.entity.ExamDocumentAnalysisChunk;
import com.exam.ai.document.dto.AiQuestionItem;
import com.exam.ai.document.dto.AiQuestionResult;
import com.exam.ai.document.vo.AnalysisResponse;
import com.exam.ai.document.vo.AnalysisSummary;
import com.exam.ai.document.vo.ChunkProgressResponse;
import com.exam.ai.document.vo.DocumentContentResponse;
import com.exam.ai.document.vo.DocumentResponse;
import com.exam.ai.document.vo.QuestionAnalysisResponse;
import com.exam.ai.document.mapper.ExamDocumentAnalysisChunkMapper;
import com.exam.ai.document.mapper.ExamDocumentAnalysisMapper;
import com.exam.ai.document.mapper.ExamDocumentMapper;
import com.exam.ai.question.entity.ExamQuestionSource;
import com.exam.ai.question.dto.QuestionImportResult;
import com.exam.ai.question.vo.QuestionResponse;
import com.exam.ai.question.mapper.ExamQuestionSourceMapper;
import com.exam.ai.question.service.QuestionBankService;
import com.exam.ai.security.UserPrincipal;
import com.exam.ai.system.service.SystemConfigService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * DocumentService 接口，定义当前业务模块对外提供的服务契约。
 */
public interface DocumentService {

    /**
     * 创建业务数据并完成必要的状态初始化。
     * @param file 业务参数，参与当前方法的校验、查询或状态变更。
     * @param principal 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public DocumentResponse upload(MultipartFile file, UserPrincipal principal);
    /**
     * 查询业务数据集合，并按调用场景组织返回结构。
     * @param page 业务参数，参与当前方法的校验、查询或状态变更。
     * @param size 业务参数，参与当前方法的校验、查询或状态变更。
     * @param principal 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public IPage<DocumentResponse> list(long page, long size, UserPrincipal principal);
    /**
     * 查询或解析业务数据，返回前端或内部流程需要的结果。
     * @param id 业务参数，参与当前方法的校验、查询或状态变更。
     * @param principal 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public DocumentResponse detail(Long id, UserPrincipal principal);
    /**
     * 执行当前业务步骤，维护调用方需要的处理结果。
     * @param id 业务参数，参与当前方法的校验、查询或状态变更。
     * @param principal 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public DocumentContentResponse content(Long id, UserPrincipal principal);
    /**
     * 执行当前业务步骤，维护调用方需要的处理结果。
     * @param documentId 业务参数，参与当前方法的校验、查询或状态变更。
     * @param principal 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public AnalysisResponse analyze(Long documentId, UserPrincipal principal);
    /**
     * 查询或解析业务数据，返回前端或内部流程需要的结果。
     * @param documentId 业务参数，参与当前方法的校验、查询或状态变更。
     * @param principal 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public AnalysisResponse latestAnalysis(Long documentId, UserPrincipal principal);
}
