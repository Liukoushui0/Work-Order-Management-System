package com.serviceflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 服务评价 ticket_evaluation，一个工单最多一条评价（唯一索引保障）。
 */
@Getter
@Setter
@TableName("ticket_evaluation")
public class TicketEvaluation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long ticketId;
    private Long customerId;
    private Long evaluatorId;
    private Integer score;
    private String satisfaction;
    private Integer speedScore;
    private Integer qualityScore;
    private Integer attitudeScore;
    private String tags;
    private String content;
    private Integer isAnonymous;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
