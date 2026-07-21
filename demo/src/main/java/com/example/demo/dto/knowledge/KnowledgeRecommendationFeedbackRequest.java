package com.example.demo.dto.knowledge;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class KnowledgeRecommendationFeedbackRequest {
    @NotBlank(message = "反馈状态不能为空")
    private String status;
}
