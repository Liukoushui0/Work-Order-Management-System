package com.serviceflow.security;

import com.serviceflow.common.enums.ErrorCode;
import com.serviceflow.common.exception.BusinessException;
import org.springframework.stereotype.Component;

/**
 * 资源级访问校验：在 Service 层对"操作人与资源的关系"进行强制校验，
 * 防止通过修改 URL、ID 或请求参数越权访问他人数据。
 */
@Component
public class AccessGuard {

    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_SERVICE = "ROLE_SERVICE";
    public static final String ROLE_ENGINEER = "ROLE_ENGINEER";
    public static final String ROLE_CUSTOMER = "ROLE_CUSTOMER";

    /**
     * 要求当前用户拥有指定权限（管理员默认放行）。
     */
    public void requirePermission(String permCode) {
        UserPrincipal principal = SecurityUtils.currentUser();
        if (principal.hasRole(ROLE_ADMIN) || principal.hasPermission(permCode)) {
            return;
        }
        throw BusinessException.of(ErrorCode.ACCESS_DENIED);
    }

    /**
     * 要求当前用户具备指定角色之一。
     */
    public void requireAnyRole(String... roleCodes) {
        UserPrincipal principal = SecurityUtils.currentUser();
        for (String roleCode : roleCodes) {
            if (principal.hasRole(roleCode)) {
                return;
            }
        }
        throw BusinessException.of(ErrorCode.ACCESS_DENIED);
    }

    /**
     * 客户数据域校验：客户角色只能访问自己绑定的客户档案。
     *
     * @param customerId 资源所属客户 ID
     */
    public void assertCustomerScope(Long customerId) {
        UserPrincipal principal = SecurityUtils.currentUser();
        if (!principal.hasRole(ROLE_CUSTOMER)) {
            return;
        }
        if (customerId == null || !customerId.equals(principal.getCustomerId())) {
            throw BusinessException.of(ErrorCode.DATA_SCOPE_DENIED);
        }
    }

    /**
     * 允许访问自身数据，或由具备指定权限的人员访问。
     */
    public void assertSelfOrPermission(Long ownerUserId, String permCode) {
        UserPrincipal principal = SecurityUtils.currentUser();
        if (ownerUserId != null && ownerUserId.equals(principal.getUserId())) {
            return;
        }
        requirePermission(permCode);
    }
}
