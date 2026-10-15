package com.serviceflow.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Schema(description = "工单详情")
public class TicketDetailVO {

    private TicketVO ticket;
    private String description;
    private String reporterName;
    private String reporterPhone;

    @Schema(description = "客户联系方式，便于工程师上门前联系")
    private String customerContactName;
    private String customerContactPhone;
    private String customerAddress;

    @Schema(description = "设备明细")
    private String deviceBrand;
    private String deviceModel;
    private String deviceSn;
    private LocalDate deviceWarrantyExpireDate;
    private Boolean deviceInWarranty;

    private LocalDateTime assignTime;
    private LocalDateTime startTime;
    private LocalDateTime confirmTime;
    private LocalDateTime closedTime;
    private LocalDateTime cancelTime;
    private String cancelReason;
    private Integer rejectCount;
    private Integer reopenCount;
    private Integer transferCount;

    @Schema(description = "处理历史（按时间正序）")
    private List<TicketRecordVO> records;
}
