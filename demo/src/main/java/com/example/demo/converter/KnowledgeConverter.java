package com.example.demo.converter;

import com.example.demo.dto.knowledge.KnowledgeArticleCreateRequest;
import com.example.demo.dto.knowledge.KnowledgeCategoryCreateRequest;
import com.example.demo.entity.KnowledgeArticle;
import com.example.demo.entity.KnowledgeCategory;
import com.example.demo.vo.KnowledgeArticleResponse;
import com.example.demo.vo.KnowledgeCategoryResponse;
import org.springframework.beans.BeanUtils;

public final class KnowledgeConverter {
    private KnowledgeConverter() { }
    public static KnowledgeCategory toEntity(KnowledgeCategoryCreateRequest source) { KnowledgeCategory target = new KnowledgeCategory(); BeanUtils.copyProperties(source, target); return target; }
    public static KnowledgeArticle toEntity(KnowledgeArticleCreateRequest source) { KnowledgeArticle target = new KnowledgeArticle(); BeanUtils.copyProperties(source, target); return target; }
    public static KnowledgeCategoryResponse toResponse(KnowledgeCategory source) { KnowledgeCategoryResponse target = new KnowledgeCategoryResponse(); BeanUtils.copyProperties(source, target); return target; }
    public static KnowledgeArticleResponse toResponse(KnowledgeArticle source) { KnowledgeArticleResponse target = new KnowledgeArticleResponse(); BeanUtils.copyProperties(source, target); return target; }
}
