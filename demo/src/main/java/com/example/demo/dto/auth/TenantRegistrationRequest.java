package com.example.demo.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 公开企业注册时允许提交的最小资料。 */
public record TenantRegistrationRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_-]{1,31}$", message = "企业编码需以字母开头，只能包含字母、数字、下划线和连字符") String tenantCode,
        @NotBlank @Size(max = 100) String tenantName,
        @NotBlank @Size(max = 64) String adminUsername,
        @NotBlank @Size(min = 8, max = 72) String adminPassword,
        @NotBlank @Size(max = 64) String adminDisplayName,
        @Email @Size(max = 128) String adminEmail,
        @Size(max = 32) String adminPhone
) {
}
