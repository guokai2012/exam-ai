package com.exam.ai.user.service;

import com.exam.ai.user.vo.PermissionResponse;
import com.exam.ai.user.vo.PermissionScanResponse;
import java.util.List;

/**
 * 后台权限管理服务契约，提供扫描生成权限和查询权限树能力。
 *
 * <p>权限数据必须来源于 Controller 的 Spring Security 权限表达式扫描结果，
 * 业务代码不得通过该服务手工创建、编辑或删除权限。</p>
 */
public interface AdminPermissionService {

    String TYPE_GROUP = "GROUP";
    String TYPE_MENU = "MENU";
    String TYPE_VIEW = "VIEW";
    String TYPE_ACTION = "ACTION";

    /**
     * 查询当前有效权限树，供角色授权和权限管理页面展示。
     *
     * @return 按 Controller 分组组织后的权限树。
     */
    List<PermissionResponse> list();

    /**
     * 扫描 Controller 接口权限表达式，并以扫描结果全量同步权限表。
     *
     * @return 本次扫描新增、更新和删除的权限数量。
     */
    PermissionScanResponse scanControllerPermissions();
}
