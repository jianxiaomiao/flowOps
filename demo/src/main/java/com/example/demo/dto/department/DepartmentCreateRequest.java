package com.example.demo.dto.department;

import lombok.Data;

/**
 * 创建部门时客户端允许提交的数据。
 */
@Data
public class DepartmentCreateRequest {

    /** 根部门为空。 */
    private Long parentId;

    private String departmentCode;

    private String departmentName;

    private Integer sortOrder;
}
