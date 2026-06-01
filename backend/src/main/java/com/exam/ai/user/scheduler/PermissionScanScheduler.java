package com.exam.ai.user.scheduler;

import com.exam.ai.user.vo.PermissionScanResponse;
import com.exam.ai.user.service.AdminPermissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * PermissionScanScheduler 类，承载当前分层中的业务职责。
 */
@Component
public class PermissionScanScheduler {

    private static final Logger log = LoggerFactory.getLogger(PermissionScanScheduler.class);

    private final AdminPermissionService permissionService;

    /**
     * 创建接口权限扫描定时调度器。
     *
     * @param permissionService 权限管理业务服务
     */
    public PermissionScanScheduler(AdminPermissionService permissionService) {
        this.permissionService = permissionService;
    }

    /**
     * 每 10 分钟扫描 Controller 权限注解，同步动作权限树。
     */
    @Scheduled(
            fixedDelayString = "${app.permission.scan-delay:600000}",
            initialDelayString = "${app.permission.scan-initial-delay:60000}"
    )
    /**
     * 更新业务状态，并保持相关数据的一致性。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public void syncControllerPermissions() {
        try {
            // 扫描结果会自动新增、更新和删除扫描来源的动作权限。
            PermissionScanResponse result = permissionService.scanControllerPermissions();
            log.info("权限扫描完成，新增 {}，更新 {}，删除 {}", result.created(), result.updated(), result.deleted());
        } catch (Exception ex) {
            log.warn("权限扫描失败", ex);
        }
    }
}
