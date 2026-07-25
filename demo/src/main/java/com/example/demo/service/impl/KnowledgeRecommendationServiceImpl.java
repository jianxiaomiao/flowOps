package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.demo.common.BusinessException;
import com.example.demo.dto.knowledge.KnowledgeRecommendationDTO;
import com.example.demo.dto.knowledge.KnowledgeRecommendationFeedbackRequest;
import com.example.demo.entity.KnowledgeArticle;
import com.example.demo.entity.KnowledgeRecommendationFeedback;
import com.example.demo.entity.Ticket;
import com.example.demo.mapper.KnowledgeRecommendationFeedbackMapper;
import com.example.demo.mapper.TicketMapper;
import com.example.demo.security.CurrentUserProvider;
import com.example.demo.security.TenantContext;
import com.example.demo.security.TicketReadScope;
import com.example.demo.service.KnowledgeRecommendationService;
import com.example.demo.service.KnowledgeService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeRecommendationServiceImpl implements KnowledgeRecommendationService {
    private final VectorStore vectorStore;
    private final KnowledgeService knowledgeService;
    private final TicketMapper ticketMapper;
    private final KnowledgeRecommendationFeedbackMapper feedbackMapper;
    private final TicketReadScope ticketReadScope;
    private final TenantContext tenantContext;
    private final CurrentUserProvider currentUserProvider;
    private final MeterRegistry meterRegistry;

    @Override
    public List<KnowledgeRecommendationDTO> recommendForTicket(Long ticketId) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            requireKnowledgeReadAuthority();
            if (!ticketReadScope.canRead(ticketId)) {
                throw new BusinessException(20001, "工单不存在或无权访问");
            }
            Long tenantId = tenantContext.tenantId();
            Ticket ticket = ticketMapper.selectById(ticketId);
            if (ticket == null || !ticket.getTenantId().equals(tenantId)) {
                throw new BusinessException(20001, "工单不存在");
            }

            String queryText = "标题: " + ticket.getTitle() + "\n正文: " + (StringUtils.hasText(ticket.getDescription()) ? ticket.getDescription() : "");
            
            List<KnowledgeRecommendationDTO> results = new ArrayList<>();
            try {
                // 1. 召回：使用 Milvus 进行向量相似度搜索，带上严格的租户隔离过滤
                FilterExpressionBuilder filterBuilder = new FilterExpressionBuilder();
                SearchRequest searchRequest = SearchRequest.builder()
                        .query(queryText)
                        .filterExpression(filterBuilder.eq("tenant_id", tenantId).build())
                        .topK(10)
                        .build();
                
                List<Document> documents = vectorStore.similaritySearch(searchRequest);
                
                // 去重
                List<Long> articleIds = documents.stream()
                        .map(doc -> Long.valueOf(doc.getMetadata().get("article_id").toString()))
                        .distinct()
                        .limit(5)
                        .toList();

                // 2. 鉴权过滤：查询 MySQL 并复用既有的知识库可见范围规则
                for (Long articleId : articleIds) {
                    try {
                        KnowledgeArticle article = knowledgeService.getVisibleArticle(articleId);
                        KnowledgeRecommendationDTO dto = new KnowledgeRecommendationDTO();
                        dto.setArticleId(article.getId());
                        dto.setTitle(article.getTitle());
                        dto.setSummary(article.getSummary());
                        // TODO: Calculate real document max similarity if needed. We simplify it here.
                        
                        // 查询采用反馈
                        KnowledgeRecommendationFeedback feedback = feedbackMapper.selectOne(
                                Wrappers.lambdaQuery(KnowledgeRecommendationFeedback.class)
                                        .eq(KnowledgeRecommendationFeedback::getTenantId, tenantId)
                                        .eq(KnowledgeRecommendationFeedback::getTicketId, ticketId)
                                        .eq(KnowledgeRecommendationFeedback::getArticleId, articleId)
                        );
                        if (feedback != null) {
                            dto.setFeedbackStatus(feedback.getStatus());
                        }
                        results.add(dto);
                    } catch (BusinessException ignored) {
                        // 剔除越权、已归档或处于草稿状态的文章
                    }
                }
            } catch (Exception exception) {
                log.error("AI 向量检索失败，tenantId={}, ticketId={}", tenantId, ticketId, exception);
                meterRegistry.counter("knowledge.recommendation.failure").increment();
                throw new BusinessException(50001, "AI 推荐服务暂时不可用，请稍后重试");
            }
            return results;
        } finally {
            sample.stop(meterRegistry.timer("knowledge.recommendation.recall"));
        }
    }

    @Override
    @Transactional
    public void feedback(Long ticketId, Long articleId, KnowledgeRecommendationFeedbackRequest request) {
        requireKnowledgeReadAuthority();
        if (!ticketReadScope.canRead(ticketId)) {
            throw new BusinessException(20001, "工单不存在或无权访问");
        }
        Long tenantId = tenantContext.tenantId();
        Long userId = currentUserProvider.requireCurrentUser().userId();
        
        // 确保文章可见
        knowledgeService.getVisibleArticle(articleId);

        KnowledgeRecommendationFeedback feedback = feedbackMapper.selectOne(
                Wrappers.lambdaQuery(KnowledgeRecommendationFeedback.class)
                        .eq(KnowledgeRecommendationFeedback::getTenantId, tenantId)
                        .eq(KnowledgeRecommendationFeedback::getTicketId, ticketId)
                        .eq(KnowledgeRecommendationFeedback::getArticleId, articleId)
        );

        if (feedback == null) {
            feedback = new KnowledgeRecommendationFeedback();
            feedback.setTenantId(tenantId);
            feedback.setTicketId(ticketId);
            feedback.setArticleId(articleId);
            feedback.setStatus(request.getStatus());
            feedback.setCreatedBy(userId);
            feedback.setCreatedAt(LocalDateTime.now());
            feedback.setUpdatedAt(LocalDateTime.now());
            feedbackMapper.insert(feedback);
        } else {
            feedback.setStatus(request.getStatus());
            feedback.setUpdatedAt(LocalDateTime.now());
            feedbackMapper.updateById(feedback);
        }
        
        meterRegistry.counter("knowledge.recommendation.feedback", "status", request.getStatus()).increment();
    }

    private void requireKnowledgeReadAuthority() {
        Set<String> authorities = currentUserProvider.requireCurrentUser().authorities();
        if (!authorities.contains("knowledge:read")
                && !authorities.contains("knowledge:write")
                && !authorities.contains("knowledge:manage")) {
            throw new BusinessException(10003, "没有读取知识推荐的权限");
        }
    }
}
