package com.example.demo.service;

import com.example.demo.dto.auth.TenantRegistrationRequest;
import com.example.demo.vo.TenantRegistrationResponse;

/** 新企业和首位管理员的原子初始化用例。 */
public interface OnboardingService {
    TenantRegistrationResponse registerTenant(TenantRegistrationRequest request);
}
