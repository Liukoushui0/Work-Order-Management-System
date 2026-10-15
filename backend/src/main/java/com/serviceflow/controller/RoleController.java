package com.serviceflow.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.serviceflow.common.api.Result;
import com.serviceflow.entity.SysPermission;
import com.serviceflow.entity.SysRole;
import com.serviceflow.mapper.SysPermissionMapper;
import com.serviceflow.mapper.SysRoleMapper;
import com.serviceflow.vo.PermissionVO;
import com.serviceflow.vo.RoleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 角色与权限查询接口（角色为系统内置，不支持增删，保证权限模型稳定）。
 */
@Tag(name = "角色与权限")
@RestController
@RequestMapping("/api/v1")
public class RoleController {

    private final SysRoleMapper sysRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;

    public RoleController(SysRoleMapper sysRoleMapper, SysPermissionMapper sysPermissionMapper) {
        this.sysRoleMapper = sysRoleMapper;
        this.sysPermissionMapper = sysPermissionMapper;
    }

    @Operation(summary = "角色列表")
    @PreAuthorize("hasAnyAuthority('role:view', 'user:view')")
    @GetMapping("/roles")
    public Result<List<RoleVO>> roles() {
        return Result.success(sysRoleMapper.selectList(Wrappers.<SysRole>lambdaQuery()
                        .eq(SysRole::getDeleted, 0L)
                        .orderByAsc(SysRole::getSort))
                .stream().map(RoleVO::from).toList());
    }

    @Operation(summary = "权限列表")
    @PreAuthorize("hasAuthority('role:view')")
    @GetMapping("/permissions")
    public Result<List<PermissionVO>> permissions() {
        return Result.success(sysPermissionMapper.selectList(Wrappers.<SysPermission>lambdaQuery()
                        .eq(SysPermission::getStatus, 1)
                        .orderByAsc(SysPermission::getSort))
                .stream().map(PermissionVO::from).toList());
    }
}
