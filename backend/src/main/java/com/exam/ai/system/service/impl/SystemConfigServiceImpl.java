package com.exam.ai.system.service.impl;

import com.exam.ai.system.service.SystemConfigService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.ai.common.exception.BusinessException;
import com.exam.ai.security.UserPrincipal;
import com.exam.ai.system.entity.SysConfig;
import com.exam.ai.system.vo.SystemConfigResponse;
import com.exam.ai.system.vo.SystemConfigUpdateResult;
import com.exam.ai.system.dto.UpdateSystemConfigRequest;
import com.exam.ai.system.mapper.SysConfigMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * SystemConfigServiceImpl 类，承载当前分层中的业务职责。
 */
@Service
public class SystemConfigServiceImpl implements SystemConfigService {

    public static final String AI_TAGGING_MAX_RETRIES = "ai.tagging.max-retries";
    public static final String AI_DOCUMENT_ANALYSIS_MAX_RETRIES = "ai.document-analysis.max-retries";
    private static final int DEFAULT_AI_TAGGING_MAX_RETRIES = 3;
    private static final int DEFAULT_AI_DOCUMENT_ANALYSIS_MAX_RETRIES = 1;
    private static final String DOCUMENT_ANALYSIS_RETRY_CLAMP_MESSAGE = "文档 AI 解析重试次数不建议超过 3 次，已自动设置为 3 次";

    private final SysConfigMapper configMapper;

    /**
     * 构造 SystemConfigServiceImpl 实例并注入运行所需依赖。
     * @param configMapper 业务参数，参与当前方法的校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public SystemConfigServiceImpl(SysConfigMapper configMapper) {
        this.configMapper = configMapper;
    }

    /**
     * 查询业务数据集合，并按调用场景组织返回结构。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public List<SystemConfigResponse> listConfigs() {
        return configMapper.selectList(new LambdaQueryWrapper<SysConfig>().orderByAsc(SysConfig::getConfigKey))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 执行当前业务步骤，维护调用方需要的处理结果。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public int aiTaggingMaxRetries() {
        SysConfig config = configMapper.selectById(AI_TAGGING_MAX_RETRIES);
        if (config == null) {
            return DEFAULT_AI_TAGGING_MAX_RETRIES;
        }
        return parseRetryCount(config.getConfigValue());
    }

    /**
     * 执行当前业务步骤，维护调用方需要的处理结果。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public int aiDocumentAnalysisMaxRetries() {
        SysConfig config = configMapper.selectById(AI_DOCUMENT_ANALYSIS_MAX_RETRIES);
        if (config == null) {
            return DEFAULT_AI_DOCUMENT_ANALYSIS_MAX_RETRIES;
        }
        int retries = parseRetryCount(config.getConfigValue(), "文档 AI 解析最大重试次数必须是整数");
        if (retries < 0) {
            throw BusinessException.badRequest("文档 AI 解析最大重试次数不能小于 0");
        }
        return Math.min(retries, 3);
    }

    /**
     * 更新业务状态，并保持相关数据的一致性。
     * @param key 业务参数，参与当前方法的校验、查询或状态变更。
     * @param request 业务参数，参与当前方法的校验、查询或状态变更。
     * @param principal 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @Transactional(rollbackFor = Exception.class)
    public SystemConfigUpdateResult updateConfig(String key, UpdateSystemConfigRequest request, UserPrincipal principal) {
        SysConfig config = configMapper.selectById(key);
        if (config == null) {
            throw BusinessException.badRequest("系统配置不存在");
        }
        ValidatedConfigValue validated = validateValue(key, request.configValue());
        config.setConfigValue(validated.value());
        config.setUpdatedBy(principal.userId());
        configMapper.updateById(config);
        return new SystemConfigUpdateResult(toResponse(configMapper.selectById(key)), validated.message());
    }

    /**
     * 校验业务参数或状态，阻止非法流程继续执行。
     * @param key 业务参数，参与当前方法的校验、查询或状态变更。
     * @param value 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     */
    private ValidatedConfigValue validateValue(String key, String value) {
        if (AI_TAGGING_MAX_RETRIES.equals(key)) {
            int retries = parseRetryCount(value);
            if (retries < 0 || retries > 10) {
                throw BusinessException.badRequest("AI 标签最大重试次数必须在 0 到 10 之间");
            }
            return new ValidatedConfigValue(String.valueOf(retries), "success");
        }
        if (AI_DOCUMENT_ANALYSIS_MAX_RETRIES.equals(key)) {
            int retries = parseRetryCount(value, "文档 AI 解析最大重试次数必须是整数");
            if (retries < 0) {
                throw BusinessException.badRequest("文档 AI 解析最大重试次数不能小于 0");
            }
            if (retries > 3) {
                return new ValidatedConfigValue("3", DOCUMENT_ANALYSIS_RETRY_CLAMP_MESSAGE);
            }
            return new ValidatedConfigValue(String.valueOf(retries), "success");
        }
        return new ValidatedConfigValue(value.trim(), "success");
    }

    /**
     * 查询或解析业务数据，返回前端或内部流程需要的结果。
     * @param value 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     */
    private int parseRetryCount(String value) {
        return parseRetryCount(value, "AI 标签最大重试次数必须是整数");
    }

    /**
     * 查询或解析业务数据，返回前端或内部流程需要的结果。
     * @param value 业务参数，参与当前方法的校验、查询或状态变更。
     * @param errorMessage 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     */
    private int parseRetryCount(String value, String errorMessage) {
        try {
            return Integer.parseInt(value == null ? "" : value.trim());
        } catch (NumberFormatException ex) {
            throw BusinessException.badRequest(errorMessage);
        }
    }

    /**
     * 转换业务对象，生成前端返回视图或内部传输结构。
     * @param config 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     */
    private SystemConfigResponse toResponse(SysConfig config) {
        return new SystemConfigResponse(
                config.getConfigKey(),
                config.getConfigValue(),
                config.getConfigName(),
                config.getDescription(),
                config.getValueType(),
                config.getUpdatedAt()
        );
    }

    /**
     * ValidatedConfigValue 记录对象，封装当前业务流程中的不可变数据。
     * @param value 业务参数，参与当前方法的校验、查询或状态变更。
     * @param message 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     */
    private record ValidatedConfigValue(String value, String message) {
    }
}

