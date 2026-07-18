package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.example.demo.common.BusinessException;
import com.example.demo.converter.RoleConverter;
import com.example.demo.dto.role.RoleCreateRequest;
import com.example.demo.dto.role.RolePageQuery;
import com.example.demo.dto.role.RoleUpdateRequest;
import com.example.demo.entity.Role;
import com.example.demo.mapper.RoleMapper;
import com.example.demo.service.RoleService;
import com.example.demo.validator.TenantScopeValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;

/** 角色业务实现。 */
@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_DISABLED = "DISABLED";

    private final TenantScopeValidator tenantScopeValidator;

    public RoleServiceImpl(TenantScopeValidator tenantScopeValidator) {
        this.tenantScopeValidator = tenantScopeValidator;
    }

    @Override
    @Transactional
    public Role createRole(RoleCreateRequest request) {
        Long tenantId = requirePositiveId(request.getTenantId(), "tenantId");
        String roleCode = normalizeRoleCode(request.getRoleCode());
        tenantScopeValidator.requireActiveTenant(tenantId);

        boolean roleCodeExists = lambdaQuery()
                .eq(Role::getTenantId, tenantId)
                .eq(Role::getRoleCode, roleCode)
                .exists();
        if (roleCodeExists) {
            throw new BusinessException(30001, "该租户下的角色编码已存在");
        }

        Role role = RoleConverter.toEntity(request);
        role.setRoleCode(roleCode);
        role.setBuiltIn(false);
        role.setStatus(STATUS_ACTIVE);

        if (!save(role)) {
            throw new IllegalStateException("创建角色失败");
        }
        return role;
    }

    @Override
    @Transactional
    public Role updateRole(Long tenantId, RoleUpdateRequest request) {
        Role role = getRole(tenantId, request.getId());
        assertCustomRole(role);
        RoleConverter.copyToEntity(request, role);

        if (!updateById(role)) {
            throw new BusinessException(30002, "角色数据已被其他操作修改，请刷新后重试");
        }
        return role;
    }

    @Override
    public Role getRole(Long tenantId, Long roleId) {
        Long checkedTenantId = requirePositiveId(tenantId, "tenantId");
        Long checkedRoleId = requirePositiveId(roleId, "roleId");

        Role role = lambdaQuery()
                .eq(Role::getTenantId, checkedTenantId)
                .eq(Role::getId, checkedRoleId)
                .one();
        if (role == null) {
            throw new BusinessException(20001, "角色不存在");
        }
        return role;
    }

    @Override
    public Page<Role> pageRoles(RolePageQuery query) {
        Long tenantId = requirePositiveId(query.getTenantId(), "tenantId");

        return page(
                new Page<>(query.getPageNum(), query.getPageSize()),
                Wrappers.lambdaQuery(Role.class)
                        .eq(Role::getTenantId, tenantId)
                        .like(StringUtils.hasText(query.getRoleCode()),
                                Role::getRoleCode, query.getRoleCode())
                        .like(StringUtils.hasText(query.getRoleName()),
                                Role::getRoleName, query.getRoleName())
                        .eq(StringUtils.hasText(query.getStatus()),
                                Role::getStatus, query.getStatus())
                        .orderByDesc(Role::getCreatedAt)
        );
    }

    @Override
    @Transactional
    public void disableRole(Long tenantId, Long roleId) {
        Role role = getRole(tenantId, roleId);
        assertCustomRole(role);
        if (STATUS_DISABLED.equals(role.getStatus())) {
            return;
        }

        role.setStatus(STATUS_DISABLED);
        if (!updateById(role)) {
            throw new BusinessException(30002, "角色数据已被其他操作修改，请刷新后重试");
        }
    }

    private String normalizeRoleCode(String roleCode) {
        if (!StringUtils.hasText(roleCode)) {
            throw new BusinessException(10001, "roleCode 不能为空");
        }
        String normalized = roleCode.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("^[A-Z][A-Z0-9_]{0,63}$")) {
            throw new BusinessException(10001, "roleCode 只能包含大写字母、数字和下划线，且必须以字母开头");
        }
        return normalized;
    }

    private void assertCustomRole(Role role) {
        if (Boolean.TRUE.equals(role.getBuiltIn())) {
            throw new BusinessException(30001, "内置角色不允许通过普通接口修改或停用");
        }
    }

    private Long requirePositiveId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new BusinessException(10001, fieldName + " 必须是正整数");
        }
        return id;
    }
}
