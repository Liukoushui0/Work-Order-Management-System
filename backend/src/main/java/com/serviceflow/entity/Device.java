package com.serviceflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 客户设备档案表 device。
 */
@Getter
@Setter
@TableName("device")
public class Device {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String deviceCode;
    private String deviceName;
    private String deviceType;
    private String brand;
    private String model;
    private String snNo;
    private Long customerId;
    private LocalDate purchaseDate;
    private LocalDate warrantyExpireDate;
    private String installAddress;

    /** 状态：1 在用，0 停用，2 报废。 */
    private Integer status;

    private String remark;
    private Long deleted;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
}
