package com.example.demo.integration;

import com.example.demo.common.BusinessException;
import com.example.demo.dto.ticket.TicketCreateRequest;
import com.example.demo.service.TicketService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.security.MessageDigest;
import java.util.Objects;

@Service
class ExternalTicketService {
    private final JdbcTemplate jdbc;
    private final TicketService tickets;
    private final ObjectMapper json;
    private final TransactionTemplate transaction;

    ExternalTicketService(JdbcTemplate jdbc, TicketService tickets, ObjectMapper json,
                          PlatformTransactionManager transactions) {
        this.jdbc = jdbc;
        this.tickets = tickets;
        this.json = json;
        transaction = new TransactionTemplate(transactions);
        transaction.setIsolationLevel(org.springframework.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED);
    }

    Result importTicket(TicketIntegration integration, ExternalTicketController.Request request) {
        byte[] digest = digest(request);
        try {
            return Objects.requireNonNull(transaction.execute(status -> {
                Result prior = lookup(integration, request, digest);
                if (prior != null) return prior;

                jdbc.update("""
                        INSERT INTO external_ticket_link
                            (tenant_id, integration_id, external_ticket_id, event_id, request_digest)
                        VALUES (?, ?, ?, ?, ?)
                        """, integration.tenantId(), integration.id(), request.externalTicketId().toString(),
                        request.eventId().toString(), digest);

                var input = new TicketCreateRequest();
                input.setTitle(request.title());
                input.setDescription(request.description());
                input.setHandlingDepartmentId(integration.departmentId());
                input.setPriority(integration.priority());
                input.setTicketType("CUSTOMER_ISSUE");
                var ticket = tickets.createExternalTicket(integration.tenantId(), integration.actorId(), input);
                int linked = jdbc.update("""
                        UPDATE external_ticket_link SET ticket_id = ?
                        WHERE tenant_id = ? AND integration_id = ? AND external_ticket_id = ?
                        """, ticket.getId(), integration.tenantId(), integration.id(), request.externalTicketId().toString());
                if (linked != 1) throw new IllegalStateException("外部工单关联写入失败");
                return new Result(true, ack(request, ticket.getId(), ticket.getTicketNo()));
            }));
        } catch (DuplicateKeyException conflict) {
            Result prior = lookup(integration, request, digest);
            if (prior != null) return prior;
            throw conflict;
        } catch (BusinessException failure) {
            if (failure.getCode() == 30002) {
                throw new BusinessException(50001, "FlowOps 工单创建发生并发冲突，请重试");
            }
            throw failure;
        }
    }

    private Result lookup(TicketIntegration integration, ExternalTicketController.Request request, byte[] digest) {
        return jdbc.query("""
                SELECT l.external_ticket_id, l.event_id, l.request_digest, l.ticket_id, t.ticket_no
                FROM external_ticket_link l JOIN ticket t ON t.id = l.ticket_id AND t.tenant_id = l.tenant_id
                WHERE l.tenant_id = ? AND l.integration_id = ?
                    AND (l.external_ticket_id = ? OR l.event_id = ?)
                """, rs -> {
            if (!rs.next()) return null;
            if (!request.externalTicketId().toString().equals(rs.getString("external_ticket_id"))
                    || !request.eventId().toString().equals(rs.getString("event_id"))
                    || !MessageDigest.isEqual(digest, rs.getBytes("request_digest"))) {
                throw new BusinessException(30015, "外部工单标识与既有事件冲突");
            }
            return new Result(false, ack(request, rs.getLong("ticket_id"), rs.getString("ticket_no")));
        }, integration.tenantId(), integration.id(), request.externalTicketId().toString(), request.eventId().toString());
    }

    private byte[] digest(ExternalTicketController.Request request) {
        try {
            return TicketIntegrationSecurity.sha256(json.writeValueAsBytes(request));
        } catch (com.fasterxml.jackson.core.JsonProcessingException error) {
            throw new IllegalStateException("外部工单摘要生成失败", error);
        }
    }

    private static ExternalTicketController.Ack ack(ExternalTicketController.Request request, long id, String number) {
        return new ExternalTicketController.Ack(request.eventId(), request.externalTicketId(), Long.toString(id), number);
    }

    record Result(boolean created, ExternalTicketController.Ack ack) {}
}
