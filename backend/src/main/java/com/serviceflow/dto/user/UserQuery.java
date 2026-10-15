package com.serviceflow.dto.user;

import com.serviceflow.common.api.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@Schema(description = "用户列表查询条件")
public class UserQuery extends PageQuery {

    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("id", "username", "realName", "createTime", "lastLoginTime");

    @Schema(description = "关键词：账号 / 姓名 / 手机号")
    private String keyword;

    @Schema(description = "状态：1 启用，0 禁用")
    private Integer status;

    @Schema(description = "角色编码，如 ROLE_ENGINEER")
    private String roleCode;

    @Override
    public Set<String> allowedSortFields() {
        return ALLOWED_SORT_FIELDS;
    }
}
