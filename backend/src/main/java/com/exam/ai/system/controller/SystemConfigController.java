package com.exam.ai.system.controller;

import com.exam.ai.common.result.ApiResponse;
import com.exam.ai.system.vo.SystemConfigResponse;
import com.exam.ai.system.vo.SystemConfigUpdateResult;
import com.exam.ai.system.dto.UpdateSystemConfigRequest;
import com.exam.ai.system.service.SystemConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * SystemConfigController 类，承载当前分层中的业务职责。
 */
@RestController
@RequestMapping("/api/system-configs")
@Tag(name = "系统配置接口", description = "系统配置项查询和维护")
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    /**
     * 构造 SystemConfigController 实例并注入运行所需依赖。
     * @param systemConfigService 业务参数，参与当前方法的校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public SystemConfigController(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    /**
     * 执行当前业务步骤，维护调用方需要的处理结果。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @GetMapping
    @PreAuthorize("hasAuthority('system-config:list')")
    @Operation(summary = "系统配置列表", description = "查询当前可管理的系统配置项。")
    public ApiResponse<List<SystemConfigResponse>> configs() {
        return ApiResponse.ok(systemConfigService.listConfigs());
    }

    /**
     * 更新业务状态，并保持相关数据的一致性。
     * @param key 业务参数，参与当前方法的校验、查询或状态变更。
     * @param request 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @PutMapping("/{key}")
    @PreAuthorize("hasAuthority('system-config:update')")
    @Operation(summary = "更新系统配置", description = "按配置键更新配置值。")
    public ApiResponse<SystemConfigResponse> update(@Parameter(description = "配置键") @PathVariable String key,
                                                    @Valid @RequestBody UpdateSystemConfigRequest request) {
        SystemConfigUpdateResult result = systemConfigService.updateConfig(key, request);
        return ApiResponse.ok(result.config(), result.message());
    }
}
