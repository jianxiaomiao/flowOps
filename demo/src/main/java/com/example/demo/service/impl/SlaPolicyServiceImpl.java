package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.example.demo.common.BusinessException;
import com.example.demo.converter.SlaPolicyConverter;
import com.example.demo.dto.sla.SlaPolicyCreateRequest;
import com.example.demo.dto.sla.SlaPolicyPageQuery;
import com.example.demo.dto.sla.SlaPolicyUpdateRequest;
import com.example.demo.entity.SlaPolicy;
import com.example.demo.mapper.SlaPolicyMapper;
import com.example.demo.service.SlaPolicyService;
import com.example.demo.validator.TenantScopeValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.util.Locale;

@Service
public class SlaPolicyServiceImpl extends ServiceImpl<SlaPolicyMapper, SlaPolicy> implements SlaPolicyService {
    private final TenantScopeValidator tenantScopeValidator;
    public SlaPolicyServiceImpl(TenantScopeValidator tenantScopeValidator) { this.tenantScopeValidator = tenantScopeValidator; }

    @Override @Transactional
    public SlaPolicy createPolicy(SlaPolicyCreateRequest request) {
        Long tenantId = requirePositive(request.getTenantId(), "tenantId");
        tenantScopeValidator.requireActiveTenant(tenantId);
        String priority = normalizePriority(request.getPriority());
        validateMinutes(request.getResponseMinutes(), request.getResolutionMinutes());
        if (lambdaQuery().eq(SlaPolicy::getTenantId, tenantId).eq(SlaPolicy::getPriority, priority).exists()) {
            throw new BusinessException(30001, "该租户已配置此优先级的 SLA 策略");
        }
        SlaPolicy policy = SlaPolicyConverter.toEntity(request);
        policy.setPriority(priority); policy.setEnabled(true); policy.setCreatedBy(request.getOperatorId()); policy.setUpdatedBy(request.getOperatorId());
        if (!save(policy)) throw new IllegalStateException("创建 SLA 策略失败");
        return policy;
    }

    @Override @Transactional
    public SlaPolicy updatePolicy(Long tenantId, SlaPolicyUpdateRequest request) {
        SlaPolicy policy = getPolicy(tenantId, request.getId());
        if (request.getResponseMinutes() != null || request.getResolutionMinutes() != null) {
            validateMinutes(request.getResponseMinutes() == null ? policy.getResponseMinutes() : request.getResponseMinutes(),
                    request.getResolutionMinutes() == null ? policy.getResolutionMinutes() : request.getResolutionMinutes());
        }
        SlaPolicyConverter.copyToEntity(request, policy);
        policy.setUpdatedBy(request.getOperatorId());
        if (!updateById(policy)) throw new BusinessException(30002, "SLA 策略已被其他操作修改，请刷新后重试");
        return policy;
    }

    @Override
    public SlaPolicy getPolicy(Long tenantId, Long policyId) {
        SlaPolicy policy = lambdaQuery().eq(SlaPolicy::getTenantId, requirePositive(tenantId, "tenantId"))
                .eq(SlaPolicy::getId, requirePositive(policyId, "policyId")).one();
        if (policy == null) throw new BusinessException(20001, "SLA 策略不存在");
        return policy;
    }

    @Override
    public SlaPolicy getEnabledPolicy(Long tenantId, String priority) {
        SlaPolicy policy = lambdaQuery().eq(SlaPolicy::getTenantId, requirePositive(tenantId, "tenantId"))
                .eq(SlaPolicy::getPriority, normalizePriority(priority)).eq(SlaPolicy::getEnabled, true).one();
        if (policy == null) throw new BusinessException(20001, "当前优先级没有启用的 SLA 策略");
        return policy;
    }

    @Override
    public Page<SlaPolicy> pagePolicies(SlaPolicyPageQuery query) {
        return page(new Page<>(query.getPageNum(), query.getPageSize()), Wrappers.lambdaQuery(SlaPolicy.class)
                .eq(SlaPolicy::getTenantId, requirePositive(query.getTenantId(), "tenantId"))
                .eq(StringUtils.hasText(query.getPriority()), SlaPolicy::getPriority, query.getPriority())
                .eq(query.getEnabled() != null, SlaPolicy::getEnabled, query.getEnabled()).orderByDesc(SlaPolicy::getCreatedAt));
    }

    private String normalizePriority(String priority) {
        if (!StringUtils.hasText(priority)) throw new BusinessException(10001, "priority 不能为空");
        String normalized = priority.trim().toUpperCase(Locale.ROOT);
        if (!("P0".equals(normalized) || "P1".equals(normalized) || "P2".equals(normalized))) throw new BusinessException(10001, "priority 仅支持 P0、P1、P2");
        return normalized;
    }
    private void validateMinutes(Integer response, Integer resolution) {
        if (response == null || resolution == null || response <= 0 || resolution <= 0 || response > resolution) throw new BusinessException(10001, "响应和解决时限必须为正数，且响应时限不能晚于解决时限");
    }
    private Long requirePositive(Long value, String field) { if (value == null || value <= 0) throw new BusinessException(10001, field + " 必须是正整数"); return value; }
}
