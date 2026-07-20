package com.example.demo.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.example.demo.common.BusinessException;
import com.example.demo.audit.AuditOperation;
import com.example.demo.entity.Role;
import com.example.demo.entity.Permission;
import com.example.demo.entity.RolePermission;
import com.example.demo.mapper.RolePermissionMapper;
import com.example.demo.service.PermissionService;
import com.example.demo.service.RolePermissionService;
import com.example.demo.service.RoleService;
import com.example.demo.security.TenantContext;
import com.example.demo.validator.TenantScopeValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
/**
 * 管理角色与权限的多对多授权关系，并校验角色属于当前租户。
 */
public class RolePermissionServiceImpl extends ServiceImpl<RolePermissionMapper, RolePermission> implements RolePermissionService {
    private static final String STATUS_ACTIVE = "ACTIVE";
    private final TenantScopeValidator tenantScopeValidator;
    private final RoleService roleService;
    private final PermissionService permissionService;
    private final TenantContext tenantContext;

    public RolePermissionServiceImpl(TenantScopeValidator tenantScopeValidator, RoleService roleService, PermissionService permissionService, TenantContext tenantContext) {
        this.tenantScopeValidator = tenantScopeValidator;
        this.roleService = roleService;
        this.permissionService = permissionService;
        this.tenantContext = tenantContext;
    }

    @Override
    @Transactional
    @AuditOperation(module = "RBAC", operation = "GRANT_ROLE_PERMISSION", resourceType = "ROLE_PERMISSION")
    /**
     * 为当前租户角色授予一项非平台级权限。
     *
     * @param tenantId 调用参数会被当前 JWT 的可信租户覆盖。
     * @param roleId 目标角色 ID。
     * @param permissionId 要授予的权限 ID。
     * @param operatorId 调用参数会被当前 JWT 的操作人覆盖。
     * @return 新建授权关系。
     * @throws BusinessException 当关系重复、角色停用或权限是平台级时抛出。
     */
    public RolePermission grant(Long tenantId, Long roleId, Long permissionId, Long operatorId) {
        tenantId = tenantContext.tenantId();
        operatorId = tenantContext.userId();
        tenantScopeValidator.requireActiveTenant(requirePositive(tenantId, "tenantId"));
        Role role = roleService.getRole(tenantId, requirePositive(roleId, "roleId"));
        Permission permission = permissionService.getActivePermission(requirePositive(permissionId, "permissionId"));
        if ("PLATFORM".equals(permission.getScope())) {
            throw new BusinessException(10003, "租户角色不能被授予平台级权限");
        }
        if (!STATUS_ACTIVE.equals(role.getStatus())) throw new BusinessException(30001, "不能向已停用角色授予权限");
        if (lambdaQuery().eq(RolePermission::getTenantId, tenantId).eq(RolePermission::getRoleId, roleId).eq(RolePermission::getPermissionId, permissionId).exists()) {
            throw new BusinessException(30001, "该角色已拥有此权限");
        }
        RolePermission relation = new RolePermission();
        relation.setTenantId(tenantId); relation.setRoleId(roleId); relation.setPermissionId(permissionId);
        relation.setGrantedBy(operatorId); relation.setGrantedAt(LocalDateTime.now());
        if (!save(relation)) throw new IllegalStateException("授予角色权限失败");
        return relation;
    }

    @Override
    @Transactional
    @AuditOperation(module = "RBAC", operation = "REVOKE_ROLE_PERMISSION", resourceType = "ROLE_PERMISSION")
    /** @param tenantId 当前租户 ID。@param roleId 角色 ID。@param permissionId 要撤销的权限 ID。@throws BusinessException 当授权关系不存在时抛出。 */
    public void revoke(Long tenantId, Long roleId, Long permissionId) {
        boolean removed = lambdaUpdate().eq(RolePermission::getTenantId, requirePositive(tenantId, "tenantId"))
                .eq(RolePermission::getRoleId, requirePositive(roleId, "roleId"))
                .eq(RolePermission::getPermissionId, requirePositive(permissionId, "permissionId")).remove();
        if (!removed) throw new BusinessException(20001, "角色未拥有该权限");
    }

    @Override
    /** @param tenantId 当前租户 ID。@param roleId 角色 ID。@return 该角色的授权关系列表。 */
    public List<RolePermission> listByRole(Long tenantId, Long roleId) {
        roleService.getRole(tenantId, roleId);
        return lambdaQuery().eq(RolePermission::getTenantId, tenantId).eq(RolePermission::getRoleId, roleId).list();
    }

    /** @param value 待校验 ID。@param field 字段名。@return 原 ID。@throws BusinessException 当 ID 非正数时抛出。 */
    private Long requirePositive(Long value, String field) {
        if (value == null || value <= 0) throw new BusinessException(10001, field + " 必须是正整数");
        return value;
    }
}
