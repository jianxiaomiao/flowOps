package com.example.demo.config;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/** 当未启用 Milvus 自动配置时提供兜底 VectorStore，避免阻塞非 AI 业务启动。 */
@Configuration
public class VectorStoreFallbackConfig {

    @Bean
    @ConditionalOnMissingBean(VectorStore.class)
    VectorStore fallbackVectorStore() {
        return new VectorStore() {
            @Override
            public void add(List<Document> documents) {
                throw new IllegalStateException("向量检索服务未启用");
            }

            @Override
            public void delete(List<String> idList) {
                throw new IllegalStateException("向量检索服务未启用");
            }

            @Override
            public void delete(Filter.Expression filterExpression) {
                throw new IllegalStateException("向量检索服务未启用");
            }

            @Override
            public List<Document> similaritySearch(SearchRequest request) {
                throw new IllegalStateException("向量检索服务未启用");
            }
        };
    }
}
