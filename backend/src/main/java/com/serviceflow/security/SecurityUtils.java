package com.serviceflow.security;

import com.serviceflow.common.enums.ErrorCode;
import com.serviceflow.common.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 当前登录用户的读取工具。
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * 获取当前登录主体，未登录时抛出 1004。
     */
    public static UserPrincipal currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal;
        }
        throw BusinessException.of(ErrorCode.TOKEN_INVALID);
    }

    /**
     * 获取当前登录用户 ID，未登录时抛出 1004。
     */
    public static Long currentUserId() {
        return currentUser().getUserId();
    }

    /**
     * 当前请求的令牌上下文，可能为 null（例如测试中直接注入认证信息）。
     */
    public static TokenDetails currentTokenDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getDetails() instanceof TokenDetails details) {
            return details;
        }
        return null;
    }
}
