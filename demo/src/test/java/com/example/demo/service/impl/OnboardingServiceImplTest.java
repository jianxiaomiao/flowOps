package com.example.demo.service.impl;

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
import com.example.demo.service.EmailVerificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnboardingServiceImplTest {
    @Mock TenantMapper tenantMapper;
    @Mock DepartmentMapper departmentMapper;
    @Mock UserAccountMapper userAccountMapper;
    @Mock RoleMapper roleMapper;
    @Mock PermissionMapper permissionMapper;
    @Mock RolePermissionMapper rolePermissionMapper;
    @Mock UserRoleMapper userRoleMapper;
    @Mock PasswordEncoder passwordEncoder;
    @Mock EmailVerificationService emailVerificationService;

    @Test
    void shouldCreateTenantDepartmentAdminAndRbacInOneUseCase() {
        when(tenantMapper.selectCount(any())).thenReturn(0L);
        when(tenantMapper.insert(any(Tenant.class))).thenAnswer(invocation -> { Tenant value = invocation.getArgument(0); value.setId(11L); return 1; });
        when(departmentMapper.insert(any(Department.class))).thenAnswer(invocation -> { Department value = invocation.getArgument(0); value.setId(12L); return 1; });
        when(userAccountMapper.insert(any(UserAccount.class))).thenAnswer(invocation -> { UserAccount value = invocation.getArgument(0); value.setId(13L); return 1; });
        when(roleMapper.insert(any(Role.class))).thenAnswer(invocation -> { Role value = invocation.getArgument(0); value.setId(14L); return 1; });
        Permission first = new Permission(); first.setId(1L);
        Permission second = new Permission(); second.setId(2L);
        when(permissionMapper.selectList(any())).thenReturn(List.of(first, second));
        when(rolePermissionMapper.insert(any(RolePermission.class))).thenReturn(1);
        when(userRoleMapper.insert(any(UserRole.class))).thenReturn(1);
        when(passwordEncoder.encode("Password@123")).thenReturn("bcrypt-hash");

        var response = service(true).registerTenant(request());

        assertEquals(11L, response.tenantId());
        assertEquals("NEW_CO", response.tenantCode());
        assertEquals(13L, response.adminUserId());
        assertEquals("admin", response.adminUsername());
        verify(rolePermissionMapper, org.mockito.Mockito.times(2)).insert(any(RolePermission.class));
        verify(userRoleMapper).insert(any(UserRole.class));
    }

    @Test
    void shouldRejectRegistrationWhenFeatureIsDisabled() {
        BusinessException error = assertThrows(BusinessException.class,
                () -> service(false).registerTenant(request()));

        assertEquals(10003, error.getCode());
        verifyNoInteractions(tenantMapper, departmentMapper, userAccountMapper, passwordEncoder);
    }

    @Test
    void shouldRejectDuplicateTenantCodeBeforeCreatingAnything() {
        when(tenantMapper.selectCount(any())).thenReturn(1L);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service(true).registerTenant(request()));

        assertEquals(30001, error.getCode());
        verify(tenantMapper, never()).insert(any(Tenant.class));
        verifyNoInteractions(departmentMapper, userAccountMapper, passwordEncoder);
    }

    private OnboardingServiceImpl service(boolean enabled) {
        return new OnboardingServiceImpl(tenantMapper, departmentMapper, userAccountMapper, roleMapper,
                permissionMapper, rolePermissionMapper, userRoleMapper, passwordEncoder,
                new RegistrationProperties(enabled, false), emailVerificationService);
    }

    private TenantRegistrationRequest request() {
        return new TenantRegistrationRequest(" new_co ", "新企业", " ADMIN ", "Password@123",
                "首位管理员", "admin@example.com", "13800138000");
    }
}
