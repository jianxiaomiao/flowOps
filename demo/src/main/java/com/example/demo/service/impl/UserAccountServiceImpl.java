package com.example.demo.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.example.demo.common.BusinessException;
import com.example.demo.audit.AuditOperation;
import com.example.demo.converter.UserAccountConverter;
import com.example.demo.dto.user.UserAccountCreateRequest;
import com.example.demo.dto.user.UserAccountPageQuery;
import com.example.demo.dto.user.UserAccountUpdateRequest;
import com.example.demo.entity.UserAccount;
import com.example.demo.mapper.UserAccountMapper;
import com.example.demo.service.UserAccountService;
import com.example.demo.security.TenantContext;
import com.example.demo.validator.TenantScopeValidator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * 用户账号管理实现，负责租户内用户名唯一、部门归属、密码摘要和停用逻辑。
 */
@Service
public class UserAccountServiceImpl
        extends ServiceImpl<UserAccountMapper, UserAccount>
        implements UserAccountService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_DISABLED = "DISABLED";

    private final TenantScopeValidator tenantScopeValidator;
    private final PasswordEncoder passwordEncoder;
    private final TenantContext tenantContext;

    public UserAccountServiceImpl(
            TenantScopeValidator tenantScopeValidator,
            PasswordEncoder passwordEncoder,
            TenantContext tenantContext
    ) {
        this.tenantScopeValidator = tenantScopeValidator;
        this.passwordEncoder = passwordEncoder;
        this.tenantContext = tenantContext;
    }

    @Override
    @Transactional
    @AuditOperation(module = "USER", operation = "CREATE", resourceType = "USER_ACCOUNT")
    /**
     * 创建当前租户的启用用户，并将明文密码转为 BCrypt 摘要。
     *
     * @param request 用户资料、部门和初始明文密码。
     * @return 已保存用户；调用方不得向响应暴露 passwordHash。
     * @throws BusinessException 当用户名重复、部门不属于当前租户或密码不合规时抛出。
     */
    public UserAccount createUser(UserAccountCreateRequest request) {
        Long tenantId = tenantContext.tenantId();
        Long departmentId = requirePositiveId(request.getDepartmentId(), "departmentId");
        String username = normalizeUsername(request.getUsername());
        validateRawPassword(request.getRawPassword());

        tenantScopeValidator.requireActiveTenant(tenantId);
        tenantScopeValidator.requireActiveDepartment(tenantId, departmentId);

        boolean usernameExists = lambdaQuery()
                .eq(UserAccount::getTenantId, tenantId)
                .eq(UserAccount::getUsername, username)
                .exists();
        if (usernameExists) {
            throw new BusinessException(30001, "该租户下的用户名已存在");
        }

        UserAccount userAccount = UserAccountConverter.toEntity(request);
        userAccount.setTenantId(tenantId);
        userAccount.setCreatedBy(tenantContext.userId());
        userAccount.setUpdatedBy(tenantContext.userId());
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
    @AuditOperation(module = "USER", operation = "UPDATE", resourceType = "USER_ACCOUNT")
    /**
     * 更新用户的可变资料，例如显示名、联系方式和所属部门。
     *
     * @param tenantId 当前租户 ID。
     * @param request 待更新资料和乐观锁版本。
     * @return 更新后的用户。
     * @throws BusinessException 当用户、部门不属于租户或版本冲突时抛出。
     */
    public UserAccount updateUser(Long tenantId, UserAccountUpdateRequest request) {
        Long checkedTenantId = requirePositiveId(tenantId, "tenantId");
        Long departmentId = requirePositiveId(request.getDepartmentId(), "departmentId");
        UserAccount userAccount = getUser(checkedTenantId, request.getId());

        tenantScopeValidator.requireActiveDepartment(checkedTenantId, departmentId);
        UserAccountConverter.copyToEntity(request, userAccount);

        if (!updateById(userAccount)) {
            throw new BusinessException(30002, "用户数据已被其他操作修改，请刷新后重试");
        }
        return userAccount;
    }

    @Override
    /** @param tenantId 当前租户 ID。@param userId 用户 ID。@return 租户范围内用户。@throws BusinessException 当用户不存在时抛出。 */
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
    /** @param query 分页及部门、用户名、显示名、状态筛选条件。@return 当前租户用户分页。 */
    public Page<UserAccount> pageUsers(UserAccountPageQuery query) {
        Long tenantId = tenantContext.tenantId();

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
    @AuditOperation(module = "USER", operation = "DISABLE", resourceType = "USER_ACCOUNT")
    /**
     * 停用账号而非物理删除，避免历史工单、审计和授权关系丢失引用。
     *
     * @param tenantId 当前租户 ID。
     * @param userId 待停用用户 ID。
     */
    public void disableUser(Long tenantId, Long userId) {
        UserAccount userAccount = getUser(tenantId, userId);
        if (STATUS_DISABLED.equals(userAccount.getStatus())) {
            return;
        }

        userAccount.setStatus(STATUS_DISABLED);
        userAccount.setUpdatedBy(tenantContext.userId());
        if (!updateById(userAccount)) {
            throw new BusinessException(30002, "用户数据已被其他操作修改，请刷新后重试");
        }
    }

    @Override
    @Transactional
    @AuditOperation(module = "USER", operation = "ENABLE", resourceType = "USER_ACCOUNT")
    public void enableUser(Long tenantId, Long userId) {
        UserAccount userAccount = getUser(tenantId, userId);
        if (STATUS_ACTIVE.equals(userAccount.getStatus())) {
            return;
        }
        tenantScopeValidator.requireActiveTenant(tenantId);
        tenantScopeValidator.requireActiveDepartment(tenantId, userAccount.getDepartmentId());
        userAccount.setStatus(STATUS_ACTIVE);
        userAccount.setUpdatedBy(tenantContext.userId());
        if (!updateById(userAccount)) {
            throw new BusinessException(30002, "用户数据已被其他操作修改，请刷新后重试");
        }
    }

    /** @param username 原用户名。@return 去除首尾空格且转小写的用户名。@throws BusinessException 当用户名为空时抛出。 */
    private String normalizeUsername(String username) {
        if (!StringUtils.hasText(username)) {
            throw new BusinessException(10001, "username 不能为空");
        }
        return username.trim().toLowerCase(Locale.ROOT);
    }

    /** @param rawPassword 明文初始密码。@throws BusinessException 当密码少于 8 字符或 UTF-8 编码后超过 BCrypt 的 72 字节限制时抛出。 */
    private void validateRawPassword(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < 8) {
            throw new BusinessException(10001, "密码长度不能少于 8 个字符");
        }
        if (rawPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BusinessException(10001, "密码使用 UTF-8 编码后不能超过 72 字节");
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
