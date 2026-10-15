package com.serviceflow.dto.ticket;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "退回工单请求")
public class TicketRejectRequest {

    @NotBlank(message = "退回原因不能为空")
    @Size(max = 500, message = "退回原因长度不能超过 500")
    private String reason;
}
