package com.serviceflow.controller;

import com.serviceflow.common.api.PageResult;
import com.serviceflow.common.api.Result;
import com.serviceflow.dto.system.OperationLogQuery;
import com.serviceflow.service.OperationLogService;
import com.serviceflow.vo.OperationLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 操作日志查询接口。
 */
@Tag(name = "操作日志")
@RestController
@RequestMapping("/api/v1/operation-logs")
public class OperationLogController {

    private final OperationLogService operationLogService;

    public OperationLogController(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @Operation(summary = "操作日志分页列表")
    @PreAuthorize("hasAuthority('log:view')")
    @GetMapping
    public Result<PageResult<OperationLogVO>> page(@Valid OperationLogQuery query) {
        return Result.success(operationLogService.page(query));
    }
}
