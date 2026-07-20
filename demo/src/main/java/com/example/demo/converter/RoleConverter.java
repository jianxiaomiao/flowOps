package com.example.demo.converter;

import com.example.demo.dto.role.RoleCreateRequest;
import com.example.demo.dto.role.RoleUpdateRequest;
import com.example.demo.entity.Role;
import com.example.demo.vo.RoleResponse;

/** Role 模块的数据转换器，不处理状态或内置角色等业务规则。 */
public final class RoleConverter {

    private RoleConverter() {
    }

    public static Role toEntity(RoleCreateRequest request) {
        Role role = new Role();
        role.setRoleCode(request.getRoleCode());
        role.setRoleName(request.getRoleName());
        role.setDescription(request.getDescription());
        return role;
    }

    public static void copyToEntity(RoleUpdateRequest request, Role role) {
        role.setRoleName(request.getRoleName());
        role.setDescription(request.getDescription());
    }

    public static RoleResponse toResponse(Role role) {
        RoleResponse response = new RoleResponse();
        response.setId(role.getId());
        response.setTenantId(role.getTenantId());
        response.setRoleCode(role.getRoleCode());
        response.setRoleName(role.getRoleName());
        response.setDescription(role.getDescription());
        response.setBuiltIn(role.getBuiltIn());
        response.setStatus(role.getStatus());
        response.setCreatedAt(role.getCreatedAt());
        response.setUpdatedAt(role.getUpdatedAt());
        return response;
    }
}
