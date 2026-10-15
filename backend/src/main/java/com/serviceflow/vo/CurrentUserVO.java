package com.serviceflow.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Schema(description = "当前登录用户信息")
public class CurrentUserVO {

    private Long id;
    private String username;
    private String realName;
    private String phone;
    private String email;
    private String avatar;
    private Long customerId;
    private List<String> roles;
    private List<String> permissions;
}
