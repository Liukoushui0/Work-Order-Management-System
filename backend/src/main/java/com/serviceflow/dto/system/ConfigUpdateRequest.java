package com.serviceflow.dto.system;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "系统配置修改请求")
public class ConfigUpdateRequest {

    @NotBlank(message = "配置值不能为空")
    @Size(max = 500, message = "配置值长度不能超过 500")
    private String configValue;
}
