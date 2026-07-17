package com.example.demo.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.Result;
import com.example.demo.entity.Tenant;
import com.example.demo.service.TenantService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tenant")
public class TenantController {
    @Resource
    private TenantService tenantService;

    /**
     * 新增租户
     * @param tenant
     * @return
     */
    @PostMapping
    public Result<Long> save(@RequestBody Tenant tenant) {
        tenantService.save(tenant);
        return Result.success(tenant.getId());
    }

    /**
     * 修改租户
     * @param tenant
     * @return
     */
    @PutMapping
    public Result<Long> update(@RequestBody Tenant tenant){
        tenantService.updateById(tenant);
        return Result.success(tenant.getId());
    }

    /**
     * 获取单个租户
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public Result<Tenant> getOne(@PathVariable Long id){
        return  Result.success(tenantService.getById(id));
    }

    /**
     * 获取全部租户
     * @return
     */
    @GetMapping
    public Result<List<Tenant>> list() {
        return Result.success(tenantService.list());
    }

    /**
     * 分页获取租户
     * @param pageNum
     * @param pageSize
     * @return
     */
    @GetMapping("/page")
    public Result<Page<Tenant>> findPage(@RequestParam(defaultValue = "1") Integer pageNum, @RequestParam(defaultValue = "10") Integer pageSize){
        return Result.success(
                tenantService.page(new Page<>(pageNum, pageSize))
        );
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
