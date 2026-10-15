package com.serviceflow.dto.ticket;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Schema(description = "提交处理结果请求")
public class TicketCompleteRequest {

    @NotBlank(message = "处理结果说明不能为空")
    @Size(max = 2000, message = "处理结果说明长度不能超过 2000")
    private String result;

    @Schema(description = "本次工时（小时）")
    @DecimalMin(value = "0", message = "工时不能为负数")
    private BigDecimal usedHours;
}
