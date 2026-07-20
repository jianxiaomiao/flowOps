package com.example.demo.sla;

import com.example.demo.entity.Ticket;
import com.example.demo.mapper.TicketMapper;
import com.example.demo.messaging.OutboxEventService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SlaBreachProcessorTest {
    @Mock TicketMapper ticketMapper;
    @Mock OutboxEventService outboxEventService;

    @Test
    void duplicateScansShouldOnlyCreateOneOutboxEvent() {
        Ticket ticket = new Ticket();
        ticket.setId(1L); ticket.setTenantId(2L); ticket.setCreatorId(3L);
        ticket.setResponseSlaStatus("PENDING"); ticket.setResponseDueAt(LocalDateTime.now().minusMinutes(1));
        ticket.setResolutionSlaStatus("MET"); ticket.setResolutionDueAt(LocalDateTime.now().plusHours(1));
        when(ticketMapper.selectById(1L)).thenReturn(ticket);
        when(ticketMapper.update(isNull(), any())).thenReturn(1, 0);
        SlaBreachProcessor processor = new SlaBreachProcessor(ticketMapper, outboxEventService);

        processor.process(1L);
        processor.process(1L);

        verify(outboxEventService, times(1)).enqueueTicketEvent(eq(ticket),
                eq("TICKET_SLA_RESPONSE_BREACHED"), anyCollection());
    }
}
