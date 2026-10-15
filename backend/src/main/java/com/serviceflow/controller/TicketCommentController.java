package com.serviceflow.controller;

import com.serviceflow.aspect.OperationLogRecord;
import com.serviceflow.common.api.Result;
import com.serviceflow.dto.feedback.CommentCreateRequest;
import com.serviceflow.service.TicketCommentService;
import com.serviceflow.vo.CommentVO;
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
 * 工单评论接口（内部备注与客户沟通）。
 */
@Tag(name = "工单评论")
@RestController
@RequestMapping("/api/v1/tickets/{ticketId}/comments")
public class TicketCommentController {

    private final TicketCommentService commentService;

    public TicketCommentController(TicketCommentService commentService) {
        this.commentService = commentService;
    }

    @Operation(summary = "评论列表", description = "客户角色只能看到公开评论，内部备注仅员工可见")
    @PreAuthorize("hasAuthority('ticket:view')")
    @GetMapping
    public Result<List<CommentVO>> list(@PathVariable Long ticketId) {
        return Result.success(commentService.list(ticketId));
    }

    @Operation(summary = "新增评论", description = "员工可标记为内部备注；客户只能发布公开评论")
    @PreAuthorize("hasAuthority('ticket:view')")
    @OperationLogRecord(module = "ticket", operation = "新增工单评论")
    @PostMapping
    public Result<CommentVO> create(@PathVariable Long ticketId,
                                    @Valid @RequestBody CommentCreateRequest request) {
        return Result.success(commentService.create(ticketId, request));
    }
}
