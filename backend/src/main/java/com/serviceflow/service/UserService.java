package com.serviceflow.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.serviceflow.common.api.PageResult;
import com.serviceflow.common.enums.ErrorCode;
import com.serviceflow.common.exception.BusinessException;
import com.serviceflow.dto.user.AssignRolesRequest;
import com.serviceflow.dto.user.UserCreateRequest;
import com.serviceflow.dto.user.UserQuery;
import com.serviceflow.dto.user.UserUpdateRequest;
import com.serviceflow.entity.SysRole;
import com.serviceflow.entity.SysUser;
import com.serviceflow.entity.SysUserRole;
import com.serviceflow.mapper.SysRoleMapper;
import com.serviceflow.mapper.SysUserMapper;
import com.serviceflow.mapper.SysUserRoleMapper;
import com.serviceflow.mapper.view.UserRoleView;
import com.serviceflow.security.AccessGuard;
import com.serviceflow.security.SecurityUtils;
import com.serviceflow.vo.RoleVO;
import com.serviceflow.vo.UserVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户管理：查询、新增、修改、启用禁用、分配角色、重置密码。
 */
@Service
public class UserService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final AccessGuard accessGuard;

    public UserService(SysUserMapper sysUserMapper,
                       SysRoleMapper sysRoleMapper,
                       SysUserRoleMapper sysUserRoleMapper,
                       PasswordEncoder passwordEncoder,
                       AccessGuard accessGuard) {
        this.sysUserMapper = sysUserMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.passwordEncoder = passwordEncoder;
        this.accessGuard = accessGuard;
    }

    @Transactional(readOnly = true)
    public PageResult<UserVO> page(UserQuery query) {
        List<Long> roleFilteredUserIds = null;
        if (StringUtils.hasText(query.getRoleCode())) {
            roleFilteredUserIds = sysUserRoleMapper.selectUserIdsByRoleCode(query.getRoleCode());
            if (roleFilteredUserIds.isEmpty()) {
                return PageResult.empty(query.getPageNum(), query.getPageSize());
            }
        }
        if (!query.isSortFieldAllowed()) {
            throw BusinessException.of(ErrorCode.SORT_FIELD_INVALID);
        }
        Page<SysUser> page = query.toPage("createTime");
        Page<SysUser> result = sysUserMapper.selectPage(page,
                Wrappers.<SysUser>lambdaQuery()
                        .eq(SysUser::getDeleted, 0L)
                        .eq(query.getStatus() != null, SysUser::getStatus, query.getStatus())
                        .in(roleFilteredUserIds != null, SysUser::getId, roleFilteredUserIds)
                        .and(StringUtils.hasText(query.getKeyword()), wrapper -> wrapper
                                .like(SysUser::getUsername, query.getKeyword())
                                .or().like(SysUser::getRealName, query.getKeyword())
                                .or().like(SysUser::getPhone, query.getKeyword())));
        Map<Long, List<RoleVO>> roleMap = loadRoles(result.getRecords());
        return PageResult.of(result, user -> UserVO.from(user, roleMap.get(user.getId())));
    }

    @Transactional(readOnly = true)
    public UserVO detail(Long id) {
        SysUser user = requireUser(id);
        return UserVO.from(user, rolesOf(id));
    }

    @Transactional(rollbackFor = Exception.class)
    public UserVO create(UserCreateRequest request) {
        Long duplicate = sysUserMapper.selectCount(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, request.getUsername())
                .eq(SysUser::getDeleted, 0L));
        if (duplicate != null && duplicate > 0) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID, "登录账号已存在：" + request.getUsername());
        }
        List<SysRole> roles = requireRoles(request.getRoleIds());
        SysUser entity = new SysUser();
        entity.setUsername(request.getUsername());
        entity.setPassword(passwordEncoder.encode(request.getPassword()));
        entity.setRealName(request.getRealName());
        entity.setPhone(request.getPhone());
        entity.setEmail(request.getEmail());
        entity.setCustomerId(request.getCustomerId());
        entity.setRemark(request.getRemark());
        entity.setStatus(1);
        entity.setLoginFailCount(0);
        entity.setVersion(0);
        entity.setDeleted(0L);
        entity.setCreateBy(SecurityUtils.currentUserId());
        sysUserMapper.insert(entity);
        roles.forEach(role -> insertUserRole(entity.getId(), role.getId()));
        return UserVO.from(entity, roles.stream().map(RoleVO::from).toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public UserVO update(Long id, UserUpdateRequest request) {
        SysUser entity = requireUser(id);
        entity.setRealName(request.getRealName());
        entity.setPhone(request.getPhone());
        entity.setEmail(request.getEmail());
        entity.setCustomerId(request.getCustomerId());
        entity.setRemark(request.getRemark());
        entity.setUpdateBy(SecurityUtils.currentUserId());
        sysUserMapper.updateById(entity);
        return UserVO.from(entity, rolesOf(id));
    }

    /**
     * 启用/禁用账号。禁止禁用自己，也禁止禁用最后一个启用状态的管理员。
     */
    @Transactional(rollbackFor = Exception.class)
    public UserVO updateStatus(Long id, Integer status) {
        SysUser entity = requireUser(id);
        Long currentUserId = SecurityUtils.currentUserId();
        if (status == 0) {
            if (id.equals(currentUserId)) {
                throw BusinessException.of(ErrorCode.PARAM_INVALID, "不能禁用当前登录账号");
            }
            if (isAdmin(id) && sysUserMapper.countEnabledAdmins() <= 1) {
                throw BusinessException.of(ErrorCode.PARAM_INVALID, "系统必须保留至少一个启用状态的管理员");
            }
        }
        sysUserMapper.update(null, Wrappers.<SysUser>lambdaUpdate()
                .eq(SysUser::getId, id)
                .set(SysUser::getStatus, status)
                .set(SysUser::getUpdateBy, currentUserId));
        entity.setStatus(status);
        return UserVO.from(entity, rolesOf(id));
    }

    /**
     * 重新分配角色。禁止把最后一个启用状态的管理员降权。
     */
    @Transactional(rollbackFor = Exception.class)
    public UserVO assignRoles(Long id, AssignRolesRequest request) {
        SysUser entity = requireUser(id);
        List<SysRole> roles = requireRoles(request.getRoleIds());
        boolean adminRequested = roles.stream().anyMatch(role -> AccessGuard.ROLE_ADMIN.equals(role.getRoleCode()));
        if (isAdmin(id) && !adminRequested && sysUserMapper.countEnabledAdmins() <= 1
                && entity.getStatus() != null && entity.getStatus() == 1) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID, "系统必须保留至少一个启用状态的管理员");
        }
        sysUserRoleMapper.deleteByUserId(id);
        roles.forEach(role -> insertUserRole(id, role.getId()));
        return UserVO.from(entity, roles.stream().map(RoleVO::from).toList());
    }

    /**
     * 管理员重置指定账号的密码，并清除其登录失败锁定状态。
     */
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long id, String newPassword) {
        requireUser(id);
        sysUserMapper.update(null, Wrappers.<SysUser>lambdaUpdate()
                .eq(SysUser::getId, id)
                .set(SysUser::getPassword, passwordEncoder.encode(newPassword))
                .set(SysUser::getLoginFailCount, 0)
                .set(SysUser::getLockUntil, null)
                .set(SysUser::getUpdateBy, SecurityUtils.currentUserId()));
    }

    /**
     * 删除用户（逻辑删除：deleted 写入本行 id，释放登录账号以便重新使用）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysUser entity = requireUser(id);
        Long currentUserId = SecurityUtils.currentUserId();
        if (id.equals(currentUserId)) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID, "不能删除当前登录账号");
        }
        if (isAdmin(id) && entity.getStatus() != null && entity.getStatus() == 1
                && sysUserMapper.countEnabledAdmins() <= 1) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID, "系统必须保留至少一个启用状态的管理员");
        }
        sysUserMapper.update(null, Wrappers.<SysUser>lambdaUpdate()
                .eq(SysUser::getId, id)
                .set(SysUser::getDeleted, id)
                .set(SysUser::getUpdateBy, currentUserId));
        sysUserRoleMapper.deleteByUserId(id);
    }

    private void insertUserRole(Long userId, Long roleId) {
        SysUserRole relation = new SysUserRole();
        relation.setUserId(userId);
        relation.setRoleId(roleId);
        sysUserRoleMapper.insert(relation);
    }

    private SysUser requireUser(Long id) {
        SysUser user = sysUserMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getId, id)
                .eq(SysUser::getDeleted, 0L));
        if (user == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private List<SysRole> requireRoles(List<Long> roleIds) {
        if (CollectionUtils.isEmpty(roleIds)) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID, "至少需要分配一个角色");
        }
        List<SysRole> roles = sysRoleMapper.selectList(Wrappers.<SysRole>lambdaQuery()
                .in(SysRole::getId, roleIds)
                .eq(SysRole::getDeleted, 0L));
        if (roles.size() != roleIds.stream().distinct().count()) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID, "存在无效的角色 ID");
        }
        return roles;
    }

    private boolean isAdmin(Long userId) {
        return sysRoleMapper.selectByUserId(userId).stream()
                .anyMatch(role -> AccessGuard.ROLE_ADMIN.equals(role.getRoleCode()));
    }

    private List<RoleVO> rolesOf(Long userId) {
        return sysRoleMapper.selectByUserId(userId).stream().map(RoleVO::from).toList();
    }

    private Map<Long, List<RoleVO>> loadRoles(List<SysUser> users) {
        Map<Long, List<RoleVO>> map = new LinkedHashMap<>();
        if (CollectionUtils.isEmpty(users)) {
            return map;
        }
        List<Long> userIds = users.stream().map(SysUser::getId).toList();
        for (UserRoleView row : sysRoleMapper.selectRolesByUserIds(userIds)) {
            RoleVO role = new RoleVO();
            role.setId(row.getId());
            role.setRoleCode(row.getRoleCode());
            role.setRoleName(row.getRoleName());
            role.setDescription(row.getDescription());
            role.setSort(row.getSort());
            role.setStatus(row.getStatus());
            map.computeIfAbsent(row.getUserId(), key -> new ArrayList<>()).add(role);
        }
        return map;
    }
}
