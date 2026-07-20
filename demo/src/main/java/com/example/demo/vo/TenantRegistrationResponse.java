package com.example.demo.vo;

/** 企业注册完成后返回的非敏感初始化结果。 */
public record TenantRegistrationResponse(
        Long tenantId,
        String tenantCode,
        Long adminUserId,
        String adminUsername
) {
}
