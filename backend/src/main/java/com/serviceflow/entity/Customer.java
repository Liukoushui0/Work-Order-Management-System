package com.serviceflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 客户表 customer。
 */
@Getter
@Setter
@TableName("customer")
public class Customer {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String customerCode;
    private String customerName;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private String province;
    private String city;
    private String address;
    private String industry;

    /** 客户等级：S/A/B/C。 */
    private String level;

    /** 来源：PHONE/WEB/WECHAT/EMAIL。 */
    private String source;

    /** 状态：1 正常，0 停用。 */
    private Integer status;

    private String remark;
    private Long deleted;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
}
