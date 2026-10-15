package com.serviceflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 故障分类表 fault_category。
 */
@Getter
@Setter
@TableName("fault_category")
public class FaultCategory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String categoryCode;
    private String categoryName;
    private Long parentId;
    private String description;
    private Integer sort;
    private Integer status;
    private Long deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
