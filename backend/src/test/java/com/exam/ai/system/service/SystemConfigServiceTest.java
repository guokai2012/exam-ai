package com.exam.ai.system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.exam.ai.common.exception.BusinessException;
import com.exam.ai.security.UserPrincipal;
import com.exam.ai.system.entity.SysConfig;
import com.exam.ai.system.vo.SystemConfigUpdateResult;
import com.exam.ai.system.dto.UpdateSystemConfigRequest;
import com.exam.ai.system.mapper.SysConfigMapper;
import com.exam.ai.system.service.impl.SystemConfigServiceImpl;
import com.exam.ai.util.CurrentUserUtils;
import java.util.concurrent.Callable;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SystemConfigServiceTest {

    @Mock
    private SysConfigMapper configMapper;

    @InjectMocks
    private SystemConfigServiceImpl systemConfigService;

    @Test
    void shouldUseDefaultRetryCountWhenConfigMissing() {
        when(configMapper.selectOne(any())).thenReturn(null);

        assertThat(systemConfigService.aiTaggingMaxRetries()).isEqualTo(3);
    }

    @Test
    void shouldUseDefaultDocumentAnalysisRetryCountWhenConfigMissing() {
        when(configMapper.selectOne(any())).thenReturn(null);

        assertThat(systemConfigService.aiDocumentAnalysisMaxRetries()).isEqualTo(1);
    }

    @Test
    void shouldClampRuntimeDocumentAnalysisRetryCountWhenConfigGreaterThanThree() {
        SysConfig config = new SysConfig();
        config.setConfigValue("9");
        when(configMapper.selectOne(any())).thenReturn(config);

        assertThat(systemConfigService.aiDocumentAnalysisMaxRetries()).isEqualTo(3);
    }

    @Test
    void shouldRejectInvalidRetryCount() {
        SysConfig config = new SysConfig();
        config.setConfigKey(SystemConfigService.AI_TAGGING_MAX_RETRIES);
        when(configMapper.selectOne(any())).thenReturn(config);

        assertThatThrownBy(() -> CurrentUserUtils.runAs(principal(), (Callable<SystemConfigUpdateResult>) () -> systemConfigService.updateConfig(
                SystemConfigService.AI_TAGGING_MAX_RETRIES,
                new UpdateSystemConfigRequest("11")
        ))).isInstanceOf(BusinessException.class)
                .hasMessage("AI 标签最大重试次数必须在 0 到 10 之间");
    }

    @Test
    void shouldClampDocumentAnalysisRetryCountWhenGreaterThanThree() {
        SysConfig config = new SysConfig();
        config.setConfigKey(SystemConfigService.AI_DOCUMENT_ANALYSIS_MAX_RETRIES);
        config.setConfigName("文档 AI 解析最大重试次数");
        config.setConfigValue("1");
        config.setValueType("INTEGER");
        when(configMapper.selectOne(any())).thenReturn(config);

        SystemConfigUpdateResult result = CurrentUserUtils.runAs(principal(), (Callable<SystemConfigUpdateResult>) () -> systemConfigService.updateConfig(
                SystemConfigService.AI_DOCUMENT_ANALYSIS_MAX_RETRIES,
                new UpdateSystemConfigRequest("5")
        ));

        assertThat(config.getConfigValue()).isEqualTo("3");
        assertThat(result.message()).isEqualTo("文档 AI 解析重试次数不建议超过 3 次，已自动设置为 3 次");
        verify(configMapper).updateById(config);
    }

    @Test
    void shouldRejectNegativeDocumentAnalysisRetryCount() {
        SysConfig config = new SysConfig();
        config.setConfigKey(SystemConfigService.AI_DOCUMENT_ANALYSIS_MAX_RETRIES);
        when(configMapper.selectOne(any())).thenReturn(config);

        assertThatThrownBy(() -> CurrentUserUtils.runAs(principal(), (Callable<SystemConfigUpdateResult>) () -> systemConfigService.updateConfig(
                SystemConfigService.AI_DOCUMENT_ANALYSIS_MAX_RETRIES,
                new UpdateSystemConfigRequest("-1")
        ))).isInstanceOf(BusinessException.class)
                .hasMessage("文档 AI 解析最大重试次数不能小于 0");
    }

    @Test
    void shouldRejectNonIntegerDocumentAnalysisRetryCount() {
        SysConfig config = new SysConfig();
        config.setConfigKey(SystemConfigService.AI_DOCUMENT_ANALYSIS_MAX_RETRIES);
        when(configMapper.selectOne(any())).thenReturn(config);

        assertThatThrownBy(() -> CurrentUserUtils.runAs(principal(), (Callable<SystemConfigUpdateResult>) () -> systemConfigService.updateConfig(
                SystemConfigService.AI_DOCUMENT_ANALYSIS_MAX_RETRIES,
                new UpdateSystemConfigRequest("abc")
        ))).isInstanceOf(BusinessException.class)
                .hasMessage("文档 AI 解析最大重试次数必须是整数");
    }

    private UserPrincipal principal() {
        return new UserPrincipal(1L, "admin", "session", List.of("ADMIN"), List.of());
    }
}

