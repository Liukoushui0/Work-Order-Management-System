package com.serviceflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.serviceflow.entity.SysRole;
import com.serviceflow.mapper.view.UserRoleView;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 查询用户拥有的启用角色（按角色排序）。
     */
    @Select("""
            SELECT r.* FROM sys_role r
            INNER JOIN sys_user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId} AND r.deleted = 0 AND r.status = 1
            ORDER BY r.sort
            """)
    List<SysRole> selectByUserId(@Param("userId") Long userId);

    /**
     * 批量查询多个用户的角色，用于列表页避免 N+1 查询。
     */
    @Select("""
            <script>
            SELECT ur.user_id AS userId, r.id, r.role_code, r.role_name, r.description, r.sort, r.status
            FROM sys_user_role ur
            INNER JOIN sys_role r ON r.id = ur.role_id
            WHERE r.deleted = 0 AND ur.user_id IN
            <foreach collection="userIds" item="userId" open="(" separator="," close=")">#{userId}</foreach>
            ORDER BY r.sort
            </script>
            """)
    List<UserRoleView> selectRolesByUserIds(@Param("userIds") List<Long> userIds);
}
