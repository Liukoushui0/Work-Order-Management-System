package com.serviceflow.service;

import com.serviceflow.common.enums.ErrorCode;
import com.serviceflow.common.exception.BusinessException;
import com.serviceflow.entity.SysConfig;
import com.serviceflow.mapper.SysConfigMapper;
import com.serviceflow.vo.ConfigVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 系统配置读取与维护。
 *
 * <p>业务参数（登录锁定阈值、SLA 时长、附件限制等）统一存放在 sys_config，
 * 避免同时存在"配置文件 + 数据库"两套来源。Redis 缓存与失效在统计/缓存阶段接入。</p>
 */
@Service
public class ConfigService {

    private final SysConfigMapper sysConfigMapper;

    public ConfigService(SysConfigMapper sysConfigMapper) {
        this.sysConfigMapper = sysConfigMapper;
    }

    /**
     * 读取字符串配置，不存在时返回默认值。
     */
    public String getString(String configKey, String defaultValue) {
        SysConfig config = sysConfigMapper.selectByConfigKey(configKey);
        return config == null || config.getConfigValue() == null ? defaultValue : config.getConfigValue();
    }

    /**
     * 读取整型配置，解析失败时返回默认值。
     */
    public int getInt(String configKey, int defaultValue) {
        String value = getString(configKey, null);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    /**
     * 读取布尔配置。
     */
    public boolean getBoolean(String configKey, boolean defaultValue) {
        String value = getString(configKey, null);
        return value == null || value.isBlank() ? defaultValue : Boolean.parseBoolean(value.trim());
    }

    /**
     * 查询全部启用配置。
     */
    public List<ConfigVO> listAll() {
        return sysConfigMapper.selectEnabledConfigs().stream().map(ConfigVO::from).toList();
    }

    /**
     * 修改配置值。
     */
    @Transactional(rollbackFor = Exception.class)
    public ConfigVO update(String configKey, String configValue) {
        SysConfig config = sysConfigMapper.selectByConfigKey(configKey);
        if (config == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "配置项不存在：" + configKey);
        }
        config.setConfigValue(configValue);
        sysConfigMapper.updateById(config);
        return ConfigVO.from(config);
    }
}
