package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("knowledge_recommendation_feedback")
public class KnowledgeRecommendationFeedback {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long ticketId;
    private Long articleId;
    private String status;
    private Double similarity;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
