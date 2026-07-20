package com.example.demo.sla;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.demo.entity.Ticket;
import com.example.demo.mapper.TicketMapper;
import com.example.demo.messaging.OutboxEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
/**
 * 原子标记 SLA 超时并产生可靠通知事件。
 */
public class SlaBreachProcessor {
    private final TicketMapper ticketMapper;
    private final OutboxEventService outboxEventService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    /**
     * 处理一张候选工单的响应与解决 SLA。
     *
     * <p>更新 SQL 以 {@code PENDING} 为条件，因此多个扫描器同时处理同一工单时，只有一个能真正标记超时并写事件。</p>
     *
     * @param ticketId 待处理的工单 ID。
     */
    public void process(Long ticketId) {
        Ticket ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) return;
        LocalDateTime now = LocalDateTime.now();
        //是否“响应超时”
        if ("PENDING".equals(ticket.getResponseSlaStatus()) && !ticket.getResponseDueAt().isAfter(now)) {
            int changed = ticketMapper.update(null, Wrappers.lambdaUpdate(Ticket.class)
                    .eq(Ticket::getId, ticketId)
                    .eq(Ticket::getResponseSlaStatus, "PENDING")
                    .le(Ticket::getResponseDueAt, now)
                    .set(Ticket::getResponseSlaStatus, "BREACHED")
                    .set(Ticket::getResponseBreachedAt, now));
            if (changed == 1) {
                outboxEventService.enqueueTicketEvent(ticket, "TICKET_SLA_RESPONSE_BREACHED",
                        List.of(recipient(ticket)));
            }
        }
        //是否“解决超时”
        if ("PENDING".equals(ticket.getResolutionSlaStatus()) && !ticket.getResolutionDueAt().isAfter(now)) {
            int changed = ticketMapper.update(null, Wrappers.lambdaUpdate(Ticket.class)
                    .eq(Ticket::getId, ticketId)
                    .eq(Ticket::getResolutionSlaStatus, "PENDING")
                    .le(Ticket::getResolutionDueAt, now)
                    .set(Ticket::getResolutionSlaStatus, "BREACHED")
                    .set(Ticket::getResolutionBreachedAt, now));
            if (changed == 1) {
                outboxEventService.enqueueTicketEvent(ticket, "TICKET_SLA_RESOLUTION_BREACHED",
                        List.of(recipient(ticket)));
            }
        }
    }

    /**
     * 选择 SLA 超时通知的接收人。
     *
     * @param ticket 已超时的工单。
     * @return 优先当前处理人；未分派时通知创建人。
     */
    private Long recipient(Ticket ticket) {
        return ticket.getCurrentAssigneeId() == null ? ticket.getCreatorId() : ticket.getCurrentAssigneeId();
    }
}
