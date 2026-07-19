package com.example.demo.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.example.demo.common.BusinessException;
import com.example.demo.entity.Role;
import com.example.demo.entity.UserAccount;
import com.example.demo.entity.UserRole;
import com.example.demo.mapper.UserRoleMapper;
import com.example.demo.service.RoleService;
import com.example.demo.service.UserAccountService;
import com.example.demo.service.UserRoleService;
import com.example.demo.validator.TenantScopeValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserRoleServiceImpl extends ServiceImpl<UserRoleMapper, UserRole> implements UserRoleService {
    private static final String STATUS_ACTIVE = "ACTIVE";
    private final TenantScopeValidator tenantScopeValidator;
    private final UserAccountService userAccountService;
    private final RoleService roleService;

    public UserRoleServiceImpl(TenantScopeValidator tenantScopeValidator, UserAccountService userAccountService, RoleService roleService) {
        this.tenantScopeValidator = tenantScopeValidator;
        this.userAccountService = userAccountService;
        this.roleService = roleService;
    }

    @Override
    @Transactional
    public UserRole grant(Long tenantId, Long userId, Long roleId, Long operatorId) {
        tenantScopeValidator.requireActiveTenant(requirePositive(tenantId, "tenantId"));
        UserAccount user = userAccountService.getUser(tenantId, requirePositive(userId, "userId"));
        Role role = roleService.getRole(tenantId, requirePositive(roleId, "roleId"));
        if (!STATUS_ACTIVE.equals(user.getStatus()) || !STATUS_ACTIVE.equals(role.getStatus())) {
            throw new BusinessException(30001, "只能向启用的用户授予启用角色");
        }
        if (lambdaQuery().eq(UserRole::getTenantId, tenantId).eq(UserRole::getUserId, userId).eq(UserRole::getRoleId, roleId).exists()) {
            throw new BusinessException(30001, "该用户已拥有此角色");
        }
        UserRole relation = new UserRole();
        relation.setTenantId(tenantId); relation.setUserId(userId); relation.setRoleId(roleId);
        relation.setAssignedBy(operatorId); relation.setAssignedAt(LocalDateTime.now());
        if (!save(relation)) throw new IllegalStateException("授予用户角色失败");
        return relation;
    }

    @Override
    @Transactional
    public void revoke(Long tenantId, Long userId, Long roleId) {
        boolean removed = lambdaUpdate().eq(UserRole::getTenantId, requirePositive(tenantId, "tenantId"))
                .eq(UserRole::getUserId, requirePositive(userId, "userId"))
                .eq(UserRole::getRoleId, requirePositive(roleId, "roleId")).remove();
        if (!removed) throw new BusinessException(20001, "用户未拥有该角色");
    }

    @Override
    public List<UserRole> listByUser(Long tenantId, Long userId) {
        userAccountService.getUser(tenantId, userId);
        return lambdaQuery().eq(UserRole::getTenantId, tenantId).eq(UserRole::getUserId, userId).list();
    }

    private Long requirePositive(Long value, String field) {
        if (value == null || value <= 0) throw new BusinessException(10001, field + " 必须是正整数");
        return value;
    }
}
