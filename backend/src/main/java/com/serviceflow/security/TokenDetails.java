package com.serviceflow.security;

import java.time.Instant;

/**
 * 挂在认证信息上的令牌上下文，供退出登录等操作读取 jti 与到期时间。
 */
public record TokenDetails(String tokenId, Instant expiresAt) {
}
