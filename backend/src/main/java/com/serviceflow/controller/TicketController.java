package com.serviceflow.controller;

import com.serviceflow.aspect.OperationLogRecord;
import com.serviceflow.common.api.PageResult;
import com.serviceflow.common.api.Result;
import com.serviceflow.dto.ticket.TicketAssignRequest;
import com.serviceflow.dto.ticket.TicketCancelRequest;
import com.serviceflow.dto.ticket.TicketCompleteRequest;
import com.serviceflow.dto.ticket.TicketConfirmRequest;
import com.serviceflow.dto.ticket.TicketCreateRequest;
import com.serviceflow.dto.ticket.TicketProcessRequest;
import com.serviceflow.dto.ticket.TicketQuery;
import com.serviceflow.dto.ticket.TicketRejectRequest;
import com.serviceflow.dto.ticket.TicketReopenRequest;
import com.serviceflow.dto.ticket.TicketTransferRequest;
import com.serviceflow.service.TicketService;
import com.serviceflow.vo.TicketDetailVO;
import com.serviceflow.vo.TicketRecordVO;
import com.serviceflow.vo.TicketVO;
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

import java.util.List;

/**
 * 售后工单接口：创建、查询、流转与处理。
 */
@Tag(name = "售后工单")
@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @Operation(summary = "工单分页列表",
            description = "支持状态、优先级、客户、负责人、故障分类、时间范围、逾期与关键词筛选；"
                    + "按角色自动收敛数据域（工程师仅本人负责、客户仅本人）")
    @PreAuthorize("hasAuthority('ticket:view')")
    @GetMapping
    public Result<PageResult<TicketVO>> page(@Valid TicketQuery query) {
        return Result.success(ticketService.page(query));
    }

    @Operation(summary = "逾期工单列表", description = "未结束且已超过 SLA 截止时间的工单")
    @PreAuthorize("hasAuthority('ticket:view')")
    @GetMapping("/overdue")
    public Result<PageResult<TicketVO>> overdue(@Valid TicketQuery query) {
        return Result.success(ticketService.overdue(query));
    }

    @Operation(summary = "工单详情", description = "含客户联系方式、设备信息与完整处理历史")
    @PreAuthorize("hasAuthority('ticket:view')")
    @GetMapping("/{id}")
    public Result<TicketDetailVO> detail(@PathVariable Long id) {
        return Result.success(ticketService.detail(id));
    }

    @Operation(summary = "工单处理历史")
    @PreAuthorize("hasAuthority('ticket:view')")
    @GetMapping("/{id}/records")
    public Result<List<TicketRecordVO>> records(@PathVariable Long id) {
        return Result.success(ticketService.records(id));
    }

    @Operation(summary = "创建工单", description = "自动生成唯一工单编号，并按优先级计算默认 SLA 截止时间")
    @PreAuthorize("hasAuthority('ticket:create')")
    @OperationLogRecord(module = "ticket", operation = "创建工单")
    @PostMapping
    public Result<TicketVO> create(@Valid @RequestBody TicketCreateRequest request) {
        return Result.success(ticketService.create(request));
    }

    @Operation(summary = "分派工单")
    @PreAuthorize("hasAuthority('ticket:assign')")
    @OperationLogRecord(module = "ticket", operation = "分派工单")
    @PostMapping("/{id}/assign")
    public Result<TicketVO> assign(@PathVariable Long id,
                                   @Valid @RequestBody TicketAssignRequest request) {
        return Result.success(ticketService.assign(id, request));
    }

    @Operation(summary = "接单", description = "并发保护：Redis 锁 + 状态条件更新，重复接单返回 1403")
    @PreAuthorize("hasAuthority('ticket:accept')")
    @OperationLogRecord(module = "ticket", operation = "接单")
    @PostMapping("/{id}/accept")
    public Result<TicketVO> accept(@PathVariable Long id) {
        return Result.success(ticketService.accept(id));
    }

    @Operation(summary = "转派工单")
    @PreAuthorize("hasAuthority('ticket:transfer')")
    @OperationLogRecord(module = "ticket", operation = "转派工单")
    @PostMapping("/{id}/transfer")
    public Result<TicketVO> transfer(@PathVariable Long id,
                                     @Valid @RequestBody TicketTransferRequest request) {
        return Result.success(ticketService.transfer(id, request));
    }

    @Operation(summary = "退回工单", description = "工程师退回后工单回到待分派")
    @PreAuthorize("hasAuthority('ticket:transfer')")
    @OperationLogRecord(module = "ticket", operation = "退回工单")
    @PostMapping("/{id}/reject")
    public Result<TicketVO> reject(@PathVariable Long id,
                                   @Valid @RequestBody TicketRejectRequest request) {
        return Result.success(ticketService.reject(id, request));
    }

    @Operation(summary = "填写处理记录")
    @PreAuthorize("hasAuthority('ticket:process')")
    @OperationLogRecord(module = "ticket", operation = "填写处理记录")
    @PostMapping("/{id}/process")
    public Result<TicketVO> process(@PathVariable Long id,
                                    @Valid @RequestBody TicketProcessRequest request) {
        return Result.success(ticketService.process(id, request));
    }

    @Operation(summary = "提交处理结果", description = "需先填写处理记录，提交后进入待确认")
    @PreAuthorize("hasAuthority('ticket:complete')")
    @OperationLogRecord(module = "ticket", operation = "提交处理结果")
    @PostMapping("/{id}/complete")
    public Result<TicketVO> complete(@PathVariable Long id,
                                     @Valid @RequestBody TicketCompleteRequest request) {
        return Result.success(ticketService.complete(id, request));
    }

    @Operation(summary = "客户确认服务结果")
    @PreAuthorize("hasAuthority('ticket:confirm')")
    @OperationLogRecord(module = "ticket", operation = "客户确认服务结果")
    @PostMapping("/{id}/confirm")
    public Result<TicketVO> confirm(@PathVariable Long id,
                                    @Valid @RequestBody TicketConfirmRequest request) {
        return Result.success(ticketService.confirm(id, request));
    }

    @Operation(summary = "退回重做或重新打开")
    @PreAuthorize("hasAuthority('ticket:reopen')")
    @OperationLogRecord(module = "ticket", operation = "退回重做或重新打开")
    @PostMapping("/{id}/reopen")
    public Result<TicketVO> reopen(@PathVariable Long id,
                                   @Valid @RequestBody TicketReopenRequest request) {
        return Result.success(ticketService.reopen(id, request));
    }

    @Operation(summary = "取消工单")
    @PreAuthorize("hasAuthority('ticket:cancel')")
    @OperationLogRecord(module = "ticket", operation = "取消工单")
    @PostMapping("/{id}/cancel")
    public Result<TicketVO> cancel(@PathVariable Long id,
                                   @Valid @RequestBody TicketCancelRequest request) {
        return Result.success(ticketService.cancel(id, request));
    }
}
