package com.serviceflow.dto.device;

import com.serviceflow.common.api.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@Schema(description = "设备列表查询条件")
public class DeviceQuery extends PageQuery {

    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("id", "deviceCode", "deviceName", "purchaseDate", "warrantyExpireDate", "createTime");

    @Schema(description = "关键词：设备编号 / 名称 / 型号 / 序列号")
    private String keyword;

    @Schema(description = "所属客户 ID")
    private Long customerId;

    @Schema(description = "状态：1 在用，0 停用，2 报废")
    private Integer status;

    @Override
    public Set<String> allowedSortFields() {
        return ALLOWED_SORT_FIELDS;
    }
}
