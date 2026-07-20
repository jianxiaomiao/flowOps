package com.example.demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.common.Result;
import com.example.demo.converter.SlaPolicyConverter;
import com.example.demo.dto.sla.SlaPolicyCreateRequest;
import com.example.demo.dto.sla.SlaPolicyPageQuery;
import com.example.demo.dto.sla.SlaPolicyUpdateRequest;
import com.example.demo.service.SlaPolicyService;
import com.example.demo.security.TenantContext;
import com.example.demo.vo.SlaPolicyResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/sla-policy")
@PreAuthorize("hasAuthority('sla:manage')")
@RequiredArgsConstructor
/**
 * 当前租户的 SLA 响应和解决时限策略接口。
 */
public class SlaPolicyController {
    private final SlaPolicyService slaPolicyService;
    private final TenantContext tenantContext;

    /** @param request 优先级及响应、解决分钟数。@return 新建 SLA 策略。 */
    @PostMapping
    @PreAuthorize("hasAuthority('sla:manage')")
    public Result<SlaPolicyResponse> create(@Valid @RequestBody SlaPolicyCreateRequest request) {
        return Result.success(SlaPolicyConverter.toResponse(slaPolicyService.createPolicy(request)));
    }
    /** @param request 待修改策略的数据和版本。@return 修改后的 SLA 策略。 */
    @PutMapping
    public Result<SlaPolicyResponse> update(@Valid @RequestBody SlaPolicyUpdateRequest request) {
        return Result.success(SlaPolicyConverter.toResponse(slaPolicyService.updatePolicy(tenantContext.tenantId(), request)));
    }
    /** @param id SLA 策略 ID。@return 策略详情。 */
    @GetMapping("/{id}")
    public Result<SlaPolicyResponse> get(@PathVariable @Positive Long id) {
        return Result.success(SlaPolicyConverter.toResponse(slaPolicyService.getPolicy(tenantContext.tenantId(), id)));
    }
    /** @param query 分页和筛选条件。@return SLA 策略分页。 */
    @GetMapping("/page")
    public Result<IPage<SlaPolicyResponse>> page(@Valid @ModelAttribute SlaPolicyPageQuery query) {
        return Result.success(slaPolicyService.pagePolicies(query).convert(SlaPolicyConverter::toResponse));
    }
}
