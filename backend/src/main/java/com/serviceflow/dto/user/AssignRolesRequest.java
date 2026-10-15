package com.serviceflow.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Schema(description = "分配角色请求")
public class AssignRolesRequest {

    @Schema(description = "角色 ID 列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "至少需要分配一个角色")
    private List<Long> roleIds;
}
