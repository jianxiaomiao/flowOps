package com.example.demo.dto.knowledge;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class KnowledgeArticleCreateRequest {
    @NotNull @Positive private Long categoryId;
    @NotBlank @Size(max = 200) private String title;
    @Size(max = 500) private String summary;
    @NotBlank private String content;
    @NotBlank private String visibility;
    @Positive private Long departmentId;
}
