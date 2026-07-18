package com.example.demo.dto.department;

import lombok.Data;

/**
 * 创建部门时客户端允许提交的数据。
 */
@Data
public class DepartmentCreateRequest {

    /** 当前未接入登录上下文，第一版暂由请求传入；后续改为从当前登录用户获取。 */
    private Long tenantId;

    /** 根部门为空。 */
    private Long parentId;

    private String departmentCode;

    private String departmentName;

    private Integer sortOrder;
}
