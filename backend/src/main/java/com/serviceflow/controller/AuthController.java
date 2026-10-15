package com.serviceflow.controller;

import com.serviceflow.aspect.OperationLogRecord;
import com.serviceflow.common.api.Result;
import com.serviceflow.dto.auth.ChangePasswordRequest;
import com.serviceflow.dto.auth.LoginRequest;
import com.serviceflow.dto.auth.RefreshTokenRequest;
import com.serviceflow.dto.auth.UpdateProfileRequest;
import com.serviceflow.security.SecurityUtils;
import com.serviceflow.service.AuthService;
import com.serviceflow.vo.CurrentUserVO;
import com.serviceflow.vo.LoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证与个人中心接口。
 */
@Tag(name = "认证与个人中心")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "登录", description = "校验账号密码并签发 access token 与 refresh token")
    @OperationLogRecord(module = "auth", operation = "用户登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return Result.success(authService.login(request, httpRequest));
    }

    @Operation(summary = "退出登录", description = "把当前令牌加入黑名单")
    @OperationLogRecord(module = "auth", operation = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout(SecurityUtils.currentTokenDetails());
        return Result.success();
    }

    @Operation(summary = "刷新令牌", description = "使用 refresh token 换取新的访问令牌")
    @OperationLogRecord(module = "auth", operation = "刷新令牌", saveParams = false)
    @PostMapping("/refresh")
    public Result<LoginVO> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return Result.success(authService.refresh(request.getRefreshToken()));
    }

    @Operation(summary = "当前用户信息", description = "返回当前用户、角色与权限集合")
    @GetMapping("/me")
    public Result<CurrentUserVO> me() {
        return Result.success(authService.currentUser());
    }

    @Operation(summary = "修改密码")
    @OperationLogRecord(module = "auth", operation = "修改密码", saveParams = false)
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return Result.success();
    }

    @Operation(summary = "更新个人资料")
    @OperationLogRecord(module = "auth", operation = "更新个人资料")
    @PutMapping("/profile")
    public Result<CurrentUserVO> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return Result.success(authService.updateProfile(request));
    }
}
