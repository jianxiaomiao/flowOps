package com.example.demo.service;

import com.example.demo.dto.knowledge.KnowledgeRecommendationDTO;
import com.example.demo.dto.knowledge.KnowledgeRecommendationFeedbackRequest;

import java.util.List;

public interface KnowledgeRecommendationService {
    List<KnowledgeRecommendationDTO> recommendForTicket(Long ticketId);
    void feedback(Long ticketId, Long articleId, KnowledgeRecommendationFeedbackRequest request);
}
