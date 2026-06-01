package com.exam.ai.system.controller;

import com.exam.ai.common.api.ApiResponse;
import com.exam.ai.security.UserPrincipal;
import com.exam.ai.system.dto.SystemConfigResponse;
import com.exam.ai.system.dto.SystemConfigUpdateResult;
import com.exam.ai.system.dto.UpdateSystemConfigRequest;
import com.exam.ai.system.service.SystemConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system-configs")
@Tag(name = "系统配置接口", description = "系统配置项查询和维护")
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    public SystemConfigController(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('system-config:list')")
    @Operation(summary = "系统配置列表", description = "查询当前可管理的系统配置项。")
    public ApiResponse<List<SystemConfigResponse>> configs() {
        return ApiResponse.ok(systemConfigService.listConfigs());
    }

    @PutMapping("/{key}")
    @PreAuthorize("hasAuthority('system-config:update')")
    @Operation(summary = "更新系统配置", description = "按配置键更新配置值。")
    public ApiResponse<SystemConfigResponse> update(@Parameter(description = "配置键") @PathVariable String key,
                                                    @Valid @RequestBody UpdateSystemConfigRequest request,
                                                    @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        SystemConfigUpdateResult result = systemConfigService.updateConfig(key, request, principal);
        return ApiResponse.ok(result.config(), result.message());
    }
}
