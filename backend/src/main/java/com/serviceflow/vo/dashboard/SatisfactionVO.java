package com.serviceflow.vo.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Schema(description = "客户满意度统计")
public class SatisfactionVO {

    @Schema(description = "评价总数")
    private Long total;

    @Schema(description = "平均评分")
    private BigDecimal avgScore;

    @Schema(description = "满意度分布（5 星 ~ 1 星与差评）")
    private List<DistributionItemVO> distribution;
}
