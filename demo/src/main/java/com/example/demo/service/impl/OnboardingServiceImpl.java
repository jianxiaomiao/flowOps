package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.demo.common.BusinessException;
import com.example.demo.config.RegistrationProperties;
import com.example.demo.dto.auth.TenantRegistrationRequest;
import com.example.demo.entity.Department;
import com.example.demo.entity.Permission;
import com.example.demo.entity.Role;
import com.example.demo.entity.RolePermission;
import com.example.demo.entity.Tenant;
import com.example.demo.entity.UserAccount;
import com.example.demo.entity.UserRole;
import com.example.demo.mapper.DepartmentMapper;
import com.example.demo.mapper.PermissionMapper;
import com.example.demo.mapper.RoleMapper;
import com.example.demo.mapper.RolePermissionMapper;
import com.example.demo.mapper.TenantMapper;
import com.example.demo.mapper.UserAccountMapper;
import com.example.demo.mapper.UserRoleMapper;
import com.example.demo.service.OnboardingService;
import com.example.demo.vo.TenantRegistrationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class OnboardingServiceImpl implements OnboardingService {
    private static final String STATUS_ACTIVE = "ACTIVE";

    private final TenantMapper tenantMapper;
    private final DepartmentMapper departmentMapper;
    private final UserAccountMapper userAccountMapper;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final RegistrationProperties registrationProperties;

    @Override
    @Transactional
    public TenantRegistrationResponse registerTenant(TenantRegistrationRequest request) {
        if (!registrationProperties.enabled()) {
            throw new BusinessException(10003, "当前环境未开放企业注册");
        }
        validatePasswordBytes(request.adminPassword());
        String tenantCode = request.tenantCode().trim().toUpperCase(Locale.ROOT);
        String username = request.adminUsername().trim().toLowerCase(Locale.ROOT);
        if (tenantMapper.selectCount(Wrappers.lambdaQuery(Tenant.class)
                .eq(Tenant::getTenantCode, tenantCode)) > 0) {
            throw new BusinessException(30001, "企业编码已被使用");
        }

        Tenant tenant = new Tenant();
        tenant.setTenantCode(tenantCode);
        tenant.setTenantName(request.tenantName().trim());
        tenant.setStatus(STATUS_ACTIVE);
        tenant.setContactName(request.adminDisplayName().trim());
        tenant.setContactEmail(blankToNull(request.adminEmail()));
        tenant.setContactPhone(blankToNull(request.adminPhone()));
        requireInserted(tenantMapper.insert(tenant), "创建租户失败");

        Department department = new Department();
        department.setTenantId(tenant.getId());
        department.setDepartmentCode("GENERAL");
        department.setDepartmentName("默认部门");
        department.setSortOrder(10);
        department.setStatus(STATUS_ACTIVE);
        requireInserted(departmentMapper.insert(department), "创建默认部门失败");

        UserAccount admin = new UserAccount();
        admin.setTenantId(tenant.getId());
        admin.setDepartmentId(department.getId());
        admin.setUsername(username);
        admin.setPasswordHash(passwordEncoder.encode(request.adminPassword()));
        admin.setDisplayName(request.adminDisplayName().trim());
        admin.setEmail(blankToNull(request.adminEmail()));
        admin.setPhone(blankToNull(request.adminPhone()));
        admin.setStatus(STATUS_ACTIVE);
        admin.setPasswordChangedAt(LocalDateTime.now());
        requireInserted(userAccountMapper.insert(admin), "创建管理员失败");

        Role adminRole = new Role();
        adminRole.setTenantId(tenant.getId());
        adminRole.setRoleCode("TENANT_ADMIN");
        adminRole.setRoleName("企业管理员");
        adminRole.setDescription("租户内全部管理权限");
        adminRole.setBuiltIn(true);
        adminRole.setStatus(STATUS_ACTIVE);
        adminRole.setCreatedBy(admin.getId());
        adminRole.setUpdatedBy(admin.getId());
        requireInserted(roleMapper.insert(adminRole), "创建管理员角色失败");

        List<Permission> permissions = permissionMapper.selectList(Wrappers.lambdaQuery(Permission.class)
                .eq(Permission::getStatus, STATUS_ACTIVE)
                .and(wrapper -> wrapper.isNull(Permission::getScope).or().ne(Permission::getScope, "PLATFORM")));
        if (permissions.isEmpty()) {
            throw new IllegalStateException("权限字典尚未初始化");
        }
        LocalDateTime now = LocalDateTime.now();
        for (Permission permission : permissions) {
            RolePermission relation = new RolePermission();
            relation.setTenantId(tenant.getId());
            relation.setRoleId(adminRole.getId());
            relation.setPermissionId(permission.getId());
            relation.setGrantedBy(admin.getId());
            relation.setGrantedAt(now);
            requireInserted(rolePermissionMapper.insert(relation), "初始化管理员权限失败");
        }

        UserRole userRole = new UserRole();
        userRole.setTenantId(tenant.getId());
        userRole.setUserId(admin.getId());
        userRole.setRoleId(adminRole.getId());
        userRole.setAssignedBy(admin.getId());
        userRole.setAssignedAt(now);
        requireInserted(userRoleMapper.insert(userRole), "绑定管理员角色失败");

        return new TenantRegistrationResponse(tenant.getId(), tenantCode, admin.getId(), username);
    }

    private void validatePasswordBytes(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BusinessException(10001, "密码使用 UTF-8 编码后不能超过 72 字节");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void requireInserted(int affectedRows, String message) {
        if (affectedRows != 1) {
            throw new IllegalStateException(message);
        }
    }
}
