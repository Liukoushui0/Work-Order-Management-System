package com.serviceflow.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.serviceflow.common.enums.ErrorCode;
import com.serviceflow.common.exception.BusinessException;
import com.serviceflow.entity.ServiceTicket;
import com.serviceflow.entity.TicketAttachment;
import com.serviceflow.mapper.TicketAttachmentMapper;
import com.serviceflow.security.AccessGuard;
import com.serviceflow.security.SecurityUtils;
import com.serviceflow.security.UserPrincipal;
import com.serviceflow.vo.AttachmentVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

/**
 * 工单附件：上传、列表、下载与删除。
 *
 * 所有操作都先校验"当前用户是否可访问该工单"，再执行文件操作；
 * 下载时文件路径再次做规范化校验，杜绝路径穿越。
 */
@Service
public class TicketAttachmentService {

    private static final Logger log = LoggerFactory.getLogger(TicketAttachmentService.class);
    private static final List<String> BIZ_TYPES = List.of("FAULT_PHOTO", "REPAIR_PHOTO", "REPORT", "OTHER");

    private final TicketAttachmentMapper attachmentMapper;
    private final TicketService ticketService;
    private final FileStorageService fileStorageService;
    private final AccessGuard accessGuard;

    public TicketAttachmentService(TicketAttachmentMapper attachmentMapper,
                                   TicketService ticketService,
                                   FileStorageService fileStorageService,
                                   AccessGuard accessGuard) {
        this.attachmentMapper = attachmentMapper;
        this.ticketService = ticketService;
        this.fileStorageService = fileStorageService;
        this.accessGuard = accessGuard;
    }

    @Transactional(readOnly = true)
    public List<AttachmentVO> list(Long ticketId) {
        ticketService.requireVisibleTicket(ticketId);
        accessGuard.requirePermission("attachment:download");
        return attachmentMapper.selectList(Wrappers.<TicketAttachment>lambdaQuery()
                        .eq(TicketAttachment::getTicketId, ticketId)
                        .eq(TicketAttachment::getDeleted, 0L)
                        .orderByDesc(TicketAttachment::getCreateTime))
                .stream().map(AttachmentVO::from).toList();
    }

    /**
     * 上传附件：先落盘，再写元数据；写库失败则删除已落盘文件，避免产生孤儿文件。
     */
    @Transactional(rollbackFor = Exception.class)
    public AttachmentVO upload(Long ticketId, MultipartFile file, String bizType) {
        ticketService.requireVisibleTicket(ticketId);
        accessGuard.requirePermission("attachment:upload");
        String normalizedBizType = StringUtils.hasText(bizType) && BIZ_TYPES.contains(bizType)
                ? bizType : "OTHER";

        FileStorageService.StoredFile stored = fileStorageService.store(file, ticketId);
        UserPrincipal principal = SecurityUtils.currentUser();
        boolean staff = principal.hasRole(AccessGuard.ROLE_ADMIN)
                || principal.hasRole(AccessGuard.ROLE_SERVICE)
                || principal.hasRole(AccessGuard.ROLE_ENGINEER);

        TicketAttachment attachment = new TicketAttachment();
        attachment.setTicketId(ticketId);
        attachment.setFileName(stored.originalName());
        attachment.setStoredName(stored.storedName());
        attachment.setFilePath(stored.relativePath());
        attachment.setFileExt(stored.extension());
        attachment.setFileSize(stored.size());
        attachment.setContentType(stored.contentType());
        attachment.setSha256(stored.sha256());
        attachment.setBizType(normalizedBizType);
        attachment.setUploaderId(principal.getUserId());
        attachment.setUploaderType(staff ? "STAFF" : "CUSTOMER");
        attachment.setStatus(1);
        attachment.setDeleted(0L);
        try {
            attachmentMapper.insert(attachment);
        } catch (RuntimeException ex) {
            fileStorageService.delete(stored.relativePath());
            throw ex;
        }
        return AttachmentVO.from(attachment);
    }

    /**
     * 下载附件：先校验工单访问权限，再校验路径安全。
     */
    @Transactional(readOnly = true)
    public DownloadFile download(Long ticketId, Long attachmentId) {
        ServiceTicket ticket = ticketService.requireVisibleTicket(ticketId);
        accessGuard.requirePermission("attachment:download");
        TicketAttachment attachment = requireAttachment(ticketId, attachmentId);
        Path path = fileStorageService.resolveForRead(attachment.getFilePath());
        Resource resource = new FileSystemResource(path);
        log.info("下载附件 attachmentId={} ticketId={} user={}", attachmentId, ticket.getId(),
                SecurityUtils.currentUserId());
        return new DownloadFile(resource, attachment.getFileName(), attachment.getContentType(),
                attachment.getFileSize());
    }

    /**
     * 删除附件（逻辑删除 + 删除物理文件）；员工仅能删除自己上传的附件。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long ticketId, Long attachmentId) {
        ticketService.requireVisibleTicket(ticketId);
        accessGuard.requirePermission("attachment:delete");
        TicketAttachment attachment = requireAttachment(ticketId, attachmentId);
        UserPrincipal principal = SecurityUtils.currentUser();
        boolean manager = principal.hasRole(AccessGuard.ROLE_ADMIN) || principal.hasRole(AccessGuard.ROLE_SERVICE);
        if (!manager && !attachment.getUploaderId().equals(principal.getUserId())) {
            throw BusinessException.of(ErrorCode.ACCESS_DENIED, "只能删除自己上传的附件");
        }
        attachmentMapper.update(null, Wrappers.<TicketAttachment>lambdaUpdate()
                .eq(TicketAttachment::getId, attachment.getId())
                .set(TicketAttachment::getStatus, 0)
                .set(TicketAttachment::getDeleted, attachment.getId()));
        fileStorageService.delete(attachment.getFilePath());
    }

    private TicketAttachment requireAttachment(Long ticketId, Long attachmentId) {
        TicketAttachment attachment = attachmentMapper.selectOne(Wrappers.<TicketAttachment>lambdaQuery()
                .eq(TicketAttachment::getId, attachmentId)
                .eq(TicketAttachment::getTicketId, ticketId)
                .eq(TicketAttachment::getDeleted, 0L));
        if (attachment == null) {
            throw BusinessException.of(ErrorCode.ATTACHMENT_NOT_FOUND);
        }
        return attachment;
    }

    /**
     * 下载结果。
     */
    public record DownloadFile(Resource resource, String fileName, String contentType, Long size) {
    }
}
