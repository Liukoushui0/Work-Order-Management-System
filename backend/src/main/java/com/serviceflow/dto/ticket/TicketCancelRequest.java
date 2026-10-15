package com.serviceflow.dto.ticket;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "取消工单请求")
public class TicketCancelRequest {

    @NotBlank(message = "取消原因不能为空")
    @Size(max = 255, message = "取消原因长度不能超过 255")
    private String reason;
}
