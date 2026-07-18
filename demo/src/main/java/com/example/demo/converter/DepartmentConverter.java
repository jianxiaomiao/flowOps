package com.example.demo.converter;

import com.example.demo.dto.department.DepartmentCreateRequest;
import com.example.demo.dto.department.DepartmentUpdateRequest;
import com.example.demo.entity.Department;
import com.example.demo.vo.DepartmentResponse;

/**
 * Department 模块的数据转换器。
 *
 * <p>只负责字段复制，不负责租户校验、状态设置和部门树规则。</p>
 */
public final class DepartmentConverter {

    private DepartmentConverter() {
    }

    /** 将创建请求转换为待保存的实体。 */
    public static Department toEntity(DepartmentCreateRequest request) {
        Department department = new Department();
        department.setTenantId(request.getTenantId());
        department.setParentId(request.getParentId());
        department.setDepartmentCode(request.getDepartmentCode());
        department.setDepartmentName(request.getDepartmentName());
        department.setSortOrder(request.getSortOrder());
        return department;
    }

    /** 将可修改的字段覆盖到已存在的实体上。 */
    public static void copyToEntity(DepartmentUpdateRequest request, Department department) {
        department.setDepartmentName(request.getDepartmentName());
        department.setSortOrder(request.getSortOrder());
    }

    /** 将数据库实体转换为接口响应。 */
    public static DepartmentResponse toResponse(Department department) {
        DepartmentResponse response = new DepartmentResponse();
        response.setId(department.getId());
        response.setTenantId(department.getTenantId());
        response.setParentId(department.getParentId());
        response.setDepartmentCode(department.getDepartmentCode());
        response.setDepartmentName(department.getDepartmentName());
        response.setLeaderUserId(department.getLeaderUserId());
        response.setSortOrder(department.getSortOrder());
        response.setStatus(department.getStatus());
        response.setCreatedAt(department.getCreatedAt());
        response.setUpdatedAt(department.getUpdatedAt());
        return response;
    }
}
