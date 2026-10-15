package com.serviceflow.vo;

import com.serviceflow.entity.Customer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Schema(description = "客户信息")
public class CustomerVO {

    private Long id;
    private String customerCode;
    private String customerName;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private String province;
    private String city;
    private String address;
    private String industry;
    private String level;
    private String source;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;

    @Schema(description = "名下设备数量")
    private Long deviceCount;

    @Schema(description = "关联工单总数")
    private Long ticketCount;

    public static CustomerVO from(Customer customer, Long deviceCount, Long ticketCount) {
        CustomerVO vo = new CustomerVO();
        vo.setId(customer.getId());
        vo.setCustomerCode(customer.getCustomerCode());
        vo.setCustomerName(customer.getCustomerName());
        vo.setContactName(customer.getContactName());
        vo.setContactPhone(customer.getContactPhone());
        vo.setContactEmail(customer.getContactEmail());
        vo.setProvince(customer.getProvince());
        vo.setCity(customer.getCity());
        vo.setAddress(customer.getAddress());
        vo.setIndustry(customer.getIndustry());
        vo.setLevel(customer.getLevel());
        vo.setSource(customer.getSource());
        vo.setStatus(customer.getStatus());
        vo.setRemark(customer.getRemark());
        vo.setCreateTime(customer.getCreateTime());
        vo.setDeviceCount(deviceCount == null ? 0L : deviceCount);
        vo.setTicketCount(ticketCount == null ? 0L : ticketCount);
        return vo;
    }
}
