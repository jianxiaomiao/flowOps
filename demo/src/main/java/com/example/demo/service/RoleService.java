package com.example.demo.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.example.demo.dto.role.RoleCreateRequest;
import com.example.demo.dto.role.RolePageQuery;
import com.example.demo.dto.role.RoleUpdateRequest;
import com.example.demo.entity.Role;

/** 角色用例服务。 */
public interface RoleService extends IService<Role> {

    Role createRole(RoleCreateRequest request);

    Role updateRole(Long tenantId, RoleUpdateRequest request);

    Role getRole(Long tenantId, Long roleId);

    Page<Role> pageRoles(RolePageQuery query);

    void disableRole(Long tenantId, Long roleId);
}
