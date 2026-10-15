package com.serviceflow.vo;

import com.serviceflow.entity.TicketAttachment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Schema(description = "工单附件")
public class AttachmentVO {

    private Long id;
    private Long ticketId;
    private String fileName;
    private String fileExt;
    private Long fileSize;
    private String contentType;
    private String bizType;
    private Long uploaderId;
    private String uploaderType;
    private LocalDateTime createTime;

    @Schema(description = "下载地址")
    private String downloadUrl;

    public static AttachmentVO from(TicketAttachment attachment) {
        AttachmentVO vo = new AttachmentVO();
        vo.setId(attachment.getId());
        vo.setTicketId(attachment.getTicketId());
        vo.setFileName(attachment.getFileName());
        vo.setFileExt(attachment.getFileExt());
        vo.setFileSize(attachment.getFileSize());
        vo.setContentType(attachment.getContentType());
        vo.setBizType(attachment.getBizType());
        vo.setUploaderId(attachment.getUploaderId());
        vo.setUploaderType(attachment.getUploaderType());
        vo.setCreateTime(attachment.getCreateTime());
        vo.setDownloadUrl("/api/v1/tickets/" + attachment.getTicketId()
                + "/attachments/" + attachment.getId() + "/download");
        return vo;
    }
}
