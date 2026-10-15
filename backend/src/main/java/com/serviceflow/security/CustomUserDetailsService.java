package com.serviceflow.security;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.serviceflow.entity.SysRole;
import com.serviceflow.entity.SysUser;
import com.serviceflow.mapper.SysPermissionMapper;
import com.serviceflow.mapper.SysRoleMapper;
import com.serviceflow.mapper.SysUserMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 从数据库加载用户及其角色、权限。每次请求都重新加载，保证角色/权限变更与禁用能立即生效。
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;

    public CustomUserDetailsService(SysUserMapper sysUserMapper,
                                    SysRoleMapper sysRoleMapper,
                                    SysPermissionMapper sysPermissionMapper) {
        this.sysUserMapper = sysUserMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysPermissionMapper = sysPermissionMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = sysUserMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, username)
                .eq(SysUser::getDeleted, 0L));
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在：" + username);
        }
        return buildPrincipal(user);
    }

    /**
     * 根据用户 ID 加载主体（用于令牌换发等场景）。
     */
    public UserPrincipal loadByUserId(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || (user.getDeleted() != null && user.getDeleted() != 0L)) {
            throw new UsernameNotFoundException("用户不存在：" + userId);
        }
        return buildPrincipal(user);
    }

    private UserPrincipal buildPrincipal(SysUser user) {
        List<SysRole> roles = sysRoleMapper.selectByUserId(user.getId());
        Set<String> roleCodes = new LinkedHashSet<>();
        roles.forEach(role -> roleCodes.add(role.getRoleCode()));
        Set<String> permCodes = new LinkedHashSet<>(sysPermissionMapper.selectPermCodesByUserId(user.getId()));
        boolean enabled = user.getStatus() != null && user.getStatus() == 1;
        return new UserPrincipal(user.getId(), user.getUsername(), user.getPassword(), user.getRealName(),
                user.getCustomerId(), enabled, roleCodes, permCodes);
    }
}
