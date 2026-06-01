package com.exam.ai.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.ai.common.exception.BusinessException;
import com.exam.ai.security.UserPrincipal;
import com.exam.ai.system.entity.SysConfig;
import com.exam.ai.system.dto.SystemConfigResponse;
import com.exam.ai.system.dto.SystemConfigUpdateResult;
import com.exam.ai.system.dto.UpdateSystemConfigRequest;
import com.exam.ai.system.mapper.SysConfigMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemConfigService {

    public static final String AI_TAGGING_MAX_RETRIES = "ai.tagging.max-retries";
    public static final String AI_DOCUMENT_ANALYSIS_MAX_RETRIES = "ai.document-analysis.max-retries";
    private static final int DEFAULT_AI_TAGGING_MAX_RETRIES = 3;
    private static final int DEFAULT_AI_DOCUMENT_ANALYSIS_MAX_RETRIES = 1;
    private static final String DOCUMENT_ANALYSIS_RETRY_CLAMP_MESSAGE = "文档 AI 解析重试次数不建议超过 3 次，已自动设置为 3 次";

    private final SysConfigMapper configMapper;

    public SystemConfigService(SysConfigMapper configMapper) {
        this.configMapper = configMapper;
    }

    public List<SystemConfigResponse> listConfigs() {
        return configMapper.selectList(new LambdaQueryWrapper<SysConfig>().orderByAsc(SysConfig::getConfigKey))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public int aiTaggingMaxRetries() {
        SysConfig config = configMapper.selectById(AI_TAGGING_MAX_RETRIES);
        if (config == null) {
            return DEFAULT_AI_TAGGING_MAX_RETRIES;
        }
        return parseRetryCount(config.getConfigValue());
    }

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

    private int parseRetryCount(String value) {
        return parseRetryCount(value, "AI 标签最大重试次数必须是整数");
    }

    private int parseRetryCount(String value, String errorMessage) {
        try {
            return Integer.parseInt(value == null ? "" : value.trim());
        } catch (NumberFormatException ex) {
            throw BusinessException.badRequest(errorMessage);
        }
    }

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

    private record ValidatedConfigValue(String value, String message) {
    }
}

