package com.example.demo.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.example.demo.common.BusinessException;
import com.example.demo.converter.UserAccountConverter;
import com.example.demo.dto.user.UserAccountCreateRequest;
import com.example.demo.dto.user.UserAccountPageQuery;
import com.example.demo.dto.user.UserAccountUpdateRequest;
import com.example.demo.entity.Department;
import com.example.demo.entity.Tenant;
import com.example.demo.entity.UserAccount;
import com.example.demo.mapper.UserAccountMapper;
import com.example.demo.service.DepartmentService;
import com.example.demo.service.TenantService;
import com.example.demo.service.UserAccountService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** 用户账号业务实现。 */
@Service
public class UserAccountServiceImpl
        extends ServiceImpl<UserAccountMapper, UserAccount>
        implements UserAccountService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_DISABLED = "DISABLED";

    private final TenantService tenantService;
    private final DepartmentService departmentService;
    private final PasswordEncoder passwordEncoder;

    public UserAccountServiceImpl(
            TenantService tenantService,
            DepartmentService departmentService,
            PasswordEncoder passwordEncoder
    ) {
        this.tenantService = tenantService;
        this.departmentService = departmentService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public UserAccount createUser(UserAccountCreateRequest request) {
        Long tenantId = requirePositiveId(request.getTenantId(), "tenantId");
        Long departmentId = requirePositiveId(request.getDepartmentId(), "departmentId");
        String username = normalizeUsername(request.getUsername());
        validateRawPassword(request.getRawPassword());

        validateActiveTenant(tenantId);
        validateActiveDepartment(tenantId, departmentId);

        boolean usernameExists = lambdaQuery()
                .eq(UserAccount::getTenantId, tenantId)
                .eq(UserAccount::getUsername, username)
                .exists();
        if (usernameExists) {
            throw new BusinessException(30001, "该租户下的用户名已存在");
        }

        UserAccount userAccount = UserAccountConverter.toEntity(request);
        userAccount.setUsername(username);
        userAccount.setPasswordHash(passwordEncoder.encode(request.getRawPassword()));
        userAccount.setPasswordChangedAt(LocalDateTime.now());
        userAccount.setStatus(STATUS_ACTIVE);

        if (!save(userAccount)) {
            throw new IllegalStateException("创建用户失败");
        }
        return userAccount;
    }

    @Override
    @Transactional
    public UserAccount updateUser(Long tenantId, UserAccountUpdateRequest request) {
        Long checkedTenantId = requirePositiveId(tenantId, "tenantId");
        Long departmentId = requirePositiveId(request.getDepartmentId(), "departmentId");
        UserAccount userAccount = getUser(checkedTenantId, request.getId());

        validateActiveDepartment(checkedTenantId, departmentId);
        UserAccountConverter.copyToEntity(request, userAccount);

        if (!updateById(userAccount)) {
            throw new BusinessException(30002, "用户数据已被其他操作修改，请刷新后重试");
        }
        return userAccount;
    }

    @Override
    public UserAccount getUser(Long tenantId, Long userId) {
        Long checkedTenantId = requirePositiveId(tenantId, "tenantId");
        Long checkedUserId = requirePositiveId(userId, "userId");

        UserAccount userAccount = lambdaQuery()
                .eq(UserAccount::getTenantId, checkedTenantId)
                .eq(UserAccount::getId, checkedUserId)
                .one();
        if (userAccount == null) {
            throw new BusinessException(20001, "用户不存在");
        }
        return userAccount;
    }

    @Override
    public Page<UserAccount> pageUsers(UserAccountPageQuery query) {
        Long tenantId = requirePositiveId(query.getTenantId(), "tenantId");

        return page(
                new Page<>(query.getPageNum(), query.getPageSize()),
                Wrappers.lambdaQuery(UserAccount.class)
                        .eq(UserAccount::getTenantId, tenantId)
                        .eq(query.getDepartmentId() != null,
                                UserAccount::getDepartmentId, query.getDepartmentId())
                        .like(StringUtils.hasText(query.getUsername()),
                                UserAccount::getUsername, query.getUsername())
                        .like(StringUtils.hasText(query.getDisplayName()),
                                UserAccount::getDisplayName, query.getDisplayName())
                        .eq(StringUtils.hasText(query.getStatus()),
                                UserAccount::getStatus, query.getStatus())
                        .orderByDesc(UserAccount::getCreatedAt)
        );
    }

    @Override
    @Transactional
    public void disableUser(Long tenantId, Long userId) {
        UserAccount userAccount = getUser(tenantId, userId);
        if (STATUS_DISABLED.equals(userAccount.getStatus())) {
            return;
        }

        userAccount.setStatus(STATUS_DISABLED);
        if (!updateById(userAccount)) {
            throw new BusinessException(30002, "用户数据已被其他操作修改，请刷新后重试");
        }
    }

    private void validateActiveTenant(Long tenantId) {
        boolean exists = tenantService.lambdaQuery()
                .eq(Tenant::getId, tenantId)
                .eq(Tenant::getStatus, STATUS_ACTIVE)
                .exists();
        if (!exists) {
            throw new BusinessException(20001, "租户不存在或已停用");
        }
    }

    private void validateActiveDepartment(Long tenantId, Long departmentId) {
        boolean exists = departmentService.lambdaQuery()
                .eq(Department::getId, departmentId)
                .eq(Department::getTenantId, tenantId)
                .eq(Department::getStatus, STATUS_ACTIVE)
                .exists();
        if (!exists) {
            throw new BusinessException(20001, "部门不存在、已停用或不属于当前租户");
        }
    }

    private String normalizeUsername(String username) {
        if (!StringUtils.hasText(username)) {
            throw new BusinessException(10001, "username 不能为空");
        }
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private void validateRawPassword(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < 8) {
            throw new BusinessException(10001, "密码长度不能少于 8 个字符");
        }
        if (rawPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BusinessException(10001, "密码使用 UTF-8 编码后不能超过 72 字节");
        }
    }

    private Long requirePositiveId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new BusinessException(10001, fieldName + " 必须是正整数");
        }
        return id;
    }
}
