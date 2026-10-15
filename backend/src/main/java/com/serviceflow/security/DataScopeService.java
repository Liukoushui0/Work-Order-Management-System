package com.serviceflow.security;

import org.springframework.stereotype.Service;

/**
 * 数据域服务：为列表查询提供"当前用户能看多远"的统一判断。
 * 工单、设备等模块的查询条件统一由本服务收敛，避免各模块各写一套判断。
 */
@Service
public class DataScopeService {

    /** 是否可查看全部数据（管理员、客服）。 */
    public boolean canViewAll() {
        UserPrincipal principal = SecurityUtils.currentUser();
        return principal.hasRole(AccessGuard.ROLE_ADMIN) || principal.hasRole(AccessGuard.ROLE_SERVICE);
    }

    /** 客户角色绑定的客户 ID；非客户角色返回 null。 */
    public Long scopedCustomerId() {
        UserPrincipal principal = SecurityUtils.currentUser();
        return principal.hasRole(AccessGuard.ROLE_CUSTOMER) ? principal.getCustomerId() : null;
    }

    /** 工程师只能查看分配给自己的数据，返回其用户 ID；其余角色返回 null。 */
    public Long scopedAssigneeId() {
        UserPrincipal principal = SecurityUtils.currentUser();
        return principal.hasRole(AccessGuard.ROLE_ENGINEER) ? principal.getUserId() : null;
    }

    public boolean isCustomer() {
        return SecurityUtils.currentUser().hasRole(AccessGuard.ROLE_CUSTOMER);
    }

    public boolean isEngineer() {
        return SecurityUtils.currentUser().hasRole(AccessGuard.ROLE_ENGINEER);
    }
}
