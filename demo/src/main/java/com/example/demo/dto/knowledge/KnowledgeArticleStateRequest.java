package com.example.demo.dto.knowledge;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class KnowledgeArticleStateRequest {
    @NotNull private Integer version;
}
