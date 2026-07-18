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
import com.example.demo.vo.RoleResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

/** 角色接口。 */
@RestController
@RequestMapping("/role")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @PostMapping
    public Result<RoleResponse> create(@Valid @RequestBody RoleCreateRequest request) {
        Role role = roleService.createRole(request);
        return Result.success(RoleConverter.toResponse(role));
    }

    @PutMapping
    public Result<RoleResponse> update(
            @RequestParam Long tenantId,
            @Valid @RequestBody RoleUpdateRequest request
    ) {
        Role role = roleService.updateRole(tenantId, request);
        return Result.success(RoleConverter.toResponse(role));
    }

    @GetMapping("/{id}")
    public Result<RoleResponse> getOne(
            @PathVariable Long id,
            @RequestParam Long tenantId
    ) {
        Role role = roleService.getRole(tenantId, id);
        return Result.success(RoleConverter.toResponse(role));
    }

    @GetMapping("/page")
    public Result<IPage<RoleResponse>> findPage(@Valid @ModelAttribute RolePageQuery query) {
        Page<Role> rolePage = roleService.pageRoles(query);
        return Result.success(rolePage.convert(RoleConverter::toResponse));
    }

    @DeleteMapping("/{id}")
    public Result<Void> disable(
            @PathVariable Long id,
            @RequestParam Long tenantId
    ) {
        roleService.disableRole(tenantId, id);
        return Result.success();
    }
}
