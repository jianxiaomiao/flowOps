package com.example.demo.dto.authorization;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/** 给用户授予角色；operatorId 在接入认证后将由登录上下文替代。 */
@Data
public class UserRoleGrantRequest {
    @NotNull @Positive
    private Long tenantId;
    @NotNull @Positive
    private Long userId;
    @NotNull @Positive
    private Long roleId;
    @Positive
    private Long operatorId;
}
