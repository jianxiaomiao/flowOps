package com.example.demo.integration;

import com.example.demo.common.BusinessException;
import com.example.demo.service.TicketService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.sql.ResultSet;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ExternalTicketServiceTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final TicketService tickets = mock(TicketService.class);
    private final PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    private final ExternalTicketService service = new ExternalTicketService(jdbc, tickets, json, transactions);
    private final TicketIntegration integration = new TicketIntegration("nexus-a", 11, 12, 13, "P2");
    private final ExternalTicketController.Request request = new ExternalTicketController.Request(1,
            UUID.randomUUID(), UUID.randomUUID(), "Title", "Body", Instant.parse("2026-11-24T00:00:00Z"), null);

    @Test
    void replayReturnsCommittedTicketWithoutCreatingAnother() throws Exception {
        stubExisting(TicketIntegrationSecurity.sha256(json.writeValueAsBytes(request)));
        var result = service.importTicket(integration, request);
        assertFalse(result.created());
        assertEquals("42", result.ack().flowOpsTicketId());
        assertEquals(request.eventId(), result.ack().eventId());
        verifyNoInteractions(tickets);
    }

    @Test
    void changedContentIsAConflict() throws Exception {
        stubExisting(new byte[32]);
        var conflict = assertThrows(BusinessException.class, () -> service.importTicket(integration, request));
        assertEquals(30015, conflict.getCode());
        verifyNoInteractions(tickets);
    }

    @Test
    void retryableCreationConflictRollsBackTheImport() {
        when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(tickets.createExternalTicket(eq(11L), eq(13L), any()))
                .thenThrow(new BusinessException(30002, "工单编号存在并发冲突"));

        var failure = assertThrows(BusinessException.class, () -> service.importTicket(integration, request));

        assertEquals(50001, failure.getCode());
        verify(transactions).rollback(any());
        verify(transactions, never()).commit(any());
    }

    @SuppressWarnings("unchecked")
    private void stubExisting(byte[] digest) throws Exception {
        when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        var resultSet = mock(ResultSet.class);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString("external_ticket_id")).thenReturn(request.externalTicketId().toString());
        when(resultSet.getString("event_id")).thenReturn(request.eventId().toString());
        when(resultSet.getBytes("request_digest")).thenReturn(digest);
        when(resultSet.getLong("ticket_id")).thenReturn(42L);
        when(resultSet.getString("ticket_no")).thenReturn("OPS-20261124-0001");
        when(jdbc.query(anyString(), any(ResultSetExtractor.class), eq(11L), eq("nexus-a"),
                eq(request.externalTicketId().toString()), eq(request.eventId().toString())))
                .thenAnswer(call -> call.<ResultSetExtractor<?>>getArgument(1).extractData(resultSet));
    }
}
