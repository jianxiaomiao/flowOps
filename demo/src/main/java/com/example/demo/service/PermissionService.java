package com.example.demo.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.example.demo.dto.permission.PermissionPageQuery;
import com.example.demo.entity.Permission;

public interface PermissionService extends IService<Permission> {
    Permission getActivePermission(Long permissionId);
    Page<Permission> pagePermissions(PermissionPageQuery query);
}
