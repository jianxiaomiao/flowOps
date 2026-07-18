package com.example.demo.validator;

import com.example.demo.common.BusinessException;
import com.example.demo.entity.Department;
import com.example.demo.entity.Tenant;
import com.example.demo.service.DepartmentService;
import com.example.demo.service.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 校验租户范围内资源是否可用。
 *
 * <p>这里集中处理“存在、启用、归属当前租户”这类跨表规则；
 * 具体业务规则仍由调用方的 Service 负责。</p>
 */
@Component
@RequiredArgsConstructor
public class TenantScopeValidator {

    private static final String STATUS_ACTIVE = "ACTIVE";

    private final TenantService tenantService;
    private final DepartmentService departmentService;

    /** 确保租户存在且处于启用状态。 */
    public void requireActiveTenant(Long tenantId) {
        boolean exists = tenantService.lambdaQuery()
                .eq(Tenant::getId, tenantId)
                .eq(Tenant::getStatus, STATUS_ACTIVE)
                .exists();
        if (!exists) {
            throw new BusinessException(20001, "租户不存在或已停用");
        }
    }

    /** 确保部门存在、启用，并且属于指定租户。 */
    public void requireActiveDepartment(Long tenantId, Long departmentId) {
        boolean exists = departmentService.lambdaQuery()
                .eq(Department::getId, departmentId)
                .eq(Department::getTenantId, tenantId)
                .eq(Department::getStatus, STATUS_ACTIVE)
                .exists();
        if (!exists) {
            throw new BusinessException(20001, "部门不存在、已停用或不属于当前租户");
        }
    }
}
