package com.example.demo.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class KnowledgeArticleResponse {
    private Long id; private Long tenantId; private Long categoryId; private String title; private String summary; private String content;
    private String status; private String visibility; private Long departmentId; private Long authorId; private LocalDateTime publishedAt;
    private Integer version; private LocalDateTime createdAt; private LocalDateTime updatedAt;
}
