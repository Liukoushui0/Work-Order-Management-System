package com.serviceflow.vo;

import com.serviceflow.common.enums.TicketPriority;
import com.serviceflow.common.enums.TicketStatus;
import com.serviceflow.entity.ServiceTicket;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Schema(description = "工单列表项")
public class TicketVO {

    private Long id;
    private String ticketNo;
    private String title;
    private String status;
    private String statusLabel;
    private String priority;
    private String priorityLabel;
    private String source;
    private Long customerId;
    private String customerName;
    private Long deviceId;
    private String deviceName;
    private String deviceCode;
    private Long faultCategoryId;
    private String faultCategoryName;
    private Long assigneeId;
    private String assigneeName;
    private LocalDateTime deadlineTime;
    private LocalDateTime acceptTime;
    private LocalDateTime finishTime;
    private LocalDateTime createTime;
    private BigDecimal handleHours;

    @Schema(description = "退回次数")
    private Integer rejectCount;

    @Schema(description = "退回重做或重新打开次数")
    private Integer reopenCount;

    @Schema(description = "转派次数")
    private Integer transferCount;

    @Schema(description = "服务评价分数（未评价时为 null）")
    private Integer evaluationScore;

    @Schema(description = "满意度（未评价时为 null）")
    private String evaluationSatisfaction;

    @Schema(description = "是否已逾期（未结束且超过截止时间）")
    private Boolean overdue;

    public static TicketVO from(ServiceTicket ticket, String customerName, String deviceName,
                                String deviceCode, String faultCategoryName, String assigneeName) {
        TicketVO vo = new TicketVO();
        vo.setId(ticket.getId());
        vo.setTicketNo(ticket.getTicketNo());
        vo.setTitle(ticket.getTitle());
        vo.setStatus(ticket.getStatus());
        TicketStatus status = TicketStatus.of(ticket.getStatus());
        vo.setStatusLabel(status == null ? ticket.getStatus() : status.getLabel());
        vo.setPriority(ticket.getPriority());
        TicketPriority priority = TicketPriority.of(ticket.getPriority());
        vo.setPriorityLabel(priority == null ? ticket.getPriority() : priority.getLabel());
        vo.setSource(ticket.getSource());
        vo.setCustomerId(ticket.getCustomerId());
        vo.setCustomerName(customerName);
        vo.setDeviceId(ticket.getDeviceId());
        vo.setDeviceName(deviceName);
        vo.setDeviceCode(deviceCode);
        vo.setFaultCategoryId(ticket.getFaultCategoryId());
        vo.setFaultCategoryName(faultCategoryName);
        vo.setAssigneeId(ticket.getAssigneeId());
        vo.setAssigneeName(assigneeName);
        vo.setDeadlineTime(ticket.getDeadlineTime());
        vo.setAcceptTime(ticket.getAcceptTime());
        vo.setFinishTime(ticket.getFinishTime());
        vo.setCreateTime(ticket.getCreateTime());
        vo.setHandleHours(ticket.getHandleHours());
        vo.setRejectCount(ticket.getRejectCount());
        vo.setReopenCount(ticket.getReopenCount());
        vo.setTransferCount(ticket.getTransferCount());
        vo.setOverdue(status != null && status.isOpen() && ticket.getDeadlineTime() != null
                && ticket.getDeadlineTime().isBefore(LocalDateTime.now()));
        return vo;
    }
}
