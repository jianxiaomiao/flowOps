package com.example.demo.dto.knowledge;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class KnowledgeArticleUpdateRequest extends KnowledgeArticleCreateRequest {
    @NotNull @Positive private Long id;
    @NotNull private Integer version;
}
