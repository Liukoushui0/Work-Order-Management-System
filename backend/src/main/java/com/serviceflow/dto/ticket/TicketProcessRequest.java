package com.serviceflow.dto.ticket;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Schema(description = "填写处理记录请求")
public class TicketProcessRequest {

    @NotBlank(message = "处理说明不能为空")
    @Size(max = 2000, message = "处理说明长度不能超过 2000")
    private String content;

    @Schema(description = "本次工时（小时）")
    @DecimalMin(value = "0", message = "工时不能为负数")
    @DecimalMax(value = "999", message = "工时数值过大")
    private BigDecimal usedHours;
}
