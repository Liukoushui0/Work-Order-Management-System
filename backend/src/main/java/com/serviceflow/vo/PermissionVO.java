package com.serviceflow.vo;

import com.serviceflow.entity.SysPermission;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "权限信息")
public class PermissionVO {

    private Long id;
    private String permCode;
    private String permName;
    private String permType;
    private String module;
    private String description;
    private Integer sort;

    public static PermissionVO from(SysPermission permission) {
        PermissionVO vo = new PermissionVO();
        vo.setId(permission.getId());
        vo.setPermCode(permission.getPermCode());
        vo.setPermName(permission.getPermName());
        vo.setPermType(permission.getPermType());
        vo.setModule(permission.getModule());
        vo.setDescription(permission.getDescription());
        vo.setSort(permission.getSort());
        return vo;
    }
}
