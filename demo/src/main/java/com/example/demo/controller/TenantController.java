package com.example.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.Result;
import com.example.demo.converter.TenantConverter;
import com.example.demo.dto.tenant.TenantCreateRequest;
import com.example.demo.dto.tenant.TenantPageQuery;
import com.example.demo.dto.tenant.TenantUpdateRequest;
import com.example.demo.entity.Tenant;
import com.example.demo.service.TenantService;
import com.example.demo.vo.TenantResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tenant")
@PreAuthorize("hasAuthority('platform:tenant:manage')")
@RequiredArgsConstructor
/**
 * 平台级租户管理接口。
 *
 * <p>该接口需要平台权限，不属于普通租户管理员可访问的业务范围。</p>
 */
public class TenantController {
    private final TenantService tenantService;

    /** @param request 客户端可填写的租户资料。@return 新建且默认启用的租户。 */
    @PostMapping
    public Result<TenantResponse> save(@RequestBody TenantCreateRequest request) {
        Tenant tenant = TenantConverter.toEntity(request);
        tenant.setStatus("ACTIVE");
        tenantService.save(tenant);
        return Result.success(TenantConverter.toResponse(tenant));
    }

    /** @param request 租户 ID 和允许修改的资料。@return 修改后的租户。 */
    @PutMapping
    public Result<TenantResponse> update(@RequestBody TenantUpdateRequest request){
        Tenant tenant = tenantService.getById(request.getId());
        if (tenant == null) {
            return Result.failure(20001, "租户不存在");
        }
        TenantConverter.copyToEntity(request, tenant);
        tenantService.updateById(tenant);
        return Result.success(TenantConverter.toResponse(tenant));
    }

    /** @param id 租户 ID。@return 租户详情。 */
    @GetMapping("/{id}")
    public Result<TenantResponse> getOne(@PathVariable Long id){
        Tenant tenant = tenantService.getById(id);
        if (tenant == null) {
            return Result.failure(20001, "租户不存在");
        }
        return Result.success(TenantConverter.toResponse(tenant));
    }

    /** @return 平台可见的全部租户。 */
    @GetMapping
    public Result<List<TenantResponse>> list() {
        List<TenantResponse> responseList = tenantService.list().stream()
                .map(TenantConverter::toResponse)
                .toList();
        return Result.success(responseList);
    }


    /** @param query 分页及租户编码、名称、状态筛选条件。@return 租户分页。 */
    @GetMapping("/page")
    public Result<Page<TenantResponse>> findPage(@ModelAttribute TenantPageQuery query){
        LambdaQueryWrapper<Tenant> wrapper = Wrappers.lambdaQuery(Tenant.class)
                .eq(StringUtils.hasText(query.getTenantCode()),
                        Tenant::getTenantCode, query.getTenantCode())
                .like(StringUtils.hasText(query.getTenantName()),
                        Tenant::getTenantName, query.getTenantName())
                .eq(StringUtils.hasText(query.getStatus()),
                        Tenant::getStatus, query.getStatus())
                .orderByDesc(Tenant::getCreatedAt);
        Page<Tenant> tenantPage = tenantService.page(
                new Page<>(query.getPageNum(), query.getPageSize()),
                wrapper
        );
        Page<TenantResponse> responsePage =
                (Page<TenantResponse>) tenantPage.convert(TenantConverter::toResponse);
        return  Result.success(responsePage);
    }

    /**
     * 停用租户而非物理删除，保留其历史数据。
     *
     * @param id 要停用的租户 ID。
     * @return 空的成功响应。
     */
    @DeleteMapping("/{id}")
    public Result<Void> disable(@PathVariable Long id){
        boolean updated = tenantService.lambdaUpdate()
                .eq(Tenant::getId, id)
                .set(Tenant::getStatus, "DISABLED")
                .update();
        return Result.success();
    }

    /** @param id 要重新启用的租户 ID。@return 空的成功响应。 */
    @PutMapping("/{id}/enable")
    public Result<Void> enable(@PathVariable Long id) {
        tenantService.lambdaUpdate()
                .eq(Tenant::getId, id)
                .set(Tenant::getStatus, "ACTIVE")
                .update();
        return Result.success();
    }
}
