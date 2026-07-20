package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.example.demo.common.BusinessException;
import com.example.demo.audit.AuditOperation;
import com.example.demo.converter.SlaPolicyConverter;
import com.example.demo.dto.sla.SlaPolicyCreateRequest;
import com.example.demo.dto.sla.SlaPolicyPageQuery;
import com.example.demo.dto.sla.SlaPolicyUpdateRequest;
import com.example.demo.entity.SlaPolicy;
import com.example.demo.mapper.SlaPolicyMapper;
import com.example.demo.service.SlaPolicyService;
import com.example.demo.security.TenantContext;
import com.example.demo.validator.TenantScopeValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.util.Locale;

@Service
/**
 * 租户 SLA 策略管理实现。
 *
 * <p>策略只影响新建工单；工单创建时已保存截止时间快照，后续修改策略不会篡改历史承诺。</p>
 */
public class SlaPolicyServiceImpl extends ServiceImpl<SlaPolicyMapper, SlaPolicy> implements SlaPolicyService {
    private final TenantScopeValidator tenantScopeValidator;
    private final TenantContext tenantContext;
    public SlaPolicyServiceImpl(TenantScopeValidator tenantScopeValidator, TenantContext tenantContext) { this.tenantScopeValidator = tenantScopeValidator; this.tenantContext = tenantContext; }

    @Override @Transactional
    @AuditOperation(module = "SLA", operation = "CREATE_POLICY", resourceType = "SLA_POLICY")
    /** @param request 优先级和响应、解决分钟数。@return 新建启用策略。@throws BusinessException 当优先级重复或分钟数非法时抛出。 */
    public SlaPolicy createPolicy(SlaPolicyCreateRequest request) {
        Long tenantId = tenantContext.tenantId();
        tenantScopeValidator.requireActiveTenant(tenantId);
        String priority = normalizePriority(request.getPriority());
        validateMinutes(request.getResponseMinutes(), request.getResolutionMinutes());
        if (lambdaQuery().eq(SlaPolicy::getTenantId, tenantId).eq(SlaPolicy::getPriority, priority).exists()) {
            throw new BusinessException(30001, "该租户已配置此优先级的 SLA 策略");
        }
        SlaPolicy policy = SlaPolicyConverter.toEntity(request);
        policy.setTenantId(tenantId); policy.setPriority(priority); policy.setEnabled(true); policy.setCreatedBy(tenantContext.userId()); policy.setUpdatedBy(tenantContext.userId());
        if (!save(policy)) throw new IllegalStateException("创建 SLA 策略失败");
        return policy;
    }

    @Override @Transactional
    @AuditOperation(module = "SLA", operation = "UPDATE_POLICY", resourceType = "SLA_POLICY")
    /** @param tenantId 当前租户 ID。@param request 更新资料和版本。@return 更新后的策略。@throws BusinessException 当版本冲突或分钟数非法时抛出。 */
    public SlaPolicy updatePolicy(Long tenantId, SlaPolicyUpdateRequest request) {
        SlaPolicy policy = getPolicy(tenantId, request.getId());
        if (request.getResponseMinutes() != null || request.getResolutionMinutes() != null) {
            validateMinutes(request.getResponseMinutes() == null ? policy.getResponseMinutes() : request.getResponseMinutes(),
                    request.getResolutionMinutes() == null ? policy.getResolutionMinutes() : request.getResolutionMinutes());
        }
        SlaPolicyConverter.copyToEntity(request, policy);
        policy.setUpdatedBy(tenantContext.userId());
        if (!updateById(policy)) throw new BusinessException(30002, "SLA 策略已被其他操作修改，请刷新后重试");
        return policy;
    }

    @Override
    /** @param tenantId 当前租户 ID。@param policyId 策略 ID。@return 策略详情。 */
    public SlaPolicy getPolicy(Long tenantId, Long policyId) {
        SlaPolicy policy = lambdaQuery().eq(SlaPolicy::getTenantId, requirePositive(tenantId, "tenantId"))
                .eq(SlaPolicy::getId, requirePositive(policyId, "policyId")).one();
        if (policy == null) throw new BusinessException(20001, "SLA 策略不存在");
        return policy;
    }

    @Override
    /** @param tenantId 当前租户 ID。@param priority 工单优先级。@return 对应的启用 SLA 策略。@throws BusinessException 当策略不存在或未启用时抛出。 */
    public SlaPolicy getEnabledPolicy(Long tenantId, String priority) {
        SlaPolicy policy = lambdaQuery().eq(SlaPolicy::getTenantId, requirePositive(tenantId, "tenantId"))
                .eq(SlaPolicy::getPriority, normalizePriority(priority)).eq(SlaPolicy::getEnabled, true).one();
        if (policy == null) throw new BusinessException(20001, "当前优先级没有启用的 SLA 策略");
        return policy;
    }

    @Override
    /** @param query 分页及优先级、启用状态筛选条件。@return 当前租户策略分页。 */
    public Page<SlaPolicy> pagePolicies(SlaPolicyPageQuery query) {
        return page(new Page<>(query.getPageNum(), query.getPageSize()), Wrappers.lambdaQuery(SlaPolicy.class)
                .eq(SlaPolicy::getTenantId, tenantContext.tenantId())
                .eq(StringUtils.hasText(query.getPriority()), SlaPolicy::getPriority, query.getPriority())
                .eq(query.getEnabled() != null, SlaPolicy::getEnabled, query.getEnabled()).orderByDesc(SlaPolicy::getCreatedAt));
    }

    /** @param priority 原优先级。@return 标准 P0/P1/P2。@throws BusinessException 当优先级不支持时抛出。 */
    private String normalizePriority(String priority) {
        if (!StringUtils.hasText(priority)) throw new BusinessException(10001, "priority 不能为空");
        String normalized = priority.trim().toUpperCase(Locale.ROOT);
        if (!("P0".equals(normalized) || "P1".equals(normalized) || "P2".equals(normalized))) throw new BusinessException(10001, "priority 仅支持 P0、P1、P2");
        return normalized;
    }
    /** @param response 响应分钟数。@param resolution 解决分钟数。@throws BusinessException 当时限不为正或响应晚于解决时限时抛出。 */
    private void validateMinutes(Integer response, Integer resolution) {
        if (response == null || resolution == null || response <= 0 || resolution <= 0 || response > resolution) throw new BusinessException(10001, "响应和解决时限必须为正数，且响应时限不能晚于解决时限");
    }
    /** @param value 待校验 ID。@param field 字段名。@return 原 ID。@throws BusinessException 当 ID 非正数时抛出。 */
    private Long requirePositive(Long value, String field) { if (value == null || value <= 0) throw new BusinessException(10001, field + " 必须是正整数"); return value; }
}
