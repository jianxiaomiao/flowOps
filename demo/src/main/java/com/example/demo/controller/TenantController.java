package com.example.demo.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.Result;
import com.example.demo.converter.TenantConverter;
import com.example.demo.dto.TenantCreateRequest;
import com.example.demo.dto.TenantUpdateRequest;
import com.example.demo.entity.Tenant;
import com.example.demo.service.TenantService;
import com.example.demo.vo.TenantResponse;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/tenant")
public class TenantController {
    @Resource
    private TenantService tenantService;

    /**
     * 新增租户
     * DTO 只接收客户端允许填写的字段；状态和审计字段由后端设置。
     */
    @PostMapping
    public Result<TenantResponse> save(@RequestBody TenantCreateRequest request) {
        Tenant tenant = TenantConverter.toEntity(request);
        tenant.setStatus("ACTIVE");
        tenantService.save(tenant);
        return Result.success(TenantConverter.toResponse(tenant));
    }

    /**
     * 修改租户
     * tenantCode 和 status 不在本接口中修改，避免普通资料修改影响租户身份或启停状态。
     */
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

    /**
     * 获取单个租户
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public Result<TenantResponse> getOne(@PathVariable Long id){
        Tenant tenant = tenantService.getById(id);
        if (tenant == null) {
            return Result.failure(20001, "租户不存在");
        }
        return Result.success(TenantConverter.toResponse(tenant));
    }

    /**
     * 获取全部租户
     * @return
     */
    @GetMapping
    public Result<List<TenantResponse>> list() {
        List<TenantResponse> responseList = tenantService.list().stream()
                .map(TenantConverter::toResponse)
                .toList();
        return Result.success(responseList);
    }

    /**
     * 分页获取租户
     * @param pageNum
     * @param pageSize
     * @return
     */
    @GetMapping("/page")
    public Result<Page<TenantResponse>> findPage(@RequestParam(defaultValue = "1") Integer pageNum, @RequestParam(defaultValue = "10") Integer pageSize){
        Page<Tenant> tenantPage = tenantService.page(new Page<>(pageNum, pageSize));
        Page<TenantResponse> responsePage = (Page<TenantResponse>) tenantPage.convert(TenantConverter::toResponse);
        return Result.success(responsePage);
    }

    /**
     * 软删除租户
     * @param id
     * @return
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id){
        boolean updated = tenantService.lambdaUpdate()
                .eq(Tenant::getId, id)
                .set(Tenant::getStatus, "DISABLED")
                .update();
        return Result.success();
    }
}
