package com.serviceflow.vo;

import com.serviceflow.entity.SysRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "角色信息")
public class RoleVO {

    private Long id;
    private String roleCode;
    private String roleName;
    private String description;
    private Integer sort;
    private Integer status;

    public static RoleVO from(SysRole role) {
        RoleVO vo = new RoleVO();
        vo.setId(role.getId());
        vo.setRoleCode(role.getRoleCode());
        vo.setRoleName(role.getRoleName());
        vo.setDescription(role.getDescription());
        vo.setSort(role.getSort());
        vo.setStatus(role.getStatus());
        return vo;
    }
}
