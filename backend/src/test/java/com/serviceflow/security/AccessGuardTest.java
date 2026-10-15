package com.serviceflow.security;

import com.serviceflow.common.enums.ErrorCode;
import com.serviceflow.common.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccessGuardTest {

    private final AccessGuard accessGuard = new AccessGuard();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(String roleCode, Long customerId, Set<String> permissions) {
        UserPrincipal principal = new UserPrincipal(10L, "tester", "hash", "测试用户", customerId, true,
                Set.of(roleCode), permissions);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @Test
    @DisplayName("客户角色访问其他客户数据被拒绝，访问自身数据放行")
    void customerCanOnlyAccessOwnScope() {
        loginAs(AccessGuard.ROLE_CUSTOMER, 100L, Set.of("ticket:view"));

        assertThatCode(() -> accessGuard.assertCustomerScope(100L)).doesNotThrowAnyException();
        assertThatThrownBy(() -> accessGuard.assertCustomerScope(200L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.DATA_SCOPE_DENIED);
        assertThatThrownBy(() -> accessGuard.assertCustomerScope(null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("非客户角色的数据域校验不生效")
    void staffIsNotRestrictedByCustomerScope() {
        loginAs(AccessGuard.ROLE_SERVICE, null, Set.of("customer:view"));

        assertThatCode(() -> accessGuard.assertCustomerScope(999L)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("缺少权限时抛出 1101，管理员默认放行")
    void requirePermissionChecksPermissionOrAdminRole() {
        loginAs(AccessGuard.ROLE_CUSTOMER, 100L, Set.of("ticket:view"));

        assertThatThrownBy(() -> accessGuard.requirePermission("user:manage"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ACCESS_DENIED);

        loginAs(AccessGuard.ROLE_ADMIN, null, Set.of());

        assertThatCode(() -> accessGuard.requirePermission("user:manage")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("assertSelfOrPermission：本人放行，他人需要权限")
    void assertSelfOrPermissionBehaviour() {
        loginAs(AccessGuard.ROLE_ENGINEER, null, Set.of("ticket:view"));

        assertThatCode(() -> accessGuard.assertSelfOrPermission(10L, "user:view"))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> accessGuard.assertSelfOrPermission(99L, "user:view"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("未登录时读取主体抛出 1004")
    void shouldRejectAnonymous() {
        assertThatThrownBy(SecurityUtils::currentUserId)
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.TOKEN_INVALID);
    }
}
