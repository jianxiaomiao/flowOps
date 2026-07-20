package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.dto.authorization.RolePermissionGrantRequest;
import com.example.demo.entity.RolePermission;
import com.example.demo.service.RolePermissionService;
import com.example.demo.security.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
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
@PreAuthorize("hasAuthority('role:manage')")
@RequiredArgsConstructor
/**
 * 角色与权限之间的授权关系接口。
 */
public class RolePermissionController {
    private final RolePermissionService rolePermissionService;
    private final TenantContext tenantContext;

    /** @param request 角色和权限 ID。@return 新建授权关系。 */
    @PostMapping
    @PreAuthorize("hasAuthority('role:manage')")
    public Result<RolePermission> grant(@Valid @RequestBody RolePermissionGrantRequest request) {
        return Result.success(rolePermissionService.grant(tenantContext.tenantId(), request.getRoleId(), request.getPermissionId(), tenantContext.userId()));
    }

    /** @param roleId 角色 ID。@param permissionId 权限 ID。@return 空的成功响应。 */
    @DeleteMapping
    public Result<Void> revoke(@RequestParam @Positive Long roleId, @RequestParam @Positive Long permissionId) {
        rolePermissionService.revoke(tenantContext.tenantId(), roleId, permissionId);
        return Result.success();
    }

    /** @param roleId 角色 ID。@return 该角色的授权关系列表。 */
    @GetMapping
    public Result<List<RolePermission>> list(@RequestParam @Positive Long roleId) {
        return Result.success(rolePermissionService.listByRole(tenantContext.tenantId(), roleId));
    }
}
