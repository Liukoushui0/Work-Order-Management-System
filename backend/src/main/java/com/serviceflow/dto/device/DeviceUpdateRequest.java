package com.serviceflow.dto.device;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Schema(description = "修改设备请求（设备编号不可修改）")
public class DeviceUpdateRequest {

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

    @Schema(description = "所属客户 ID（变更归属需校验目标客户有效性）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "所属客户不能为空")
    private Long customerId;

    private LocalDate purchaseDate;

    private LocalDate warrantyExpireDate;

    @Size(max = 255, message = "安装地址长度不能超过 255")
    private String installAddress;

    @Min(value = 0, message = "设备状态取值不正确")
    @Max(value = 2, message = "设备状态取值不正确")
    private Integer status;

    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;
}
