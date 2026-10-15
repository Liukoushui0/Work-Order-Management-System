package com.serviceflow.controller;

import com.serviceflow.aspect.OperationLogRecord;
import com.serviceflow.common.api.PageResult;
import com.serviceflow.common.api.Result;
import com.serviceflow.dto.user.AssignRolesRequest;
import com.serviceflow.dto.user.ResetPasswordRequest;
import com.serviceflow.dto.user.StatusUpdateRequest;
import com.serviceflow.dto.user.UserCreateRequest;
import com.serviceflow.dto.user.UserQuery;
import com.serviceflow.dto.user.UserUpdateRequest;
import com.serviceflow.service.UserService;
import com.serviceflow.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户管理接口（仅管理员）。
 */
@Tag(name = "用户管理")
@Validated
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "用户分页列表")
    @PreAuthorize("hasAuthority('user:view')")
    @GetMapping
    public Result<PageResult<UserVO>> page(@Valid UserQuery query) {
        return Result.success(userService.page(query));
    }

    @Operation(summary = "用户详情")
    @PreAuthorize("hasAuthority('user:view')")
    @GetMapping("/{id}")
    public Result<UserVO> detail(@PathVariable Long id) {
        return Result.success(userService.detail(id));
    }

    @Operation(summary = "新增用户")
    @PreAuthorize("hasAuthority('user:manage')")
    @OperationLogRecord(module = "user", operation = "新增用户", saveParams = false)
    @PostMapping
    public Result<UserVO> create(@Valid @RequestBody UserCreateRequest request) {
        return Result.success(userService.create(request));
    }

    @Operation(summary = "修改用户")
    @PreAuthorize("hasAuthority('user:manage')")
    @OperationLogRecord(module = "user", operation = "修改用户")
    @PutMapping("/{id}")
    public Result<UserVO> update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return Result.success(userService.update(id, request));
    }

    @Operation(summary = "启用或禁用用户")
    @PreAuthorize("hasAuthority('user:manage')")
    @OperationLogRecord(module = "user", operation = "启用或禁用用户")
    @PutMapping("/{id}/status")
    public Result<UserVO> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
        return Result.success(userService.updateStatus(id, request.getStatus()));
    }

    @Operation(summary = "分配角色")
    @PreAuthorize("hasAuthority('user:manage')")
    @OperationLogRecord(module = "user", operation = "分配角色")
    @PutMapping("/{id}/roles")
    public Result<UserVO> assignRoles(@PathVariable Long id, @Valid @RequestBody AssignRolesRequest request) {
        return Result.success(userService.assignRoles(id, request));
    }

    @Operation(summary = "重置用户密码")
    @PreAuthorize("hasAuthority('user:manage')")
    @OperationLogRecord(module = "user", operation = "重置用户密码", saveParams = false)
    @PostMapping("/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(id, request.getNewPassword());
        return Result.success();
    }

    @Operation(summary = "删除用户（逻辑删除）")
    @PreAuthorize("hasAuthority('user:manage')")
    @OperationLogRecord(module = "user", operation = "删除用户")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.success();
    }
}
