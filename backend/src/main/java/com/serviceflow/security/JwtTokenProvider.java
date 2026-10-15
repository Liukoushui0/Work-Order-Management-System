package com.serviceflow.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 签发与解析。
 *
 * <p>令牌类型：access（短期，用于接口鉴权）与 refresh（长期，仅用于换取新令牌）。</p>
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);
    private static final String CLAIM_USER_ID = "uid";
    private static final String CLAIM_TOKEN_TYPE = "typ";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final JwtProperties properties;
    private SecretKey secretKey;

    /** access token 有效期（秒）。 */
    @Getter
    private long accessTokenSeconds;

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void init() {
        String secret = properties.getSecret();
        if (!StringUtils.hasText(secret)) {
            byte[] random = new byte[32];
            new SecureRandom().nextBytes(random);
            secret = Base64.getEncoder().encodeToString(random);
            log.warn("app.jwt.secret 未配置，已生成一次性随机密钥；重启后已签发的令牌将全部失效。"
                    + "生产环境请通过环境变量 JWT_SECRET 配置固定密钥。");
        } else if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("app.jwt.secret 长度不足：HMAC-SHA256 要求至少 32 字节");
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenSeconds = properties.getAccessTokenMinutes() * 60;
    }

    /**
     * 签发 access token。
     */
    public String createAccessToken(UserPrincipal principal) {
        return buildToken(principal.getUsername(), principal.getUserId(), TYPE_ACCESS,
                Instant.now().plusSeconds(properties.getAccessTokenMinutes() * 60));
    }

    /**
     * 签发 refresh token。
     */
    public String createRefreshToken(UserPrincipal principal) {
        return buildToken(principal.getUsername(), principal.getUserId(), TYPE_REFRESH,
                Instant.now().plusSeconds(properties.getRefreshTokenDays() * 24 * 3600));
    }

    private String buildToken(String username, Long userId, String type, Instant expiresAt) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(properties.getIssuer())
                .subject(username)
                .id(UUID.randomUUID().toString().replace("-", ""))
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_TOKEN_TYPE, type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 解析并校验令牌，失败时返回 null（调用方按未认证处理）。
     */
    public Claims parse(String token) {
        try {
            return Jwts.parser().verifyWith(secretKey).build()
                    .parseSignedClaims(token).getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("JWT 解析失败：{}", ex.getMessage());
            return null;
        }
    }

    public boolean isAccessToken(Claims claims) {
        return claims != null && TYPE_ACCESS.equals(claims.get(CLAIM_TOKEN_TYPE, String.class));
    }

    public boolean isRefreshToken(Claims claims) {
        return claims != null && TYPE_REFRESH.equals(claims.get(CLAIM_TOKEN_TYPE, String.class));
    }

    public String getUsername(Claims claims) {
        return claims.getSubject();
    }

    public Long getUserId(Claims claims) {
        Number uid = claims.get(CLAIM_USER_ID, Number.class);
        return uid == null ? null : uid.longValue();
    }

    public String getTokenId(Claims claims) {
        return claims.getId();
    }

    public Instant getExpiration(Claims claims) {
        Date expiration = claims.getExpiration();
        return expiration == null ? null : expiration.toInstant();
    }
}
