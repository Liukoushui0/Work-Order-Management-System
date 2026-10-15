package com.serviceflow.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.serviceflow.common.enums.ErrorCode;
import com.serviceflow.common.exception.BusinessException;
import com.serviceflow.common.util.RequestUtils;
import com.serviceflow.dto.auth.ChangePasswordRequest;
import com.serviceflow.dto.auth.LoginRequest;
import com.serviceflow.dto.auth.UpdateProfileRequest;
import com.serviceflow.entity.SysUser;
import com.serviceflow.mapper.SysUserMapper;
import com.serviceflow.security.CustomUserDetailsService;
import com.serviceflow.security.JwtTokenProvider;
import com.serviceflow.security.SecurityUtils;
import com.serviceflow.security.TokenBlacklistService;
import com.serviceflow.security.TokenDetails;
import com.serviceflow.security.UserPrincipal;
import com.serviceflow.vo.CurrentUserVO;
import com.serviceflow.vo.LoginVO;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 认证服务：登录、退出、刷新令牌、当前用户信息、修改密码与个人资料。
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final TokenBlacklistService blacklistService;
    private final CustomUserDetailsService userDetailsService;
    private final LoginAttemptService loginAttemptService;

    public AuthService(SysUserMapper sysUserMapper,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       TokenBlacklistService blacklistService,
                       CustomUserDetailsService userDetailsService,
                       LoginAttemptService loginAttemptService) {
        this.sysUserMapper = sysUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.blacklistService = blacklistService;
        this.userDetailsService = userDetailsService;
        this.loginAttemptService = loginAttemptService;
    }

    /**
     * 登录：校验账号状态、锁定状态与密码，成功后签发令牌并记录登录信息。
     */
    public LoginVO login(LoginRequest request, HttpServletRequest httpRequest) {
        SysUser user = sysUserMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, request.getUsername())
                .eq(SysUser::getDeleted, 0L));
        if (user == null) {
            // 统一提示，避免暴露账号是否存在
            throw BusinessException.of(ErrorCode.LOGIN_FAILED);
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw BusinessException.of(ErrorCode.ACCOUNT_DISABLED);
        }
        if (user.getLockUntil() != null && user.getLockUntil().isAfter(LocalDateTime.now())) {
            throw BusinessException.of(ErrorCode.ACCOUNT_LOCKED);
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            loginAttemptService.recordFailure(user.getId());
            throw BusinessException.of(ErrorCode.LOGIN_FAILED);
        }

        UserPrincipal principal = userDetailsService.loadByUserId(user.getId());
        if (!principal.isEnabled()) {
            throw BusinessException.of(ErrorCode.ACCOUNT_DISABLED);
        }
        String ip = RequestUtils.clientIp(httpRequest);
        loginAttemptService.recordSuccess(user.getId(), ip);
        log.info("用户登录成功 userId={} username={} ip={}", user.getId(), user.getUsername(), ip);
        return buildLoginVO(principal);
    }

    /**
     * 退出登录：把当前令牌加入黑名单。
     */
    public void logout(TokenDetails tokenDetails) {
        if (tokenDetails != null) {
            blacklistService.blacklist(tokenDetails.tokenId(), tokenDetails.expiresAt());
        }
    }

    /**
     * 使用 refresh token 换取新的访问令牌（旧 refresh token 立即失效）。
     */
    @Transactional(rollbackFor = Exception.class)
    public LoginVO refresh(String refreshToken) {
        Claims claims = tokenProvider.parse(refreshToken);
        if (claims == null || !tokenProvider.isRefreshToken(claims)) {
            throw BusinessException.of(ErrorCode.TOKEN_INVALID);
        }
        if (blacklistService.isBlacklisted(tokenProvider.getTokenId(claims))) {
            throw BusinessException.of(ErrorCode.TOKEN_INVALID);
        }
        Long userId = tokenProvider.getUserId(claims);
        UserPrincipal principal = userDetailsService.loadByUserId(userId);
        if (!principal.isEnabled()) {
            throw BusinessException.of(ErrorCode.ACCOUNT_DISABLED);
        }
        blacklistService.blacklist(tokenProvider.getTokenId(claims), tokenProvider.getExpiration(claims));
        return buildLoginVO(principal);
    }

    /**
     * 当前登录用户信息（含角色与权限，供前端做菜单与按钮控制）。
     */
    public CurrentUserVO currentUser() {
        UserPrincipal principal = SecurityUtils.currentUser();
        return toCurrentUserVO(principal);
    }

    /**
     * 修改密码：校验原密码，更新后当前令牌失效，需重新登录。
     */
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(ChangePasswordRequest request) {
        UserPrincipal principal = SecurityUtils.currentUser();
        SysUser user = sysUserMapper.selectById(principal.getUserId());
        if (user == null || (user.getDeleted() != null && user.getDeleted() != 0L)) {
            throw BusinessException.of(ErrorCode.TOKEN_INVALID);
        }
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID, "原密码不正确");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID, "新密码不能与原密码相同");
        }
        sysUserMapper.update(null, Wrappers.<SysUser>lambdaUpdate()
                .eq(SysUser::getId, user.getId())
                .set(SysUser::getPassword, passwordEncoder.encode(request.getNewPassword()))
                .set(SysUser::getUpdateBy, principal.getUserId()));
        TokenDetails details = SecurityUtils.currentTokenDetails();
        if (details != null) {
            blacklistService.blacklist(details.tokenId(), details.expiresAt());
        }
        log.info("用户修改密码成功 userId={}", user.getId());
    }

    /**
     * 更新个人资料（仅允许修改姓名、手机号、邮箱、头像）。
     */
    @Transactional(rollbackFor = Exception.class)
    public CurrentUserVO updateProfile(UpdateProfileRequest request) {
        UserPrincipal principal = SecurityUtils.currentUser();
        sysUserMapper.update(null, Wrappers.<SysUser>lambdaUpdate()
                .eq(SysUser::getId, principal.getUserId())
                .set(SysUser::getRealName, request.getRealName())
                .set(SysUser::getPhone, request.getPhone())
                .set(SysUser::getEmail, request.getEmail())
                .set(SysUser::getAvatar, request.getAvatar())
                .set(SysUser::getUpdateBy, principal.getUserId()));
        return toCurrentUserVO(userDetailsService.loadByUserId(principal.getUserId()));
    }

    private LoginVO buildLoginVO(UserPrincipal principal) {
        LoginVO vo = new LoginVO();
        vo.setAccessToken(tokenProvider.createAccessToken(principal));
        vo.setRefreshToken(tokenProvider.createRefreshToken(principal));
        vo.setExpiresIn(tokenProvider.getAccessTokenSeconds());
        vo.setUser(toCurrentUserVO(principal));
        return vo;
    }

    private CurrentUserVO toCurrentUserVO(UserPrincipal principal) {
        SysUser user = sysUserMapper.selectById(principal.getUserId());
        CurrentUserVO vo = new CurrentUserVO();
        vo.setId(principal.getUserId());
        vo.setUsername(principal.getUsername());
        vo.setRealName(user == null ? principal.getRealName() : user.getRealName());
        vo.setPhone(user == null ? null : user.getPhone());
        vo.setEmail(user == null ? null : user.getEmail());
        vo.setAvatar(user == null ? null : user.getAvatar());
        vo.setCustomerId(principal.getCustomerId());
        vo.setRoles(new ArrayList<>(principal.getRoles()));
        vo.setPermissions(new ArrayList<>(principal.getPermissions()));
        return vo;
    }
}
