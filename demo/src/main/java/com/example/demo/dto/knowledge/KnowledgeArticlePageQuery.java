package com.example.demo.dto.knowledge;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class KnowledgeArticlePageQuery {
    @Min(1) private long pageNum = 1;
    @Min(1) @Max(100) private long pageSize = 10;
    @Positive private Long categoryId;
    @Size(max = 100) private String keyword;
}
