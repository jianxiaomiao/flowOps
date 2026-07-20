package com.example.demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.Result;
import com.example.demo.converter.RoleConverter;
import com.example.demo.dto.role.RoleCreateRequest;
import com.example.demo.dto.role.RolePageQuery;
import com.example.demo.dto.role.RoleUpdateRequest;
import com.example.demo.entity.Role;
import com.example.demo.service.RoleService;
import com.example.demo.security.TenantContext;
import com.example.demo.vo.RoleResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 当前租户的角色管理接口。
 */
@RestController
@RequestMapping("/role")
@PreAuthorize("hasAuthority('role:manage')")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;
    private final TenantContext tenantContext;

    /** @param request 新角色的编码、名称和说明。@return 新建角色。 */
    @PostMapping
    @PreAuthorize("hasAuthority('role:manage')")
    public Result<RoleResponse> create(@Valid @RequestBody RoleCreateRequest request) {
        Role role = roleService.createRole(request);
        return Result.success(RoleConverter.toResponse(role));
    }

    /** @param request 待修改角色的资料和乐观锁版本。@return 修改后的角色。 */
    @PutMapping
    public Result<RoleResponse> update(
            @Valid @RequestBody RoleUpdateRequest request
    ) {
        Role role = roleService.updateRole(tenantContext.tenantId(), request);
        return Result.success(RoleConverter.toResponse(role));
    }

    /** @param id 角色 ID。@return 当前租户中的角色详情。 */
    @GetMapping("/{id}")
    public Result<RoleResponse> getOne(
            @PathVariable Long id
    ) {
        Role role = roleService.getRole(tenantContext.tenantId(), id);
        return Result.success(RoleConverter.toResponse(role));
    }

    /** @param query 分页和角色筛选条件。@return 角色分页。 */
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('role:manage')")
    public Result<IPage<RoleResponse>> findPage(@Valid @ModelAttribute RolePageQuery query) {
        Page<Role> rolePage = roleService.pageRoles(query);
        return Result.success(rolePage.convert(RoleConverter::toResponse));
    }

    /** @param id 要停用的非内置角色 ID。@return 空的成功响应。 */
    @DeleteMapping("/{id}")
    public Result<Void> disable(
            @PathVariable Long id
    ) {
        roleService.disableRole(tenantContext.tenantId(), id);
        return Result.success();
    }

    /** @param id 要重新启用的非内置角色 ID。@return 空的成功响应。 */
    @PutMapping("/{id}/enable")
    public Result<Void> enable(@PathVariable Long id) {
        roleService.enableRole(tenantContext.tenantId(), id);
        return Result.success();
    }
}
