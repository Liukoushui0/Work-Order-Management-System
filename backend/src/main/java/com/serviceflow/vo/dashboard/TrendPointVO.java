package com.serviceflow.vo.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "趋势数据点")
public class TrendPointVO {

    @Schema(description = "时间标签，DAY=yyyy-MM-dd，WEEK=yyyy-'W'ww，MONTH=yyyy-MM")
    private String period;

    @Schema(description = "新建工单数")
    private Long created;

    @Schema(description = "完成工单数")
    private Long completed;

    public TrendPointVO() {
    }

    public TrendPointVO(String period, Long created, Long completed) {
        this.period = period;
        this.created = created;
        this.completed = completed;
    }
}
