package com.example.demo.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.example.demo.entity.RolePermission;
import java.util.List;

public interface RolePermissionService extends IService<RolePermission> {
    RolePermission grant(Long tenantId, Long roleId, Long permissionId, Long operatorId);
    void revoke(Long tenantId, Long roleId, Long permissionId);
    List<RolePermission> listByRole(Long tenantId, Long roleId);
}
