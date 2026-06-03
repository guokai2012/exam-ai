package com.exam.ai.user.service;

import com.exam.ai.user.vo.ApiPathOptionResponse;
import com.exam.ai.user.vo.MenuResponse;
import com.exam.ai.user.dto.SaveMenuRequest;
import com.exam.ai.user.dto.SyncMenuRequest;
import com.exam.ai.user.vo.MenuSyncResponse;
import java.util.List;

/**
 * MenuService 接口，定义当前业务模块对外提供的服务契约。
 */
public interface MenuService {

    /**
     * 查询业务数据集合，并按调用场景组织返回结构。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public List<MenuResponse> tree();
    /**
     * 查询或解析业务数据，返回前端或内部流程需要的结果。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public List<MenuResponse> currentUserMenus();

    /**
     * 创建菜单节点，新增时仍禁止维护前端组件标识。
     *
     * @param request 菜单创建请求；path 为空表示分组菜单，分组菜单不允许设置 apiPath 和权限码。
     * @return 创建后的菜单节点。
     * @throws com.exam.ai.common.exception.BusinessException 当父菜单不存在、path 重复或分组字段非法时抛出。
     */
    public MenuResponse create(SaveMenuRequest request);

    /**
     * 更新业务状态，并保持相关数据的一致性。
     * @param id 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param request 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public MenuResponse update(Long id, SaveMenuRequest request);

    /**
     * 扫描 Controller 根路径，返回菜单可绑定的页面主资源 API 路径选项。
     *
     * @return API 路径下拉选项，按路径稳定排序。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public List<ApiPathOptionResponse> listApiPathOptions();

    /**
     * 同步前端扫描得到的菜单树，以扫描结果全量覆盖数据库菜单数据。
     *
     * @param request 前端扫描得到的菜单树。
     * @return 本次同步新增、更新和删除数量。
     * @throws com.exam.ai.common.exception.BusinessException 当菜单结构非法或分组字段非法时抛出。
     */
    public MenuSyncResponse syncScannedMenus(SyncMenuRequest request);

    /**
     * 删除指定菜单；存在子菜单时拒绝删除，避免产生孤儿菜单。
     *
     * @param id 菜单 ID。
     * @throws com.exam.ai.common.exception.BusinessException 当菜单不存在或仍存在子菜单时抛出。
     */
    public void delete(Long id);
}
