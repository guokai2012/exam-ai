package com.exam.ai.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.ai.common.exception.BusinessException;
import com.exam.ai.security.UserPrincipal;
import com.exam.ai.user.entity.SysMenu;
import com.exam.ai.user.vo.MenuResponse;
import com.exam.ai.user.dto.SaveMenuRequest;
import com.exam.ai.user.mapper.SysMenuMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;

/**
 * MenuService 接口，定义当前业务模块对外提供的服务契约。
 */
public interface MenuService {

    /**
     * 查询业务数据集合，并按调用场景组织返回结构。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public List<MenuResponse> tree();
    /**
     * 查询或解析业务数据，返回前端或内部流程需要的结果。
     * @param principal 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public List<MenuResponse> currentUserMenus(UserPrincipal principal);
    /**
     * 创建业务数据并完成必要的状态初始化。
     * @param request 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public MenuResponse create(SaveMenuRequest request);
    /**
     * 更新业务状态，并保持相关数据的一致性。
     * @param id 业务参数，参与当前方法的校验、查询或状态变更。
     * @param request 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public MenuResponse update(Long id, SaveMenuRequest request);
    /**
     * 删除或失效指定业务数据，并同步清理关联状态。
     * @param id 业务参数，参与当前方法的校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public void delete(Long id);
}
