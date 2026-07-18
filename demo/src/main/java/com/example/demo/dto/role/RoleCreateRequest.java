package com.example.demo.dto.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 创建自定义角色时允许客户端提交的数据。 */
@Data
public class RoleCreateRequest {

    /** 当前未接入认证上下文，第一版暂由请求传入。 */
    @NotNull(message = "tenantId 不能为空")
    private Long tenantId;

    @NotBlank(message = "roleCode 不能为空")
    @Size(max = 64, message = "roleCode 长度不能超过 64")
    private String roleCode;

    @NotBlank(message = "roleName 不能为空")
    @Size(max = 64, message = "roleName 长度不能超过 64")
    private String roleName;

    @Size(max = 255, message = "description 长度不能超过 255")
    private String description;
}
