package com.example.demo.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.example.demo.dto.sla.SlaPolicyCreateRequest;
import com.example.demo.dto.sla.SlaPolicyPageQuery;
import com.example.demo.dto.sla.SlaPolicyUpdateRequest;
import com.example.demo.entity.SlaPolicy;

public interface SlaPolicyService extends IService<SlaPolicy> {
    SlaPolicy createPolicy(SlaPolicyCreateRequest request);
    SlaPolicy updatePolicy(Long tenantId, SlaPolicyUpdateRequest request);
    SlaPolicy getPolicy(Long tenantId, Long policyId);
    SlaPolicy getEnabledPolicy(Long tenantId, String priority);
    Page<SlaPolicy> pagePolicies(SlaPolicyPageQuery query);
}
