package com.exam.ai.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.exam.ai.auth.entity.SysRefreshToken;
import com.exam.ai.auth.vo.CurrentUserResponse;
import com.exam.ai.auth.dto.ChangePasswordRequest;
import com.exam.ai.auth.dto.LoginRequest;
import com.exam.ai.auth.dto.RegisterRequest;
import com.exam.ai.auth.vo.TokenResponse;
import com.exam.ai.auth.mapper.SysRefreshTokenMapper;
import com.exam.ai.common.exception.BusinessException;
import com.exam.ai.common.config.SecurityProperties;
import com.exam.ai.security.JwtService;
import com.exam.ai.security.RedisKeys;
import com.exam.ai.security.TokenHashService;
import com.exam.ai.security.UserPrincipal;
import com.exam.ai.user.entity.SysUser;
import com.exam.ai.user.entity.UserStatus;
import com.exam.ai.user.mapper.SysUserMapper;
import com.exam.ai.user.service.RolePermissionService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

/**
 * AuthService 接口，定义当前业务模块对外提供的服务契约。
 */
public interface AuthService {

    /**
     * 创建业务数据并完成必要的状态初始化。
     * @param request 业务参数，参与当前方法的校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public void register(RegisterRequest request);
    /**
     * 创建业务数据并完成必要的状态初始化。
     * @param request 业务参数，参与当前方法的校验、查询或状态变更。
     * @param ip 业务参数，参与当前方法的校验、查询或状态变更。
     * @param userAgent 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public TokenResponse login(LoginRequest request, String ip, String userAgent);
    /**
     * 创建业务数据并完成必要的状态初始化。
     * @param refreshToken 业务参数，参与当前方法的校验、查询或状态变更。
     * @param ip 业务参数，参与当前方法的校验、查询或状态变更。
     * @param userAgent 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public TokenResponse refresh(String refreshToken, String ip, String userAgent);
    /**
     * 删除或失效指定业务数据，并同步清理关联状态。
     * @param principal 业务参数，参与当前方法的校验、查询或状态变更。
     * @param refreshToken 业务参数，参与当前方法的校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public void logout(UserPrincipal principal, String refreshToken);
    /**
     * 查询或解析业务数据，返回前端或内部流程需要的结果。
     * @param principal 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public CurrentUserResponse currentUser(UserPrincipal principal);
    /**
     * 更新业务状态，并保持相关数据的一致性。
     * @param principal 业务参数，参与当前方法的校验、查询或状态变更。
     * @param request 业务参数，参与当前方法的校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public void changePassword(UserPrincipal principal, ChangePasswordRequest request);
}
