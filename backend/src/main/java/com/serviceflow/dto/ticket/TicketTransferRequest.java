package com.serviceflow.dto.ticket;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "转派工单请求")
public class TicketTransferRequest {

    @Schema(description = "转派目标工程师用户 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "必须指定转派对象")
    private Long assigneeId;

    @NotBlank(message = "转派原因不能为空")
    @Size(max = 255, message = "转派原因长度不能超过 255")
    private String reason;
}
