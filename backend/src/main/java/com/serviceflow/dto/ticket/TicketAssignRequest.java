package com.serviceflow.dto.ticket;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "分派工单请求")
public class TicketAssignRequest {

    @Schema(description = "被分派的工程师用户 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "必须指定负责人")
    private Long assigneeId;

    @Size(max = 255, message = "分派说明长度不能超过 255")
    private String remark;
}
