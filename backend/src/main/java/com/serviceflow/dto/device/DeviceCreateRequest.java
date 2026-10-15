package com.serviceflow.dto.device;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Schema(description = "新建设备请求")
public class DeviceCreateRequest {

    @NotBlank(message = "设备编号不能为空")
    @Pattern(regexp = "^[A-Z0-9\\-]{2,32}$", message = "设备编号只能包含大写字母、数字与短横线")
    private String deviceCode;

    @NotBlank(message = "设备名称不能为空")
    @Size(max = 100, message = "设备名称长度不能超过 100")
    private String deviceName;

    @Size(max = 50, message = "设备类型长度不能超过 50")
    private String deviceType;

    @Size(max = 50, message = "品牌长度不能超过 50")
    private String brand;

    @Size(max = 50, message = "型号长度不能超过 50")
    private String model;

    @Size(max = 64, message = "序列号长度不能超过 64")
    private String snNo;

    @Schema(description = "所属客户 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "所属客户不能为空")
    private Long customerId;

    private LocalDate purchaseDate;

    private LocalDate warrantyExpireDate;

    @Size(max = 255, message = "安装地址长度不能超过 255")
    private String installAddress;

    @Schema(description = "状态：1 在用，0 停用，2 报废")
    @Min(value = 0, message = "设备状态取值不正确")
    @Max(value = 2, message = "设备状态取值不正确")
    private Integer status = 1;

    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;
}
