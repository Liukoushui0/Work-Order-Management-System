package com.serviceflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 工单评论 ticket_comment：区分内部备注（仅员工可见）与客户可见沟通。
 */
@Getter
@Setter
@TableName("ticket_comment")
public class TicketComment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long ticketId;
    private Long userId;
    private String userType;
    private String content;
    private Integer isInternal;
    private Long deleted;
    private LocalDateTime createTime;
}
