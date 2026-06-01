package com.exam.ai.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.exam.ai.common.exception.BusinessException;
import com.exam.ai.security.UserPrincipal;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class CurrentUserUtilsTest {

    @AfterEach
    void tearDown() {
        CurrentUserUtils.clear();
    }

    @Test
    void shouldRejectWhenCurrentUserMissing() {
        assertThatThrownBy(CurrentUserUtils::requireCurrentUser)
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldRestorePreviousUserAfterRunAs() {
        UserPrincipal admin = principal(1L, "admin");
        UserPrincipal teacher = principal(2L, "teacher");

        CurrentUserUtils.runAs(admin, () -> {
            assertThat(CurrentUserUtils.currentUserId()).isEqualTo(1L);
            CurrentUserUtils.runAs(teacher, (Runnable) () -> assertThat(CurrentUserUtils.currentUserId()).isEqualTo(2L));
            assertThat(CurrentUserUtils.currentUserId()).isEqualTo(1L);
        });

        assertThat(CurrentUserUtils.getCurrentUser()).isEmpty();
    }

    @Test
    void shouldCreateSystemUserContext() {
        CurrentUserUtils.runAsSystem(SystemUserModule.QUESTION, () -> {
            assertThat(CurrentUserUtils.currentUserId()).isEqualTo(-900004L);
            assertThat(CurrentUserUtils.currentUsername()).isEqualTo("system_question");
            assertThat(CurrentUserUtils.isSystemUser()).isTrue();
        });
    }

    @Test
    void shouldReadCurrentUserInChildThread() throws Exception {
        AtomicReference<Long> userId = new AtomicReference<>();
        UserPrincipal principal = principal(3L, "child-user");

        CurrentUserUtils.runAs(principal, (Callable<Void>) () -> {
            Thread thread = new Thread(() -> userId.set(CurrentUserUtils.currentUserId()));
            thread.start();
            thread.join();
            return null;
        });

        assertThat(userId.get()).isEqualTo(3L);
    }

    @Test
    void shouldReadCapturedCurrentUserFromWrappedCallable() throws Exception {
        UserPrincipal principal = principal(4L, "wrapped-user");
        Callable<Long> wrapped = CurrentUserUtils.runAs(principal,
                (Callable<Callable<Long>>) () -> CurrentUserUtils.wrap((Callable<Long>) CurrentUserUtils::currentUserId));

        CurrentUserUtils.clear();

        assertThat(wrapped.call()).isEqualTo(4L);
    }

    private UserPrincipal principal(Long userId, String username) {
        return new UserPrincipal(userId, username, "session", List.of("TEST"), List.of("test:read"));
    }
}
