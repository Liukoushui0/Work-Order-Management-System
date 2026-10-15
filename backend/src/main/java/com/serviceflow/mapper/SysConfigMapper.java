package com.serviceflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.serviceflow.entity.SysConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysConfigMapper extends BaseMapper<SysConfig> {

    @Select("SELECT * FROM sys_config WHERE status = 1 ORDER BY config_group, id")
    List<SysConfig> selectEnabledConfigs();

    @Select("SELECT * FROM sys_config WHERE config_key = #{configKey} AND status = 1")
    SysConfig selectByConfigKey(@Param("configKey") String configKey);
}
