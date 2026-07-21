package com.example.demo.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.dto.knowledge.*;
import com.example.demo.entity.KnowledgeArticle;
import com.example.demo.entity.KnowledgeCategory;
import java.util.List;

/** 知识库用例入口：分类、文章状态、检索和工单关联均在这里落实租户边界。 */
public interface KnowledgeService {
    KnowledgeCategory createCategory(KnowledgeCategoryCreateRequest request);
    KnowledgeCategory updateCategory(KnowledgeCategoryUpdateRequest request);
    List<KnowledgeCategory> listCategories();
    KnowledgeArticle createArticle(KnowledgeArticleCreateRequest request);
    KnowledgeArticle updateArticle(KnowledgeArticleUpdateRequest request);
    KnowledgeArticle getVisibleArticle(Long articleId);
    Page<KnowledgeArticle> pageVisibleArticles(KnowledgeArticlePageQuery query);
    KnowledgeArticle publishArticle(Long articleId, KnowledgeArticleStateRequest request);
    KnowledgeArticle archiveArticle(Long articleId, KnowledgeArticleStateRequest request);
    void linkTicket(Long articleId, Long ticketId, KnowledgeArticleTicketCreateRequest request);
    List<KnowledgeArticle> listVisibleArticlesForTicket(Long ticketId);
}
