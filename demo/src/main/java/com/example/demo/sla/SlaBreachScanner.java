package com.example.demo.sla;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.demo.entity.Ticket;
import com.example.demo.mapper.TicketMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "flowops.sla", name = "enabled", havingValue = "true", matchIfMissing = true)
/**
 * 定时扫描可能超出响应或解决 SLA 的工单。
 *
 * <p>扫描器只负责找到候选工单；每张工单的原子更新与事件写入由 {@link SlaBreachProcessor} 独立事务处理。</p>
 */
public class SlaBreachScanner {
    private final TicketMapper ticketMapper;
    private final SlaBreachProcessor processor;

    @Scheduled(fixedDelayString = "${flowops.sla.scan-delay:60s}")
    /**
     * 按配置间隔扫描最多 100 张到期工单，并逐张交给处理器。
     *
     * <p>单张工单失败不会中断后续处理，下一轮扫描仍会重试符合条件的数据。</p>
     */
    public void scan() {
        LocalDateTime now = LocalDateTime.now();
        List<Ticket> dueTickets = ticketMapper.selectList(Wrappers.lambdaQuery(Ticket.class)
                .and(wrapper -> wrapper
                        .eq(Ticket::getResponseSlaStatus, "PENDING").le(Ticket::getResponseDueAt, now)
                        .or()
                        .eq(Ticket::getResolutionSlaStatus, "PENDING").le(Ticket::getResolutionDueAt, now))
                .orderByAsc(Ticket::getId)
                .last("LIMIT 100"));
        for (Ticket ticket : dueTickets) {
            try {
                processor.process(ticket.getId());
            } catch (Exception exception) {
                log.error("处理 SLA 超时失败 ticketId={}", ticket.getId(), exception);
            }
        }
    }
}
