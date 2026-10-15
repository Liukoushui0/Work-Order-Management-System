package com.serviceflow.dto.ticket;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "客户确认服务结果请求")
public class TicketConfirmRequest {

    @Schema(description = "确认备注")
    @Size(max = 500, message = "备注长度不能超过 500")
    private String remark;
}
