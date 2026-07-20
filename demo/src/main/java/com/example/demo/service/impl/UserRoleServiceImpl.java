package com.example.demo.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.example.demo.common.BusinessException;
import com.example.demo.audit.AuditOperation;
import com.example.demo.entity.Role;
import com.example.demo.entity.UserAccount;
import com.example.demo.entity.UserRole;
import com.example.demo.mapper.UserRoleMapper;
import com.example.demo.service.RoleService;
import com.example.demo.service.UserAccountService;
import com.example.demo.service.UserRoleService;
import com.example.demo.security.TenantContext;
import com.example.demo.validator.TenantScopeValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
/**
 * 管理用户与角色的多对多授权关系，并保证两端属于同一租户。
 */
public class UserRoleServiceImpl extends ServiceImpl<UserRoleMapper, UserRole> implements UserRoleService {
    private static final String STATUS_ACTIVE = "ACTIVE";
    private final TenantScopeValidator tenantScopeValidator;
    private final UserAccountService userAccountService;
    private final RoleService roleService;
    private final TenantContext tenantContext;

    public UserRoleServiceImpl(TenantScopeValidator tenantScopeValidator, UserAccountService userAccountService, RoleService roleService, TenantContext tenantContext) {
        this.tenantScopeValidator = tenantScopeValidator;
        this.userAccountService = userAccountService;
        this.roleService = roleService;
        this.tenantContext = tenantContext;
    }

    @Override
    @Transactional
    @AuditOperation(module = "RBAC", operation = "GRANT_USER_ROLE", resourceType = "USER_ROLE")
    /**
     * 向当前租户的启用用户授予启用角色。
     *
     * @param tenantId 调用参数会被当前 JWT 的可信租户覆盖。
     * @param userId 目标用户 ID。
     * @param roleId 要授予的角色 ID。
     * @param operatorId 调用参数会被当前 JWT 的操作人覆盖。
     * @return 新建用户角色关系。
     * @throws BusinessException 当用户或角色停用、关系重复或不属于租户时抛出。
     */
    public UserRole grant(Long tenantId, Long userId, Long roleId, Long operatorId) {
        tenantId = tenantContext.tenantId();
        operatorId = tenantContext.userId();
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
    @AuditOperation(module = "RBAC", operation = "REVOKE_USER_ROLE", resourceType = "USER_ROLE")
    /** @param tenantId 当前租户 ID。@param userId 用户 ID。@param roleId 要撤销的角色 ID。@throws BusinessException 当关系不存在时抛出。 */
    public void revoke(Long tenantId, Long userId, Long roleId) {
        boolean removed = lambdaUpdate().eq(UserRole::getTenantId, requirePositive(tenantId, "tenantId"))
                .eq(UserRole::getUserId, requirePositive(userId, "userId"))
                .eq(UserRole::getRoleId, requirePositive(roleId, "roleId")).remove();
        if (!removed) throw new BusinessException(20001, "用户未拥有该角色");
    }

    @Override
    /** @param tenantId 当前租户 ID。@param userId 用户 ID。@return 用户的角色关系列表。 */
    public List<UserRole> listByUser(Long tenantId, Long userId) {
        userAccountService.getUser(tenantId, userId);
        return lambdaQuery().eq(UserRole::getTenantId, tenantId).eq(UserRole::getUserId, userId).list();
    }

    /** @param value 待校验 ID。@param field 字段名。@return 原 ID。@throws BusinessException 当 ID 非正数时抛出。 */
    private Long requirePositive(Long value, String field) {
        if (value == null || value <= 0) throw new BusinessException(10001, field + " 必须是正整数");
        return value;
    }
}
