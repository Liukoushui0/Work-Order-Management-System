package com.serviceflow.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Schema(description = "新增用户请求")
public class UserCreateRequest {

    @NotBlank(message = "登录账号不能为空")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]{3,49}$",
            message = "登录账号需以字母开头，由字母、数字、下划线组成，长度 4~50")
    private String username;

    @NotBlank(message = "初始密码不能为空")
    @Size(min = 8, max = 64, message = "初始密码长度需在 8~64 之间")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "初始密码需同时包含字母和数字")
    private String password;

    @NotBlank(message = "真实姓名不能为空")
    @Size(max = 50, message = "真实姓名长度不能超过 50")
    private String realName;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过 100")
    private String email;

    @Schema(description = "客户角色账号绑定的客户档案 ID")
    private Long customerId;

    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;

    @NotEmpty(message = "至少需要分配一个角色")
    private List<Long> roleIds;
}
