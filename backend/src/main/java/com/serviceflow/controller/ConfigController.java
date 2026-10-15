package com.serviceflow.controller;

import com.serviceflow.aspect.OperationLogRecord;
import com.serviceflow.common.api.Result;
import com.serviceflow.dto.system.ConfigUpdateRequest;
import com.serviceflow.service.ConfigService;
import com.serviceflow.vo.ConfigVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 系统配置接口。
 */
@Tag(name = "系统配置")
@RestController
@RequestMapping("/api/v1/configs")
public class ConfigController {

    private final ConfigService configService;

    public ConfigController(ConfigService configService) {
        this.configService = configService;
    }

    @Operation(summary = "系统配置列表")
    @PreAuthorize("hasAuthority('config:view')")
    @GetMapping
    public Result<List<ConfigVO>> list() {
        return Result.success(configService.listAll());
    }

    @Operation(summary = "修改系统配置")
    @PreAuthorize("hasAuthority('config:manage')")
    @OperationLogRecord(module = "config", operation = "修改系统配置")
    @PutMapping("/{configKey}")
    public Result<ConfigVO> update(@PathVariable String configKey,
                                   @Valid @RequestBody ConfigUpdateRequest request) {
        return Result.success(configService.update(configKey, request.getConfigValue()));
    }
}
