package com.example.demo.dto.role;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 角色分页查询条件。 */
@Data
public class RolePageQuery {

    @Min(value = 1, message = "pageNum 不能小于 1")
    private Integer pageNum = 1;

    @Min(value = 1, message = "pageSize 不能小于 1")
    @Max(value = 100, message = "pageSize 不能超过 100")
    private Integer pageSize = 10;

    @NotNull(message = "tenantId 不能为空")
    private Long tenantId;

    private String roleCode;
    private String roleName;
    private String status;
}
