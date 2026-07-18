package com.example.demo.converter;

import com.example.demo.dto.user.UserAccountCreateRequest;
import com.example.demo.dto.user.UserAccountUpdateRequest;
import com.example.demo.entity.UserAccount;
import com.example.demo.vo.UserAccountResponse;

/**
 * UserAccount 的对象转换器。
 *
 * <p>rawPassword 的哈希属于安全业务规则，因此故意不在此类中处理。</p>
 */
public final class UserAccountConverter {

    private UserAccountConverter() {
    }

    public static UserAccount toEntity(UserAccountCreateRequest request) {
        UserAccount userAccount = new UserAccount();
        userAccount.setTenantId(request.getTenantId());
        userAccount.setDepartmentId(request.getDepartmentId());
        userAccount.setUsername(request.getUsername());
        userAccount.setDisplayName(request.getDisplayName());
        userAccount.setEmail(request.getEmail());
        userAccount.setPhone(request.getPhone());
        return userAccount;
    }

    public static void copyToEntity(UserAccountUpdateRequest request, UserAccount userAccount) {
        userAccount.setDepartmentId(request.getDepartmentId());
        userAccount.setDisplayName(request.getDisplayName());
        userAccount.setEmail(request.getEmail());
        userAccount.setPhone(request.getPhone());
    }

    public static UserAccountResponse toResponse(UserAccount userAccount) {
        UserAccountResponse response = new UserAccountResponse();
        response.setId(userAccount.getId());
        response.setTenantId(userAccount.getTenantId());
        response.setDepartmentId(userAccount.getDepartmentId());
        response.setUsername(userAccount.getUsername());
        response.setDisplayName(userAccount.getDisplayName());
        response.setEmail(userAccount.getEmail());
        response.setPhone(userAccount.getPhone());
        response.setStatus(userAccount.getStatus());
        response.setLastLoginAt(userAccount.getLastLoginAt());
        response.setCreatedAt(userAccount.getCreatedAt());
        response.setUpdatedAt(userAccount.getUpdatedAt());
        return response;
    }
}
