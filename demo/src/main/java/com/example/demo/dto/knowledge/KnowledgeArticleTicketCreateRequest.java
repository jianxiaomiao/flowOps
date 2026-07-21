package com.example.demo.dto.knowledge;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class KnowledgeArticleTicketCreateRequest {
    @NotBlank @Size(max = 20) private String relationType = "REFERENCE";
}
