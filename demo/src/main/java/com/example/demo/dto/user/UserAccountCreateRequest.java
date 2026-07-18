package com.example.demo.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 创建用户账号时允许客户端提交的数据。 */
@Data
public class UserAccountCreateRequest {

    /** 当前未接入登录上下文，第一版暂由请求传入。 */
    @NotNull(message = "tenantId 不能为空")
    private Long tenantId;

    @NotNull(message = "departmentId 不能为空")
    private Long departmentId;

    @NotBlank(message = "username 不能为空")
    @Size(max = 64, message = "username 长度不能超过 64")
    private String username;

    /** 只用于本次创建；Service 会将其转换为 passwordHash，绝不直接持久化。 */
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 72, message = "密码长度必须在 8 到 72 之间")
    private String rawPassword;

    @NotBlank(message = "displayName 不能为空")
    @Size(max = 64, message = "displayName 长度不能超过 64")
    private String displayName;

    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度不能超过 128")
    private String email;

    @Size(max = 32, message = "手机号长度不能超过 32")
    private String phone;
}
