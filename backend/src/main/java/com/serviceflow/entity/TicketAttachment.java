package com.serviceflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 工单附件 ticket_attachment，仅存元数据，文件本体保存在本地存储目录。
 */
@Getter
@Setter
@TableName("ticket_attachment")
public class TicketAttachment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long ticketId;
    private String fileName;
    private String storedName;
    private String filePath;
    private String fileExt;
    private Long fileSize;
    private String contentType;
    private String sha256;
    private String bizType;
    private Long uploaderId;
    private String uploaderType;
    private Integer status;
    private Long deleted;
    private LocalDateTime createTime;
}
