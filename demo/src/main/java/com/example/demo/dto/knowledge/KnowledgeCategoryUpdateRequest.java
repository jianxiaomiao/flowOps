package com.example.demo.dto.knowledge;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class KnowledgeCategoryUpdateRequest {
    @NotNull @Positive private Long id;
    @Positive private Long parentId;
    @NotBlank @Size(max = 100) private String categoryName;
    @Size(max = 500) private String description;
    @NotNull @PositiveOrZero private Integer sortOrder;
    @NotNull private Integer version;
}
