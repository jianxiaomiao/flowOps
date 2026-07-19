package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.dto.authorization.RolePermissionGrantRequest;
import com.example.demo.entity.RolePermission;
import com.example.demo.service.RolePermissionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@Validated
@RestController
@RequestMapping("/role-permission")
@RequiredArgsConstructor
public class RolePermissionController {
    private final RolePermissionService rolePermissionService;

    @PostMapping
    public Result<RolePermission> grant(@Valid @RequestBody RolePermissionGrantRequest request) {
        return Result.success(rolePermissionService.grant(request.getTenantId(), request.getRoleId(), request.getPermissionId(), request.getOperatorId()));
    }

    @DeleteMapping
    public Result<Void> revoke(@RequestParam @Positive Long tenantId, @RequestParam @Positive Long roleId, @RequestParam @Positive Long permissionId) {
        rolePermissionService.revoke(tenantId, roleId, permissionId);
        return Result.success();
    }

    @GetMapping
    public Result<List<RolePermission>> list(@RequestParam @Positive Long tenantId, @RequestParam @Positive Long roleId) {
        return Result.success(rolePermissionService.listByRole(tenantId, roleId));
    }
}
