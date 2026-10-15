package com.serviceflow.controller;

import com.serviceflow.aspect.OperationLogRecord;
import com.serviceflow.common.api.Result;
import com.serviceflow.service.TicketAttachmentService;
import com.serviceflow.vo.AttachmentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 工单附件接口：上传、列表、下载与删除，全部按工单访问权限校验。
 */
@Tag(name = "工单附件")
@RestController
@RequestMapping("/api/v1/tickets/{ticketId}/attachments")
public class TicketAttachmentController {

    private final TicketAttachmentService attachmentService;

    public TicketAttachmentController(TicketAttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @Operation(summary = "附件列表")
    @PreAuthorize("hasAuthority('attachment:download')")
    @GetMapping
    public Result<List<AttachmentVO>> list(@PathVariable Long ticketId) {
        return Result.success(attachmentService.list(ticketId));
    }

    @Operation(summary = "上传附件",
            description = "扩展名与大小取自系统配置；文件名会被清洗，存储名为 UUID，禁止路径穿越")
    @PreAuthorize("hasAuthority('attachment:upload')")
    @OperationLogRecord(module = "attachment", operation = "上传附件", saveParams = false)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<AttachmentVO> upload(@PathVariable Long ticketId,
                                       @RequestParam("file") MultipartFile file,
                                       @RequestParam(value = "bizType", required = false) String bizType) {
        return Result.success(attachmentService.upload(ticketId, file, bizType));
    }

    @Operation(summary = "下载附件", description = "先校验工单访问权限，再以流式响应返回，不暴露物理路径")
    @PreAuthorize("hasAuthority('attachment:download')")
    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<Resource> download(@PathVariable Long ticketId, @PathVariable Long attachmentId) {
        TicketAttachmentService.DownloadFile file = attachmentService.download(ticketId, attachmentId);
        String encodedName = URLEncoder.encode(file.fileName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(resolveMediaType(file.contentType()))
                .contentLength(file.size() == null ? 0 : file.size())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + encodedName + "\"; filename*=UTF-8''" + encodedName)
                .body(file.resource());
    }

    /**
     * 客户端上报的 Content-Type 不可信，解析失败时退回二进制流。
     */
    private MediaType resolveMediaType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (Exception ex) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    @Operation(summary = "删除附件", description = "逻辑删除并删除物理文件；员工仅能删除自己上传的附件")
    @PreAuthorize("hasAuthority('attachment:delete')")
    @OperationLogRecord(module = "attachment", operation = "删除附件")
    @DeleteMapping("/{attachmentId}")
    public Result<Void> delete(@PathVariable Long ticketId, @PathVariable Long attachmentId) {
        attachmentService.delete(ticketId, attachmentId);
        return Result.success();
    }
}
