package com.example.demo.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DepartmentResponse {
    private Long id;

    private Long tenantId;

    private Long parentId;

    private String departmentCode;

    private String departmentName;

    private Long leaderUserId;

    private Integer sortOrder;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
