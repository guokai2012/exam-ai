package com.exam.ai.document.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.exam.ai.document.vo.AnalysisResponse;
import com.exam.ai.document.vo.DocumentContentResponse;
import com.exam.ai.document.vo.DocumentResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * DocumentService 接口，定义当前业务模块对外提供的服务契约。
 */
public interface DocumentService {

    /**
     * 创建业务数据并完成必要的默认状态初始化。
     * @param file 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public DocumentResponse upload(MultipartFile file);
    /**
     * 查询业务数据集合，并按调用场景组织返回结构。
     * @param page 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param size 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public IPage<DocumentResponse> list(long page, long size);
    /**
     * 查询或解析业务数据，返回前端或内部流程需要的结果。
     * @param id 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public DocumentResponse detail(Long id);
    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param id 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public DocumentContentResponse content(Long id);
    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param documentId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public AnalysisResponse analyze(Long documentId);
    /**
     * 查询或解析业务数据，返回前端或内部流程需要的结果。
     * @param documentId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public AnalysisResponse latestAnalysis(Long documentId);
}
