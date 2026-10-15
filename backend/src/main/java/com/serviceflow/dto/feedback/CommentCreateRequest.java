package com.serviceflow.dto.feedback;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "新增工单评论请求")
public class CommentCreateRequest {

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 1000, message = "评论内容长度不能超过 1000")
    private String content;

    @Schema(description = "是否内部备注（仅员工可见），客户角色强制为公开")
    private Boolean internal = false;
}
