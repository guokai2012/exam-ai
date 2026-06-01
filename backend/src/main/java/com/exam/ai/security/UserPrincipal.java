package com.exam.ai.security;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * UserPrincipal 记录对象，封装当前业务流程中的不可变数据。
 * @param userId 业务参数，参与当前方法的校验、查询或状态变更。
 * @param username 业务参数，参与当前方法的校验、查询或状态变更。
 * @param sessionId 业务参数，参与当前方法的校验、查询或状态变更。
 * @param roles 业务参数，参与当前方法的校验、查询或状态变更。
 * @param permissions 业务参数，参与当前方法的校验、查询或状态变更。
 */
public record UserPrincipal(
        Long userId,
        String username,
        String sessionId,
        List<String> roles,
        List<String> permissions
) implements UserDetails {

    /**
     * 查询或解析业务数据，返回前端或内部流程需要的结果。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities();
    }

    /**
     * 执行当前业务步骤，维护调用方需要的处理结果。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public List<GrantedAuthority> authorities() {
        List<GrantedAuthority> roleAuthorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .map(GrantedAuthority.class::cast)
                .toList();
        List<GrantedAuthority> permissionAuthorities = permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();
        return java.util.stream.Stream.concat(roleAuthorities.stream(), permissionAuthorities.stream()).toList();
    }

    /**
     * 查询或解析业务数据，返回前端或内部流程需要的结果。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @Override
    public String getPassword() {
        return "";
    }

    /**
     * 查询或解析业务数据，返回前端或内部流程需要的结果。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @Override
    public String getUsername() {
        return username;
    }
}
