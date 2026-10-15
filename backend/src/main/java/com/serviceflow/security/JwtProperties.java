package com.serviceflow.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 相关配置。
 *
 * <p>密钥通过环境变量 JWT_SECRET 注入。若未配置，则启动时生成一次性随机密钥并打印告警——
 * 这样可以在本地开发时直接启动，同时避免把密钥写入仓库；但重启后旧令牌会失效，
 * 生产环境必须显式配置 JWT_SECRET。</p>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /** 签发者标识。 */
    private String issuer = "serviceflow";

    /** HMAC 密钥（至少 32 字节）。为空时启动生成随机密钥。 */
    private String secret;

    /** access token 有效期（分钟）。 */
    private long accessTokenMinutes = 120;

    /** refresh token 有效期（天）。 */
    private long refreshTokenDays = 7;
}
