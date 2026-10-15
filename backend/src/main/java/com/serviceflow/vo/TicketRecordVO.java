package com.serviceflow.vo;

import com.serviceflow.common.enums.TicketAction;
import com.serviceflow.entity.TicketProcessRecord;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Schema(description = "工单处理记录")
public class TicketRecordVO {

    private Long id;
    private String action;
    private String actionLabel;
    private String operatorName;
    private String operatorRole;
    private String fromStatus;
    private String toStatus;
    private String content;
    private BigDecimal usedHours;
    private LocalDateTime createTime;

    public static TicketRecordVO from(TicketProcessRecord record) {
        TicketRecordVO vo = new TicketRecordVO();
        vo.setId(record.getId());
        vo.setAction(record.getAction());
        TicketAction action = parseAction(record.getAction());
        vo.setActionLabel(action == null ? record.getAction() : action.getLabel());
        vo.setOperatorName(record.getOperatorName());
        vo.setOperatorRole(record.getOperatorRole());
        vo.setFromStatus(record.getFromStatus());
        vo.setToStatus(record.getToStatus());
        vo.setContent(record.getContent());
        vo.setUsedHours(record.getUsedHours());
        vo.setCreateTime(record.getCreateTime());
        return vo;
    }

    private static TicketAction parseAction(String value) {
        for (TicketAction action : TicketAction.values()) {
            if (action.name().equalsIgnoreCase(value)) {
                return action;
            }
        }
        return null;
    }
}
