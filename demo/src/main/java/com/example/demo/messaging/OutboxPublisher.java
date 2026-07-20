package com.example.demo.messaging;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.demo.entity.OutboxEvent;
import com.example.demo.mapper.OutboxEventMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "flowops.messaging", name = "enabled", havingValue = "true")
/**
 * 周期性发布 Outbox 中待投递事件，并以发布确认与指数退避处理失败。
 */
public class OutboxPublisher {
    private final OutboxEventMapper outboxEventMapper;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${flowops.messaging.publish-delay:5s}")
    /**
     * 查找到期的 PENDING 或 FAILED 事件，最多发布 100 条。
     */
    public void publishPending() {
        LocalDateTime now = LocalDateTime.now();
        List<OutboxEvent> events = outboxEventMapper.selectList(Wrappers.lambdaQuery(OutboxEvent.class)
                .in(OutboxEvent::getStatus, "PENDING", "FAILED")
                .and(wrapper -> wrapper.isNull(OutboxEvent::getNextRetryAt)
                        .or().le(OutboxEvent::getNextRetryAt, now))
                .orderByAsc(OutboxEvent::getCreatedAt)
                .last("LIMIT 100"));
        events.forEach(this::publishOne);
    }

    /**
     * 发布单条 Outbox 事件并等待 RabbitMQ 确认。
     *
     * <p>确认成功才更新为 PUBLISHED；失败则记录错误并按指数退避设置下次重试时间。</p>
     *
     * @param event 待发布事件。
     */
    private void publishOne(OutboxEvent event) {
        try {
            String message = envelope(event);
            CorrelationData correlation = new CorrelationData(event.getEventId());
            rabbitTemplate.convertAndSend(RabbitTopology.EXCHANGE, routingKey(event.getEventType()), message, correlation);
            CorrelationData.Confirm confirm = correlation.getFuture().get(5, TimeUnit.SECONDS);
            if (!confirm.isAck()) {
                throw new IllegalStateException("Broker NACK: " + confirm.getReason());
            }
            outboxEventMapper.update(null, Wrappers.lambdaUpdate(OutboxEvent.class)
                    .eq(OutboxEvent::getId, event.getId())
                    .in(OutboxEvent::getStatus, "PENDING", "FAILED")
                    .set(OutboxEvent::getStatus, "PUBLISHED")
                    .set(OutboxEvent::getPublishedAt, LocalDateTime.now())
                    .set(OutboxEvent::getLastError, null));
        } catch (Exception exception) {
            int retryCount = event.getRetryCount() == null ? 1 : event.getRetryCount() + 1;
            long delaySeconds = Math.min(300, 1L << Math.min(retryCount, 8));
            outboxEventMapper.update(null, Wrappers.lambdaUpdate(OutboxEvent.class)
                    .eq(OutboxEvent::getId, event.getId())
                    .in(OutboxEvent::getStatus, "PENDING", "FAILED")
                    .set(OutboxEvent::getStatus, "FAILED")
                    .set(OutboxEvent::getRetryCount, retryCount)
                    .set(OutboxEvent::getNextRetryAt, LocalDateTime.now().plusSeconds(delaySeconds))
                    .set(OutboxEvent::getLastError, abbreviate(exception.getMessage(), 500)));
            log.warn("Outbox 发布失败 eventId={} retryCount={}", event.getEventId(), retryCount, exception);
        }
    }

    /**
     * 将数据库事件包装为消费者可识别的消息信封。
     *
     * @param event Outbox 事件。
     * @return 包含事件元信息和 payload 的 JSON 消息。
     * @throws Exception 解析或序列化 JSON 失败时抛出。
     */
    private String envelope(OutboxEvent event) throws Exception {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("eventId", event.getEventId());
        message.put("tenantId", event.getTenantId());
        message.put("aggregateType", event.getAggregateType());
        message.put("aggregateId", event.getAggregateId());
        message.put("eventType", event.getEventType());
        JsonNode payload = objectMapper.readTree(event.getPayload());
        message.put("payload", payload);
        return objectMapper.writeValueAsString(message);
    }

    /**
     * 把事件类型转换为 Topic Exchange 路由键。
     *
     * @param eventType 大写下划线事件类型。
     * @return 小写点分隔路由键，例如 {@code ticket.assigned}。
     */
    private String routingKey(String eventType) {
        return eventType.toLowerCase(Locale.ROOT).replace('_', '.');
    }

    /**
     * 截断可能过长的错误信息，避免超过数据库字段长度。
     *
     * @param value 原始文本。
     * @param max 最大保留字符数。
     * @return 截断后的安全文本。
     */
    private String abbreviate(String value, int max) {
        if (value == null) return "unknown error";
        return value.length() <= max ? value : value.substring(0, max);
    }
}
