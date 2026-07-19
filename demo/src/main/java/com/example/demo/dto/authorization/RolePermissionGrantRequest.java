package com.example.demo.dto.authorization;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/** 给角色授予平台权限；operatorId 在接入认证后将由登录上下文替代。 */
@Data
public class RolePermissionGrantRequest {
    @NotNull @Positive
    private Long tenantId;
    @NotNull @Positive
    private Long roleId;
    @NotNull @Positive
    private Long permissionId;
    @Positive
    private Long operatorId;
}
