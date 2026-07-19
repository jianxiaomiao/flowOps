package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.example.demo.common.BusinessException;
import com.example.demo.dto.permission.PermissionPageQuery;
import com.example.demo.entity.Permission;
import com.example.demo.mapper.PermissionMapper;
import com.example.demo.service.PermissionService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 权限由平台初始化维护，本服务只提供查询和内部校验。 */
@Service
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission> implements PermissionService {
    private static final String STATUS_ACTIVE = "ACTIVE";

    @Override
    public Permission getActivePermission(Long permissionId) {
        if (permissionId == null || permissionId <= 0) {
            throw new BusinessException(10001, "permissionId 必须是正整数");
        }
        Permission permission = getById(permissionId);
        if (permission == null || !STATUS_ACTIVE.equals(permission.getStatus())) {
            throw new BusinessException(20001, "权限不存在或已停用");
        }
        return permission;
    }

    @Override
    public Page<Permission> pagePermissions(PermissionPageQuery query) {
        return page(new Page<>(query.getPageNum(), query.getPageSize()),
                Wrappers.lambdaQuery(Permission.class)
                        .eq(StringUtils.hasText(query.getResource()), Permission::getResource, query.getResource())
                        .eq(StringUtils.hasText(query.getAction()), Permission::getAction, query.getAction())
                        .eq(StringUtils.hasText(query.getStatus()), Permission::getStatus, query.getStatus())
                        .orderByAsc(Permission::getPermissionCode));
    }
}
