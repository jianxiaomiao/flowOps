package com.example.demo.dto.knowledge;

import lombok.Data;

@Data
public class KnowledgeRecommendationDTO {
    private Long articleId;
    private String title;
    private String summary;
    private Double similarity;
    private String feedbackStatus;
}
