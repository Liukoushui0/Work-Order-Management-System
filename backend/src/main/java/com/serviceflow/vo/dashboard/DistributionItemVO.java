package com.serviceflow.vo.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "分布统计项")
public class DistributionItemVO {

    @Schema(description = "原始取值，如 PROCESSING 或故障分类 ID")
    private String key;

    @Schema(description = "展示名称")
    private String name;

    private Long value;

    @Schema(description = "占比（%，保留两位小数）")
    private Double percent;

    public DistributionItemVO() {
    }

    public DistributionItemVO(String key, String name, Long value, Double percent) {
        this.key = key;
        this.name = name;
        this.value = value;
        this.percent = percent;
    }
}
