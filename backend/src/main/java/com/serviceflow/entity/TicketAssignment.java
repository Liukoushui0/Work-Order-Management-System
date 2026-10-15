package com.serviceflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 工单分派记录 ticket_assignment。
 *
 * <p>active_flag = 1 表示当前有效分派；历史记录写入自身 id，配合唯一索引
 * (ticket_id, active_flag) 保证一个工单同时只有一条有效分派记录。</p>
 */
@Getter
@Setter
@TableName("ticket_assignment")
public class TicketAssignment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long ticketId;
    private Long fromUserId;
    private Long toUserId;
    private String assignType;
    private String reason;
    private Long activeFlag;
    private Long createBy;
    private LocalDateTime createTime;
}
