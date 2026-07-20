package com.example.demo.messaging;

import com.example.demo.entity.OutboxEvent;
import com.example.demo.entity.Ticket;
import com.example.demo.mapper.OutboxEventMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
@RequiredArgsConstructor
/**
 * 将领域事件先持久化到 Outbox 表，保证业务数据和“待发送消息”处于同一数据库事务。
 *
 * <p>它不直接调用 RabbitMQ；定时发布器会在事务提交后异步发送。</p>
 */
public class OutboxEventService {
    private final OutboxEventMapper outboxEventMapper;
    private final ObjectMapper objectMapper;

    /**
     * 根据工单快照构造统一通知负载并写入 Outbox。
     *
     * @param ticket 触发事件的工单。
     * @param eventType 事件类型，例如 {@code TICKET_ASSIGNED}。
     * @param recipients 通知接收人 ID 集合；会去掉空值和重复值。
     * @return 已保存、状态为 PENDING 的 Outbox 事件。
     */
    public OutboxEvent enqueueTicketEvent(Ticket ticket, String eventType, Collection<Long> recipients) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("ticketNo", ticket.getTicketNo());
        payload.put("title", ticket.getTitle());
        payload.put("status", ticket.getStatus());
        payload.put("recipientIds", recipients.stream().filter(Objects::nonNull).distinct().toList());
        return enqueue(ticket.getTenantId(), "TICKET", ticket.getId(), eventType, payload);
    }

    /**
     * 写入一个通用 Outbox 事件。
     *
     * @param tenantId 事件所属租户。
     * @param aggregateType 聚合类型，例如 TICKET。
     * @param aggregateId 聚合 ID，例如工单 ID。
     * @param eventType 业务事件类型。
     * @param payload 将序列化为 JSON 的事件负载。
     * @return 已持久化的事件。
     * @throws IllegalStateException 当 Outbox 插入失败时抛出，使外层业务事务回滚。
     */
    public OutboxEvent enqueue(Long tenantId, String aggregateType, Long aggregateId,
                               String eventType, Object payload) {
        OutboxEvent event = new OutboxEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setTenantId(tenantId);
        event.setAggregateType(aggregateType);
        event.setAggregateId(aggregateId);
        event.setEventType(eventType);
        event.setPayload(toJson(payload));
        event.setStatus("PENDING");
        event.setRetryCount(0);
        if (outboxEventMapper.insert(event) != 1) {
            throw new IllegalStateException("写入 Outbox 事件失败");
        }
        return event;
    }

    /**
     * 将事件负载序列化为可持久化的 JSON。
     *
     * @param payload 任意可由 Jackson 序列化的事件对象。
     * @return JSON 字符串。
     * @throws IllegalArgumentException 当负载无法序列化时抛出。
     */
    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("事件负载无法序列化", exception);
        }
    }
}
