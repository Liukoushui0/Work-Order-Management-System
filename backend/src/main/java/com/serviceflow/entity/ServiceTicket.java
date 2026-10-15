package com.serviceflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 售后工单 service_ticket。
 */
@Getter
@Setter
@TableName("service_ticket")
public class ServiceTicket {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String ticketNo;
    private String title;
    private String description;
    private Long customerId;
    private Long deviceId;
    private Long faultCategoryId;
    private String priority;
    private String status;
    private String source;
    private String reporterName;
    private String reporterPhone;
    private Long assigneeId;
    private LocalDateTime deadlineTime;
    private LocalDateTime assignTime;
    private LocalDateTime acceptTime;
    private LocalDateTime startTime;
    private LocalDateTime finishTime;
    private LocalDateTime confirmTime;
    private LocalDateTime cancelTime;
    private LocalDateTime closedTime;
    private String cancelReason;
    private Integer rejectCount;
    private Integer reopenCount;
    private Integer transferCount;
    private BigDecimal handleHours;
    private Integer version;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
}
