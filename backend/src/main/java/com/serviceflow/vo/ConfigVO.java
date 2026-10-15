package com.serviceflow.vo;

import com.serviceflow.entity.SysConfig;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Schema(description = "系统配置项")
public class ConfigVO {

    private Long id;
    private String configKey;
    private String configValue;
    private String configName;
    private String configGroup;
    private String valueType;
    private String remark;
    private LocalDateTime updateTime;

    public static ConfigVO from(SysConfig config) {
        ConfigVO vo = new ConfigVO();
        vo.setId(config.getId());
        vo.setConfigKey(config.getConfigKey());
        vo.setConfigValue(config.getConfigValue());
        vo.setConfigName(config.getConfigName());
        vo.setConfigGroup(config.getConfigGroup());
        vo.setValueType(config.getValueType());
        vo.setRemark(config.getRemark());
        vo.setUpdateTime(config.getUpdateTime());
        return vo;
    }
}
