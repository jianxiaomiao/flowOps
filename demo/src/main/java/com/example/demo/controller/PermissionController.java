package com.example.demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.common.Result;
import com.example.demo.converter.PermissionConverter;
import com.example.demo.dto.permission.PermissionPageQuery;
import com.example.demo.service.PermissionService;
import com.example.demo.vo.PermissionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 平台权限字典查询接口；创建和修改由初始化迁移脚本负责。
 */
@RestController
@RequestMapping("/permission")
@RequiredArgsConstructor
public class PermissionController {
    private final PermissionService permissionService;

    /** @param query 分页和权限编码筛选条件。@return 平台权限字典分页。 */
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('role:manage')")
    public Result<IPage<PermissionResponse>> page(@Valid @ModelAttribute PermissionPageQuery query) {
        return Result.success(permissionService.pagePermissions(query).convert(PermissionConverter::toResponse));
    }
}
