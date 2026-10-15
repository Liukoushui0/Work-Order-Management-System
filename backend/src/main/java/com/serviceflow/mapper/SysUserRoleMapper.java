package com.serviceflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.serviceflow.entity.SysUserRole;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {

    @Select("SELECT role_id FROM sys_user_role WHERE user_id = #{userId}")
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT DISTINCT ur.user_id FROM sys_user_role ur
            INNER JOIN sys_role r ON r.id = ur.role_id
            WHERE r.role_code = #{roleCode} AND r.deleted = 0
            """)
    List<Long> selectUserIdsByRoleCode(@Param("roleCode") String roleCode);

    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);

    @Select("SELECT COUNT(1) FROM sys_user_role ur INNER JOIN sys_role r ON r.id = ur.role_id "
            + "WHERE ur.role_id = #{roleId} AND r.deleted = 0")
    long countByRoleId(@Param("roleId") Long roleId);
}
