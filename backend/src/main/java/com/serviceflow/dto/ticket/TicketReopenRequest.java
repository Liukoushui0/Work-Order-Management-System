package com.serviceflow.dto.ticket;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "退回重做或重新打开请求")
public class TicketReopenRequest {

    @NotBlank(message = "请说明退回重做或重新打开的原因")
    @Size(max = 500, message = "原因长度不能超过 500")
    private String reason;
}
