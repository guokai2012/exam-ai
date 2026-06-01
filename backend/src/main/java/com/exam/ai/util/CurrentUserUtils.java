package com.exam.ai.util;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.alibaba.ttl.TtlCallable;
import com.alibaba.ttl.TtlRunnable;
import com.exam.ai.common.exception.BusinessException;
import com.exam.ai.security.UserPrincipal;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 当前用户上下文工具类，统一提供登录用户、系统用户和子线程上下文传播能力。
 */
public final class CurrentUserUtils {

    private static final TransmittableThreadLocal<UserPrincipal> CURRENT_USER = new TransmittableThreadLocal<>();

    private CurrentUserUtils() {
    }

    /**
     * 获取当前用户上下文。
     * @return 当前用户上下文，未登录且未绑定系统用户时返回空。
     */
    public static Optional<UserPrincipal> getCurrentUser() {
        UserPrincipal current = CURRENT_USER.get();
        if (current != null) {
            return Optional.of(current);
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            CURRENT_USER.set(principal);
            return Optional.of(principal);
        }
        return Optional.empty();
    }

    /**
     * 获取当前用户上下文，未登录时抛出业务异常。
     * @return 当前用户上下文。
     * @throws BusinessException 当前线程没有登录用户或系统用户上下文时抛出。
     */
    public static UserPrincipal requireCurrentUser() {
        return getCurrentUser().orElseThrow(BusinessException::unauthorized);
    }

    /**
     * 获取当前用户 ID。
     * @return 当前用户 ID。
     * @throws BusinessException 当前线程没有登录用户或系统用户上下文时抛出。
     */
    public static Long currentUserId() {
        return requireCurrentUser().userId();
    }

    /**
     * 获取当前用户名。
     * @return 当前用户名。
     * @throws BusinessException 当前线程没有登录用户或系统用户上下文时抛出。
     */
    public static String currentUsername() {
        return requireCurrentUser().username();
    }

    /**
     * 判断当前上下文是否为虚拟系统用户。
     * @return 当前用户 ID 为系统预留负数 ID 时返回 true。
     */
    public static boolean isSystemUser() {
        return getCurrentUser().map(UserPrincipal::userId).filter(userId -> userId < 0).isPresent();
    }

    /**
     * 绑定指定用户执行无返回值任务，任务完成后恢复原上下文。
     * @param principal 本次执行使用的用户上下文。
     * @param runnable 业务任务。
     */
    public static void runAs(UserPrincipal principal, Runnable runnable) {
        runAs(principal, (Callable<Void>) () -> {
            runnable.run();
            return null;
        });
    }

    /**
     * 绑定指定用户执行有返回值任务，任务完成后恢复原上下文。
     * @param principal 本次执行使用的用户上下文。
     * @param callable 业务任务。
     * @return 业务任务返回结果。
     * @throws BusinessException 业务任务抛出受检异常时统一包装为业务异常。
     */
    public static <T> T runAs(UserPrincipal principal, Callable<T> callable) {
        UserPrincipal previous = CURRENT_USER.get();
        CURRENT_USER.set(principal);
        try {
            return callable.call();
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw BusinessException.badRequest(ex.getMessage() == null ? "当前用户上下文任务执行失败" : ex.getMessage());
        } finally {
            restore(previous);
        }
    }

    /**
     * 绑定指定用户执行函数式任务，任务完成后恢复原上下文。
     * @param principal 本次执行使用的用户上下文。
     * @param supplier 业务任务。
     * @return 业务任务返回结果。
     */
    public static <T> T runAs(UserPrincipal principal, Supplier<T> supplier) {
        return runAs(principal, (Callable<T>) supplier::get);
    }

    /**
     * 绑定指定系统模块用户执行无返回值任务。
     * @param module 系统用户模块。
     * @param runnable 业务任务。
     */
    public static void runAsSystem(SystemUserModule module, Runnable runnable) {
        runAs(module.toPrincipal(), runnable);
    }

    /**
     * 绑定指定系统模块用户执行有返回值任务。
     * @param module 系统用户模块。
     * @param callable 业务任务。
     * @return 业务任务返回结果。
     */
    public static <T> T runAsSystem(SystemUserModule module, Callable<T> callable) {
        return runAs(module.toPrincipal(), callable);
    }

    /**
     * 绑定指定系统模块用户执行函数式任务。
     * @param module 系统用户模块。
     * @param supplier 业务任务。
     * @return 业务任务返回结果。
     */
    public static <T> T runAsSystem(SystemUserModule module, Supplier<T> supplier) {
        return runAs(module.toPrincipal(), supplier);
    }

    /**
     * 包装 Runnable，使其提交到线程池后仍可读取提交时的当前用户上下文。
     * @param runnable 待包装任务。
     * @return 带 TTL 上下文的任务。
     */
    public static Runnable wrap(Runnable runnable) {
        return TtlRunnable.get(runnable);
    }

    /**
     * 包装 Callable，使其提交到线程池后仍可读取提交时的当前用户上下文。
     * @param callable 待包装任务。
     * @return 带 TTL 上下文的任务。
     */
    public static <T> Callable<T> wrap(Callable<T> callable) {
        return TtlCallable.get(callable);
    }

    /**
     * 包装 Supplier，使其提交到线程池后仍可读取提交时的当前用户上下文。
     * @param supplier 待包装任务。
     * @return 带 TTL 上下文的任务。
     */
    public static <T> Supplier<T> wrap(Supplier<T> supplier) {
        UserPrincipal captured = CURRENT_USER.get();
        return () -> runAs(captured, supplier);
    }

    /**
     * 直接绑定当前用户上下文。
     * @param principal 当前用户上下文。
     */
    public static void setCurrentUser(UserPrincipal principal) {
        CURRENT_USER.set(principal);
    }

    /**
     * 清理当前线程用户上下文。
     */
    public static void clear() {
        CURRENT_USER.remove();
    }

    private static void restore(UserPrincipal previous) {
        if (previous == null) {
            CURRENT_USER.remove();
        } else {
            CURRENT_USER.set(previous);
        }
    }
}
