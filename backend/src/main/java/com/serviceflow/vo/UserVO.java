package com.serviceflow.vo;

import com.serviceflow.entity.SysUser;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Schema(description = "用户信息（不含密码）")
public class UserVO {

    private Long id;
    private String username;
    private String realName;
    private String phone;
    private String email;
    private String avatar;
    private Long customerId;
    private Integer status;
    private String remark;
    private LocalDateTime lastLoginTime;
    private LocalDateTime createTime;
    private List<RoleVO> roles;

    public static UserVO from(SysUser user, List<RoleVO> roles) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setAvatar(user.getAvatar());
        vo.setCustomerId(user.getCustomerId());
        vo.setStatus(user.getStatus());
        vo.setRemark(user.getRemark());
        vo.setLastLoginTime(user.getLastLoginTime());
        vo.setCreateTime(user.getCreateTime());
        vo.setRoles(roles == null ? List.of() : roles);
        return vo;
    }
}
