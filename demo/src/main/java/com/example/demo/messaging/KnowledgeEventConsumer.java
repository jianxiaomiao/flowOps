package com.example.demo.messaging;

import com.example.demo.entity.ProcessedEvent;
import com.example.demo.mapper.ProcessedEventMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "flowops.messaging", name = "enabled", havingValue = "true")
/**
 * 消费知识库文章相关事件，并将文本转换为向量存入/移出 Milvus。
 */
public class KnowledgeEventConsumer {
    private static final String CONSUMER_NAME = "knowledge-vector-consumer-v1";
    private final ProcessedEventMapper processedEventMapper;
    private final VectorStore vectorStore;
    private final ObjectMapper objectMapper;

    @Transactional
    @RabbitListener(queues = RabbitTopology.KNOWLEDGE_QUEUE)
    public void consume(String rawMessage) throws Exception {
        JsonNode event = objectMapper.readTree(rawMessage);
        
        ProcessedEvent processed = new ProcessedEvent();
        processed.setTenantId(event.path("tenantId").longValue());
        processed.setEventId(event.path("eventId").textValue());
        processed.setConsumerName(CONSUMER_NAME);
        processed.setProcessedAt(LocalDateTime.now());
        
        // 幂等校验，防止重复消费
        if (processedEventMapper.insertIfAbsent(processed) == 0) {
            log.debug("Event {} already processed", processed.getEventId());
            return;
        }

        String eventType = event.path("eventType").textValue();
        JsonNode payload = event.path("payload");
        Long articleId = payload.path("articleId").longValue();
        Long tenantId = processed.getTenantId();

        try {
            // 先尝试移除该文章的旧向量记录，保证更新的幂等性
            deleteOldVectors(tenantId, articleId);

            if ("KNOWLEDGE_ARTICLE_PUBLISHED".equals(eventType) || "KNOWLEDGE_ARTICLE_UPDATED".equals(eventType)) {
                String title = payload.path("title").textValue();
                String content = payload.path("content").textValue();
                int version = payload.path("version").intValue();
                
                String textToEmbed = "标题: " + title + "\n正文: " + content;
                
                // 将文本按照 Token 分块
                TokenTextSplitter splitter = new TokenTextSplitter();
                List<Document> documents = splitter.split(List.of(new Document(textToEmbed)));
                
                // 附加必要的 Metadata 以供过滤
                for (int i = 0; i < documents.size(); i++) {
                    Document doc = documents.get(i);
                    doc.getMetadata().put("tenant_id", tenantId);
                    doc.getMetadata().put("article_id", articleId);
                    doc.getMetadata().put("version", version);
                    doc.getMetadata().put("chunk_index", i);
                }
                
                vectorStore.add(documents);
                log.info("Saved {} vector chunks for article {} to Milvus", documents.size(), articleId);
                
            } else if ("KNOWLEDGE_ARTICLE_ARCHIVED".equals(eventType)) {
                log.info("Deleted vectors for archived article {}", articleId);
            }
        } catch (Exception e) {
            log.error("Failed to process knowledge event {}", eventType, e);
            throw e; // 异常抛出，交给 RabbitMQ 进行重试或转入死信队列
        }
    }

    private void deleteOldVectors(Long tenantId, Long articleId) {
        try {
            // 根据 Metadata 进行精确删除，查询所有当前 articleId 的块
            FilterExpressionBuilder filterBuilder = new FilterExpressionBuilder();
            SearchRequest request = SearchRequest.builder()
                    .query("")
                    .filterExpression(filterBuilder.and(
                            filterBuilder.eq("tenant_id", tenantId),
                            filterBuilder.eq("article_id", articleId)).build())
                    .topK(1000)
                    .build();
            
            List<Document> oldDocs = vectorStore.similaritySearch(request);
            List<String> idsToDelete = oldDocs.stream().map(Document::getId).toList();
            
            if (!idsToDelete.isEmpty()) {
                vectorStore.delete(idsToDelete);
                log.debug("Deleted {} old vectors for article {}", idsToDelete.size(), articleId);
            }
        } catch (Exception e) {
            log.warn("Failed to clean up old vectors for article {}, might leave orphan vectors.", articleId, e);
        }
    }
}
