package com.serviceflow.dto.customer;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "新增客户请求")
public class CustomerCreateRequest {

    @NotBlank(message = "客户编号不能为空")
    @Pattern(regexp = "^[A-Z0-9\\-]{2,32}$", message = "客户编号只能包含大写字母、数字与短横线")
    private String customerCode;

    @NotBlank(message = "客户名称不能为空")
    @Size(max = 100, message = "客户名称长度不能超过 100")
    private String customerName;

    @NotBlank(message = "联系人不能为空")
    @Size(max = 50, message = "联系人长度不能超过 50")
    private String contactName;

    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "联系电话格式不正确")
    private String contactPhone;

    @Email(message = "联系邮箱格式不正确")
    @Size(max = 100, message = "联系邮箱长度不能超过 100")
    private String contactEmail;

    @Size(max = 50, message = "省份长度不能超过 50")
    private String province;

    @Size(max = 50, message = "城市长度不能超过 50")
    private String city;

    @Size(max = 255, message = "详细地址长度不能超过 255")
    private String address;

    @Size(max = 50, message = "所属行业长度不能超过 50")
    private String industry;

    @Schema(description = "客户等级：S/A/B/C", example = "A")
    @Pattern(regexp = "^[SABC]$", message = "客户等级只能是 S、A、B、C 之一")
    private String level = "B";

    @Schema(description = "来源：PHONE/WEB/WECHAT/EMAIL")
    @Pattern(regexp = "^$|(PHONE|WEB|WECHAT|EMAIL)$", message = "来源取值不正确")
    private String source;

    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;
}
