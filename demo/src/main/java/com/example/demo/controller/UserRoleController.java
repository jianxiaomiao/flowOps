package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.dto.authorization.UserRoleGrantRequest;
import com.example.demo.entity.UserRole;
import com.example.demo.service.UserRoleService;
import com.example.demo.security.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import java.util.List;

@Validated
@RestController
@RequestMapping("/user-role")
@PreAuthorize("hasAuthority('role:manage')")
@RequiredArgsConstructor
/**
 * 用户与角色之间的授权关系接口。
 */
public class UserRoleController {
    private final UserRoleService userRoleService;
    private final TenantContext tenantContext;

    /** @param request 用户和角色 ID。@return 新建授权关系。 */
    @PostMapping
    @PreAuthorize("hasAuthority('role:manage')")
    public Result<UserRole> grant(@Valid @RequestBody UserRoleGrantRequest request) {
        return Result.success(userRoleService.grant(tenantContext.tenantId(), request.getUserId(), request.getRoleId(), tenantContext.userId()));
    }

    /** @param userId 用户 ID。@param roleId 角色 ID。@return 空的成功响应。 */
    @DeleteMapping
    public Result<Void> revoke(@RequestParam @Positive Long userId, @RequestParam @Positive Long roleId) {
        userRoleService.revoke(tenantContext.tenantId(), userId, roleId);
        return Result.success();
    }

    /** @param userId 用户 ID。@return 用户拥有的角色关系列表。 */
    @GetMapping
    public Result<List<UserRole>> list(@RequestParam @Positive Long userId) {
        return Result.success(userRoleService.listByUser(tenantContext.tenantId(), userId));
    }
}
