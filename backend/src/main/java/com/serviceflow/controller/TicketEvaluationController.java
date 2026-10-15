package com.serviceflow.controller;

import com.serviceflow.aspect.OperationLogRecord;
import com.serviceflow.common.api.Result;
import com.serviceflow.dto.feedback.EvaluationCreateRequest;
import com.serviceflow.service.TicketEvaluationService;
import com.serviceflow.vo.EvaluationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 服务评价接口。
 */
@Tag(name = "服务评价")
@RestController
@RequestMapping("/api/v1/tickets/{ticketId}/evaluation")
public class TicketEvaluationController {

    private final TicketEvaluationService evaluationService;

    public TicketEvaluationController(TicketEvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @Operation(summary = "提交服务评价",
            description = "仅工单所属客户可评价；工单必须已完成且未评价，否则返回 1501/1502")
    @PreAuthorize("hasAuthority('ticket:evaluate')")
    @OperationLogRecord(module = "evaluation", operation = "提交服务评价")
    @PostMapping
    public Result<EvaluationVO> create(@PathVariable Long ticketId,
                                       @Valid @RequestBody EvaluationCreateRequest request) {
        return Result.success(evaluationService.create(ticketId, request));
    }

    @Operation(summary = "查询工单评价", description = "未评价时 data 为 null；匿名评价对非评价人隐藏内容")
    @PreAuthorize("hasAuthority('ticket:view')")
    @GetMapping
    public Result<EvaluationVO> detail(@PathVariable Long ticketId) {
        return Result.success(evaluationService.detail(ticketId));
    }
}
