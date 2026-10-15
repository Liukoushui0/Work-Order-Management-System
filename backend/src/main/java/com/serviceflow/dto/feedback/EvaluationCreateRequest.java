package com.serviceflow.dto.feedback;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "提交服务评价请求")
public class EvaluationCreateRequest {

    @Schema(description = "综合评分 1~5", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "综合评分不能为空")
    @Min(value = 1, message = "综合评分最低为 1")
    @Max(value = 5, message = "综合评分最高为 5")
    private Integer score;

    @Schema(description = "满意度：VERY_SATISFIED/SATISFIED/NEUTRAL/UNSATISFIED/VERY_UNSATISFIED")
    @Pattern(regexp = "^$|(VERY_SATISFIED|SATISFIED|NEUTRAL|UNSATISFIED|VERY_UNSATISFIED)$",
            message = "满意度取值不正确")
    private String satisfaction;

    @Min(value = 1, message = "响应速度评分最低为 1")
    @Max(value = 5, message = "响应速度评分最高为 5")
    private Integer speedScore;

    @Min(value = 1, message = "维修质量评分最低为 1")
    @Max(value = 5, message = "维修质量评分最高为 5")
    private Integer qualityScore;

    @Min(value = 1, message = "服务态度评分最低为 1")
    @Max(value = 5, message = "服务态度评分最高为 5")
    private Integer attitudeScore;

    @Schema(description = "评价标签，逗号分隔", example = "响应及时,一次修复")
    @Size(max = 255, message = "评价标签长度不能超过 255")
    private String tags;

    @Size(max = 1000, message = "评价内容长度不能超过 1000")
    private String content;

    @Schema(description = "是否匿名评价")
    private Boolean anonymous = false;
}
