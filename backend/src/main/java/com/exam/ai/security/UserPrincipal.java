package com.exam.ai.security;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * UserPrincipal 不可变业务数据记录，用于接口入参、接口返回或服务间传输。
 * @param userId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
 * @param username 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
 * @param sessionId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
 * @param roles 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
 * @param permissions 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
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
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities();
    }

    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @return 封装后的业务处理结果。
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
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @Override
    public String getPassword() {
        return "";
    }

    /**
     * 查询或解析业务数据，返回前端或内部流程需要的结果。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @Override
    public String getUsername() {
        return username;
    }
}
