package com.serviceflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.serviceflow.entity.SysPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermission> {

    /**
     * 查询用户通过角色获得的启用权限编码。
     */
    @Select("""
            SELECT DISTINCT p.perm_code FROM sys_permission p
            INNER JOIN sys_role_permission rp ON rp.perm_id = p.id
            INNER JOIN sys_role r ON r.id = rp.role_id
            INNER JOIN sys_user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId} AND p.status = 1 AND r.status = 1 AND r.deleted = 0
            """)
    List<String> selectPermCodesByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT p.* FROM sys_permission p
            INNER JOIN sys_role_permission rp ON rp.perm_id = p.id
            WHERE rp.role_id = #{roleId} AND p.status = 1
            ORDER BY p.sort
            """)
    List<SysPermission> selectByRoleId(@Param("roleId") Long roleId);
}
