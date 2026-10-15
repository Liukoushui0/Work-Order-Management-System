package com.serviceflow.service;

import com.serviceflow.common.enums.ErrorCode;
import com.serviceflow.common.exception.BusinessException;
import com.serviceflow.dto.user.AssignRolesRequest;
import com.serviceflow.dto.user.UserCreateRequest;
import com.serviceflow.dto.user.UserQuery;
import com.serviceflow.entity.SysRole;
import com.serviceflow.entity.SysUser;
import com.serviceflow.entity.SysUserRole;
import com.serviceflow.mapper.SysRoleMapper;
import com.serviceflow.mapper.SysUserMapper;
import com.serviceflow.mapper.SysUserRoleMapper;
import com.serviceflow.security.AccessGuard;
import com.serviceflow.security.UserPrincipal;
import com.serviceflow.support.MybatisTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private SysRoleMapper sysRoleMapper;

    @Mock
    private SysUserRoleMapper sysUserRoleMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeAll
    static void initMybatisMetadata() {
        MybatisTestSupport.initTableInfo(SysUser.class, SysRole.class, SysUserRole.class);
    }

    @BeforeEach
    void setUp() {
        userService = new UserService(sysUserMapper, sysRoleMapper, sysUserRoleMapper,
                passwordEncoder, new AccessGuard());
        loginAsAdmin();
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void loginAsAdmin() {
        UserPrincipal principal = new UserPrincipal(1L, "admin", "hash", "系统管理员", null, true,
                Set.of(AccessGuard.ROLE_ADMIN), Set.of("user:manage"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private SysUser existingUser(Long id, String username, int status) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername(username);
        user.setPassword("$2a$10$hash");
        user.setRealName("既有用户");
        user.setStatus(status);
        user.setDeleted(0L);
        return user;
    }

    private SysRole role(Long id, String code) {
        SysRole role = new SysRole();
        role.setId(id);
        role.setRoleCode(code);
        role.setRoleName(code);
        role.setDeleted(0L);
        return role;
    }

    private static ErrorCode errorCodeOf(Throwable throwable) {
        return ((BusinessException) throwable).getErrorCode();
    }

    @Test
    @DisplayName("列表查询使用非法排序字段时抛出 1202，防止 SQL 注入")
    void shouldRejectIllegalSortField() {
        UserQuery query = new UserQuery();
        query.setSortField("create_time; drop table sys_user");

        assertThatThrownBy(() -> userService.page(query))
                .isInstanceOf(BusinessException.class)
                .extracting(UserServiceTest::errorCodeOf)
                .isEqualTo(ErrorCode.SORT_FIELD_INVALID);

        verify(sysUserMapper, never()).selectPage(any(), any());
    }

    @Test
    @DisplayName("新增用户时账号重复直接拒绝")
    void shouldRejectDuplicateUsername() {
        given(sysUserMapper.selectCount(any())).willReturn(1L);
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("service01");
        request.setPassword("Abcd1234");
        request.setRealName("重复账号");
        request.setRoleIds(List.of(2L));

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(BusinessException.class)
                .extracting(UserServiceTest::errorCodeOf)
                .isEqualTo(ErrorCode.PARAM_INVALID);

        verify(sysUserMapper, never()).insert(any(SysUser.class));
    }

    @Test
    @DisplayName("新增用户使用不存在的角色 ID 时拒绝")
    void shouldRejectUnknownRoleId() {
        given(sysUserMapper.selectCount(any())).willReturn(0L);
        given(sysRoleMapper.selectList(any())).willReturn(List.of(role(2L, "ROLE_SERVICE")));
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("newuser");
        request.setPassword("Abcd1234");
        request.setRealName("新用户");
        request.setRoleIds(List.of(2L, 99L));

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(BusinessException.class)
                .extracting(UserServiceTest::errorCodeOf)
                .isEqualTo(ErrorCode.PARAM_INVALID);
    }

    @Test
    @DisplayName("新增用户成功时写入 BCrypt 密码并建立角色关系")
    void shouldCreateUserWithHashedPassword() {
        given(sysUserMapper.selectCount(any())).willReturn(0L);
        given(sysRoleMapper.selectList(any())).willReturn(List.of(role(3L, "ROLE_ENGINEER")));
        given(passwordEncoder.encode("Abcd1234")).willReturn("$2a$10$encoded");
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("newuser");
        request.setPassword("Abcd1234");
        request.setRealName("新用户");
        request.setRoleIds(List.of(3L));

        userService.create(request);

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserMapper).insert(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("$2a$10$encoded");
        assertThat(captor.getValue().getDeleted()).isZero();
        verify(sysUserRoleMapper).insert(any(SysUserRole.class));
    }

    @Test
    @DisplayName("不能禁用当前登录账号")
    void shouldNotDisableSelf() {
        given(sysUserMapper.selectOne(any())).willReturn(existingUser(1L, "admin", 1));

        assertThatThrownBy(() -> userService.updateStatus(1L, 0))
                .isInstanceOf(BusinessException.class)
                .extracting(UserServiceTest::errorCodeOf)
                .isEqualTo(ErrorCode.PARAM_INVALID);

        verify(sysUserMapper, never()).update(isNull(), any());
    }

    @Test
    @DisplayName("不能把最后一个启用状态的管理员降权")
    void shouldNotRemoveLastAdminRole() {
        given(sysUserMapper.selectOne(any())).willReturn(existingUser(2L, "admin2", 1));
        given(sysRoleMapper.selectList(any())).willReturn(List.of(role(2L, "ROLE_SERVICE")));
        given(sysRoleMapper.selectByUserId(2L)).willReturn(List.of(role(1L, "ROLE_ADMIN")));
        given(sysUserMapper.countEnabledAdmins()).willReturn(1L);
        AssignRolesRequest request = new AssignRolesRequest();
        request.setRoleIds(List.of(2L));

        assertThatThrownBy(() -> userService.assignRoles(2L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(UserServiceTest::errorCodeOf)
                .isEqualTo(ErrorCode.PARAM_INVALID);
    }

    @Test
    @DisplayName("不能删除当前登录账号")
    void shouldNotDeleteSelf() {
        given(sysUserMapper.selectOne(any())).willReturn(existingUser(1L, "admin", 1));

        assertThatThrownBy(() -> userService.delete(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(UserServiceTest::errorCodeOf)
                .isEqualTo(ErrorCode.PARAM_INVALID);
    }
}
