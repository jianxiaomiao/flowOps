package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.example.demo.common.BusinessException;
import com.example.demo.audit.AuditOperation;
import com.example.demo.converter.RoleConverter;
import com.example.demo.dto.role.RoleCreateRequest;
import com.example.demo.dto.role.RolePageQuery;
import com.example.demo.dto.role.RoleUpdateRequest;
import com.example.demo.entity.Role;
import com.example.demo.mapper.RoleMapper;
import com.example.demo.service.RoleService;
import com.example.demo.security.TenantContext;
import com.example.demo.validator.TenantScopeValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * 租户角色管理实现，保护内置角色并保证角色编码在租户内唯一。
 */
@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_DISABLED = "DISABLED";

    private final TenantScopeValidator tenantScopeValidator;
    private final TenantContext tenantContext;

    public RoleServiceImpl(TenantScopeValidator tenantScopeValidator, TenantContext tenantContext) {
        this.tenantScopeValidator = tenantScopeValidator;
        this.tenantContext = tenantContext;
    }

    @Override
    @Transactional
    @AuditOperation(module = "RBAC", operation = "CREATE_ROLE", resourceType = "ROLE")
    /**
     * 在当前租户内创建自定义角色。
     *
     * @param request 新角色资料。
     * @return 已保存的自定义角色。
     * @throws BusinessException 当租户不可用或角色编码在该租户中重复时抛出。
     */
    public Role createRole(RoleCreateRequest request) {
        Long tenantId = tenantContext.tenantId();
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
        role.setTenantId(tenantId);
        role.setCreatedBy(tenantContext.userId());
        role.setUpdatedBy(tenantContext.userId());
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
    @AuditOperation(module = "RBAC", operation = "UPDATE_ROLE", resourceType = "ROLE")
    /**
     * 更新一个非内置角色的可变资料。
     *
     * @param tenantId 当前租户 ID。
     * @param request 待更新资料和乐观锁版本。
     * @return 更新后的角色。
     * @throws BusinessException 当角色为内置角色或版本冲突时抛出。
     */
    public Role updateRole(Long tenantId, RoleUpdateRequest request) {
        Role role = getRole(tenantId, request.getId());
        assertCustomRole(role);
        RoleConverter.copyToEntity(request, role);
        role.setUpdatedBy(tenantContext.userId());

        if (!updateById(role)) {
            throw new BusinessException(30002, "角色数据已被其他操作修改，请刷新后重试");
        }
        return role;
    }

    @Override
    /** @param tenantId 当前租户 ID。@param roleId 角色 ID。@return 租户范围内的角色。 */
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
    /** @param query 分页和角色筛选条件。@return 当前租户角色分页。 */
    public Page<Role> pageRoles(RolePageQuery query) {
        Long tenantId = tenantContext.tenantId();

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
    @AuditOperation(module = "RBAC", operation = "DISABLE_ROLE", resourceType = "ROLE")
    /**
     * 停用自定义角色而不是物理删除。
     *
     * @param tenantId 当前租户 ID。
     * @param roleId 待停用角色 ID。
     * @throws BusinessException 当尝试停用内置角色或出现版本冲突时抛出。
     */
    public void disableRole(Long tenantId, Long roleId) {
        Role role = getRole(tenantId, roleId);
        assertCustomRole(role);
        if (STATUS_DISABLED.equals(role.getStatus())) {
            return;
        }

        role.setStatus(STATUS_DISABLED);
        role.setUpdatedBy(tenantContext.userId());
        if (!updateById(role)) {
            throw new BusinessException(30002, "角色数据已被其他操作修改，请刷新后重试");
        }
    }

    @Override
    @Transactional
    @AuditOperation(module = "RBAC", operation = "ENABLE_ROLE", resourceType = "ROLE")
    public void enableRole(Long tenantId, Long roleId) {
        Role role = getRole(tenantId, roleId);
        assertCustomRole(role);
        if (STATUS_ACTIVE.equals(role.getStatus())) {
            return;
        }
        tenantScopeValidator.requireActiveTenant(tenantId);
        role.setStatus(STATUS_ACTIVE);
        role.setUpdatedBy(tenantContext.userId());
        if (!updateById(role)) {
            throw new BusinessException(30002, "角色数据已被其他操作修改，请刷新后重试");
        }
    }

    /** @param roleCode 原角色编码。@return 大写且去除空白后的角色编码。@throws BusinessException 当编码为空或格式非法时抛出。 */
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

    /** @param role 待检查角色。@throws BusinessException 当角色是系统内置角色时抛出。 */
    private void assertCustomRole(Role role) {
        if (Boolean.TRUE.equals(role.getBuiltIn())) {
            throw new BusinessException(30001, "内置角色不允许通过普通接口修改或停用");
        }
    }

    /** @param id 待校验 ID。@param fieldName 字段名。@return 原 ID。@throws BusinessException 当 ID 非正数时抛出。 */
    private Long requirePositiveId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new BusinessException(10001, fieldName + " 必须是正整数");
        }
        return id;
    }
}
