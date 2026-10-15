package com.serviceflow.mapper.view;

import lombok.Getter;
import lombok.Setter;

/**
 * 用户角色联查结果（批量查询，避免 N+1）。
 */
@Getter
@Setter
public class UserRoleView {

    private Long userId;
    private Long id;
    private String roleCode;
    private String roleName;
    private String description;
    private Integer sort;
    private Integer status;
}
