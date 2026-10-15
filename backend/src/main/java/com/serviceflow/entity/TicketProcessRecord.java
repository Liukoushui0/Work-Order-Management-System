package com.serviceflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 工单处理记录 ticket_process_record，用于追溯状态流转与处理内容。
 */
@Getter
@Setter
@TableName("ticket_process_record")
public class TicketProcessRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long ticketId;
    private Long operatorId;
    private String operatorName;
    private String operatorRole;
    private String action;
    private String fromStatus;
    private String toStatus;
    private String content;
    private BigDecimal usedHours;
    private LocalDateTime createTime;
}
