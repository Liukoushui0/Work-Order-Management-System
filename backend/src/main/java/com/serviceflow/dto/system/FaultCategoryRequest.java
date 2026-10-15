package com.serviceflow.dto.system;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "故障分类新增/修改请求")
public class FaultCategoryRequest {

    @NotBlank(message = "分类编码不能为空")
    @Pattern(regexp = "^[A-Z0-9\\-]{2,50}$", message = "分类编码只能包含大写字母、数字与短横线")
    private String categoryCode;

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 50, message = "分类名称长度不能超过 50")
    private String categoryName;

    @Schema(description = "父级 ID，0 表示顶级")
    private Long parentId = 0L;

    @Size(max = 255, message = "说明长度不能超过 255")
    private String description;

    private Integer sort = 0;

    @Schema(description = "状态：1 启用，0 禁用")
    private Integer status = 1;
}
