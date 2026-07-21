package com.example.demo.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class KnowledgeCategoryResponse {
    private Long id; private Long tenantId; private Long parentId; private String categoryCode; private String categoryName;
    private String description; private Integer sortOrder; private String status; private Integer version;
    private LocalDateTime createdAt; private LocalDateTime updatedAt;
}
