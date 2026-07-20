package com.example.demo.messaging;

import com.example.demo.entity.Notification;
import com.example.demo.entity.ProcessedEvent;
import com.example.demo.mapper.NotificationMapper;
import com.example.demo.mapper.ProcessedEventMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "flowops.messaging", name = "enabled", havingValue = "true")
/**
 * 消费工单领域事件，并为事件中的每位接收人创建一条站内通知。
 *
 * <p>先写 {@code processed_event} 进行幂等去重，因此 RabbitMQ 重复投递不会制造重复通知。</p>
 */
public class NotificationEventConsumer {
    private static final String CONSUMER_NAME = "notification-consumer-v1";
    private final ProcessedEventMapper processedEventMapper;
    private final NotificationMapper notificationMapper;
    private final ObjectMapper objectMapper;

    @Transactional
    @RabbitListener(queues = RabbitTopology.NOTIFICATION_QUEUE)
    /**
     * 处理一条来自通知队列的 JSON 消息。
     *
     * @param rawMessage RabbitMQ 投递的事件信封 JSON。
     * @throws Exception JSON 解析或数据库写入失败时抛出，由消息框架按照重试和死信配置处理。
     */
    public void consume(String rawMessage) throws Exception {
        JsonNode event = objectMapper.readTree(rawMessage);
        ProcessedEvent processed = new ProcessedEvent();
        processed.setTenantId(event.path("tenantId").longValue());
        processed.setEventId(event.path("eventId").textValue());
        processed.setConsumerName(CONSUMER_NAME);
        processed.setProcessedAt(LocalDateTime.now());
        if (processedEventMapper.insertIfAbsent(processed) == 0) {
            return;
        }

        JsonNode payload = event.path("payload");
        for (JsonNode recipient : payload.path("recipientIds")) {
            Notification notification = new Notification();
            notification.setTenantId(processed.getTenantId());
            notification.setRecipientId(recipient.longValue());
            notification.setNotificationType(event.path("eventType").textValue());
            notification.setTitle(title(event.path("eventType").textValue()));
            notification.setContent(content(payload));
            notification.setBusinessType(event.path("aggregateType").textValue());
            notification.setBusinessId(event.path("aggregateId").longValue());
            notification.setSourceEventId(processed.getEventId());
            notificationMapper.insert(notification);
        }
    }

    /**
     * 将内部事件类型翻译为用户可见通知标题。
     *
     * @param eventType 工单事件类型。
     * @return 简短通知标题。
     */
    private String title(String eventType) {
        return switch (eventType) {
            case "TICKET_CREATED" -> "新工单已创建";
            case "TICKET_ASSIGNED" -> "工单已分派";
            case "TICKET_SLA_RESPONSE_BREACHED" -> "工单响应 SLA 已超时";
            case "TICKET_SLA_RESOLUTION_BREACHED" -> "工单解决 SLA 已超时";
            default -> "工单状态已变更";
        };
    }

    /**
     * 根据事件负载生成通知正文。
     *
     * @param payload 事件中的业务负载。
     * @return 包含工单编号和标题的通知正文。
     */
    private String content(JsonNode payload) {
        return "工单 " + payload.path("ticketNo").asText() + "：" + payload.path("title").asText();
    }
}
