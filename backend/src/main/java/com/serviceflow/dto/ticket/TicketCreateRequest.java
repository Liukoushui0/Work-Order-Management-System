package com.serviceflow.dto.ticket;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Schema(description = "创建工单请求")
public class TicketCreateRequest {

    @NotBlank(message = "工单标题不能为空")
    @Size(max = 200, message = "工单标题长度不能超过 200")
    private String title;

    @NotBlank(message = "故障描述不能为空")
    @Size(max = 2000, message = "故障描述长度不能超过 2000")
    private String description;

    @Schema(description = "客户 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "客户不能为空")
    private Long customerId;

    @Schema(description = "设备 ID（必须属于所选客户）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "设备不能为空")
    private Long deviceId;

    @Schema(description = "故障分类 ID")
    private Long faultCategoryId;

    @Schema(description = "优先级：LOW/MEDIUM/HIGH/URGENT", example = "HIGH")
    @Pattern(regexp = "^(LOW|MEDIUM|HIGH|URGENT)$", message = "优先级取值不正确")
    private String priority = "MEDIUM";

    @Schema(description = "来源：PHONE/WEB/WECHAT/EMAIL")
    @Pattern(regexp = "^(PHONE|WEB|WECHAT|EMAIL)$", message = "来源取值不正确")
    private String source = "WEB";

    @Size(max = 50, message = "报修人长度不能超过 50")
    private String reporterName;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "报修人电话格式不正确")
    private String reporterPhone;

    @Schema(description = "SLA 截止时间，留空时按优先级默认 SLA 计算")
    private LocalDateTime deadlineTime;
}
