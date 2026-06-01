package com.exam.ai.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.exam.ai.common.exception.BusinessException;
import com.exam.ai.user.entity.SysMenu;
import com.exam.ai.user.entity.SysPermission;
import com.exam.ai.user.entity.SysRolePermission;
import com.exam.ai.user.vo.PermissionResponse;
import com.exam.ai.user.vo.PermissionScanResponse;
import com.exam.ai.user.dto.SavePermissionRequest;
import com.exam.ai.user.mapper.SysMenuMapper;
import com.exam.ai.user.mapper.SysPermissionMapper;
import com.exam.ai.user.mapper.SysRolePermissionMapper;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * AdminPermissionService 接口，定义当前业务模块对外提供的服务契约。
 */
public interface AdminPermissionService {

    String TYPE_GROUP = "GROUP";
    String TYPE_MENU = "MENU";
    String TYPE_VIEW = "VIEW";
    String TYPE_ACTION = "ACTION";

    /**
     * 查询业务数据集合，并按调用场景组织返回结构。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public List<PermissionResponse> list();
    /**
     * 创建业务数据并完成必要的默认状态初始化。
     * @param request 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public PermissionResponse create(SavePermissionRequest request);
    /**
     * 更新业务状态，并保持相关数据的一致性。
     * @param id 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param request 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public PermissionResponse update(Long id, SavePermissionRequest request);
    /**
     * 删除或失效指定业务数据，并同步清理关联状态。
     * @param id 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public void delete(Long id);
    /**
     * 更新业务状态，并保持相关数据的一致性。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public PermissionScanResponse scanControllerPermissions();
    /**
     * 更新业务状态，并保持相关数据的一致性。
     * @param menu 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public void syncMenuPermission(SysMenu menu);
    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param menu 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public String generatedViewCode(SysMenu menu);
}
