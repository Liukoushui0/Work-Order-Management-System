package com.serviceflow.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 登录主体：携带用户 ID、角色与权限，供授权判断与数据域收敛使用。
 */
@Getter
public class UserPrincipal implements UserDetails {

    private final Long userId;
    private final String username;
    private final String password;
    private final String realName;
    private final Long customerId;
    private final boolean enabled;
    private final Set<String> roles;
    private final Set<String> permissions;
    private final Set<GrantedAuthority> authorities;

    public UserPrincipal(Long userId, String username, String password, String realName,
                         Long customerId, boolean enabled, Set<String> roles, Set<String> permissions) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.realName = realName;
        this.customerId = customerId;
        this.enabled = enabled;
        this.roles = roles == null ? Set.of() : Set.copyOf(roles);
        this.permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
        Set<GrantedAuthority> granted = new LinkedHashSet<>();
        this.roles.forEach(role -> granted.add(new SimpleGrantedAuthority(role)));
        this.permissions.forEach(perm -> granted.add(new SimpleGrantedAuthority(perm)));
        this.authorities = Set.copyOf(granted);
    }

    public boolean hasRole(String roleCode) {
        return roles.contains(roleCode);
    }

    public boolean hasPermission(String permCode) {
        return permissions.contains(permCode);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}
