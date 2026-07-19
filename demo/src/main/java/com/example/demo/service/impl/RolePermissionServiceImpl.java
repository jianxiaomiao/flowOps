package com.example.demo.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.example.demo.common.BusinessException;
import com.example.demo.entity.Role;
import com.example.demo.entity.RolePermission;
import com.example.demo.mapper.RolePermissionMapper;
import com.example.demo.service.PermissionService;
import com.example.demo.service.RolePermissionService;
import com.example.demo.service.RoleService;
import com.example.demo.validator.TenantScopeValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RolePermissionServiceImpl extends ServiceImpl<RolePermissionMapper, RolePermission> implements RolePermissionService {
    private static final String STATUS_ACTIVE = "ACTIVE";
    private final TenantScopeValidator tenantScopeValidator;
    private final RoleService roleService;
    private final PermissionService permissionService;

    public RolePermissionServiceImpl(TenantScopeValidator tenantScopeValidator, RoleService roleService, PermissionService permissionService) {
        this.tenantScopeValidator = tenantScopeValidator;
        this.roleService = roleService;
        this.permissionService = permissionService;
    }

    @Override
    @Transactional
    public RolePermission grant(Long tenantId, Long roleId, Long permissionId, Long operatorId) {
        tenantScopeValidator.requireActiveTenant(requirePositive(tenantId, "tenantId"));
        Role role = roleService.getRole(tenantId, requirePositive(roleId, "roleId"));
        permissionService.getActivePermission(requirePositive(permissionId, "permissionId"));
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
    public void revoke(Long tenantId, Long roleId, Long permissionId) {
        boolean removed = lambdaUpdate().eq(RolePermission::getTenantId, requirePositive(tenantId, "tenantId"))
                .eq(RolePermission::getRoleId, requirePositive(roleId, "roleId"))
                .eq(RolePermission::getPermissionId, requirePositive(permissionId, "permissionId")).remove();
        if (!removed) throw new BusinessException(20001, "角色未拥有该权限");
    }

    @Override
    public List<RolePermission> listByRole(Long tenantId, Long roleId) {
        roleService.getRole(tenantId, roleId);
        return lambdaQuery().eq(RolePermission::getTenantId, tenantId).eq(RolePermission::getRoleId, roleId).list();
    }

    private Long requirePositive(Long value, String field) {
        if (value == null || value <= 0) throw new BusinessException(10001, field + " 必须是正整数");
        return value;
    }
}
