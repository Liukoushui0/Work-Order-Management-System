package com.serviceflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.serviceflow.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 用户表数据访问。逻辑删除约定：deleted = 0 表示有效。
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 统计处于启用状态的管理员数量，用于防止把最后一个管理员禁用或降权。
     */
    @Select("""
            SELECT COUNT(1) FROM sys_user u
            INNER JOIN sys_user_role ur ON ur.user_id = u.id
            INNER JOIN sys_role r ON r.id = ur.role_id
            WHERE r.role_code = 'ROLE_ADMIN' AND r.deleted = 0 AND u.status = 1 AND u.deleted = 0
            """)
    long countEnabledAdmins();
}
