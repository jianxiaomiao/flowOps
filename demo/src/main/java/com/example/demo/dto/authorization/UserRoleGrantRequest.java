package com.example.demo.dto.authorization;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/** 给当前租户的用户授予角色；租户与操作人来自登录上下文。 */
@Data
public class UserRoleGrantRequest {
    @NotNull @Positive
    private Long userId;
    @NotNull @Positive
    private Long roleId;
}
