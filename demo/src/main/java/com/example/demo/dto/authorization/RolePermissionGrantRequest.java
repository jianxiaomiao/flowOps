package com.example.demo.dto.authorization;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/** 给当前租户的角色授予平台权限；租户与操作人来自登录上下文。 */
@Data
public class RolePermissionGrantRequest {
    @NotNull @Positive
    private Long roleId;
    @NotNull @Positive
    private Long permissionId;
}
