package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.dto.authorization.UserRoleGrantRequest;
import com.example.demo.entity.UserRole;
import com.example.demo.service.UserRoleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class UserRoleController {
    private final UserRoleService userRoleService;

    @PostMapping
    public Result<UserRole> grant(@Valid @RequestBody UserRoleGrantRequest request) {
        return Result.success(userRoleService.grant(request.getTenantId(), request.getUserId(), request.getRoleId(), request.getOperatorId()));
    }

    @DeleteMapping
    public Result<Void> revoke(@RequestParam @Positive Long tenantId, @RequestParam @Positive Long userId, @RequestParam @Positive Long roleId) {
        userRoleService.revoke(tenantId, userId, roleId);
        return Result.success();
    }

    @GetMapping
    public Result<List<UserRole>> list(@RequestParam @Positive Long tenantId, @RequestParam @Positive Long userId) {
        return Result.success(userRoleService.listByUser(tenantId, userId));
    }
}
