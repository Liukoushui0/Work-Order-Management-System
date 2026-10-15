package com.serviceflow.vo;

import com.serviceflow.entity.Device;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Schema(description = "设备档案信息")
public class DeviceVO {

    private Long id;
    private String deviceCode;
    private String deviceName;
    private String deviceType;
    private String brand;
    private String model;
    private String snNo;
    private Long customerId;
    private String customerName;
    private LocalDate purchaseDate;
    private LocalDate warrantyExpireDate;
    private String installAddress;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;

    @Schema(description = "关联工单数量")
    private Long ticketCount;

    @Schema(description = "是否在保修期内")
    private Boolean inWarranty;

    public static DeviceVO from(Device device, String customerName, Long ticketCount) {
        DeviceVO vo = new DeviceVO();
        vo.setId(device.getId());
        vo.setDeviceCode(device.getDeviceCode());
        vo.setDeviceName(device.getDeviceName());
        vo.setDeviceType(device.getDeviceType());
        vo.setBrand(device.getBrand());
        vo.setModel(device.getModel());
        vo.setSnNo(device.getSnNo());
        vo.setCustomerId(device.getCustomerId());
        vo.setCustomerName(customerName);
        vo.setPurchaseDate(device.getPurchaseDate());
        vo.setWarrantyExpireDate(device.getWarrantyExpireDate());
        vo.setInstallAddress(device.getInstallAddress());
        vo.setStatus(device.getStatus());
        vo.setRemark(device.getRemark());
        vo.setCreateTime(device.getCreateTime());
        vo.setTicketCount(ticketCount == null ? 0L : ticketCount);
        vo.setInWarranty(device.getWarrantyExpireDate() == null
                || !device.getWarrantyExpireDate().isBefore(LocalDate.now()));
        return vo;
    }
}
