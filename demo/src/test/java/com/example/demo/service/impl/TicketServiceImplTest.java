package com.example.demo.service.impl;

import com.example.demo.common.BusinessException;
import com.example.demo.dto.ticket.TicketTransitionRequest;
import com.example.demo.entity.Ticket;
import com.example.demo.entity.TicketTransition;
import com.example.demo.entity.UserAccount;
import com.example.demo.mapper.TicketAssignmentMapper;
import com.example.demo.mapper.TicketCommentMapper;
import com.example.demo.mapper.TicketTransitionMapper;
import com.example.demo.service.SlaPolicyService;
import com.example.demo.service.UserAccountService;
import com.example.demo.ticket.TicketNumberGenerator;
import com.example.demo.ticket.TicketStatus;
import com.example.demo.validator.TenantScopeValidator;
import com.example.demo.security.TenantContext;
import com.example.demo.messaging.OutboxEventService;
import com.example.demo.security.TicketReadScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    private static final Long TENANT_ID = 1L;
    private static final Long TICKET_ID = 100L;
    private static final Long CREATOR_ID = 10L;
    private static final Long ASSIGNEE_ID = 20L;

    @Mock private TenantScopeValidator tenantScopeValidator;
    @Mock private UserAccountService userAccountService;
    @Mock private SlaPolicyService slaPolicyService;
    @Mock private TicketNumberGenerator ticketNumberGenerator;
    @Mock private TicketTransitionMapper transitionMapper;
    @Mock private TicketAssignmentMapper assignmentMapper;
    @Mock private TicketCommentMapper commentMapper;
    @Mock private TenantContext tenantContext;
    @Mock private OutboxEventService outboxEventService;
    @Mock private TicketReadScope ticketReadScope;

    private TicketServiceImpl ticketService;

    @BeforeEach
    void setUp() {
        ticketService = spy(new TicketServiceImpl(
                tenantScopeValidator,
                userAccountService,
                slaPolicyService,
                ticketNumberGenerator,
                transitionMapper,
                assignmentMapper,
                commentMapper,
                tenantContext,
                outboxEventService,
                ticketReadScope
        ));
        when(tenantContext.tenantId()).thenReturn(TENANT_ID);
    }

    @Test
    @DisplayName("正常流程应从 ASSIGNED 一直流转到 CLOSED")
    void shouldCompleteNormalWorkflow() {
        Ticket ticket = ticket(TicketStatus.ASSIGNED, 0);
        stubTicket(ticket);
        stubSuccessfulPersistence();
        stubActiveUser(CREATOR_ID);
        stubActiveUser(ASSIGNEE_ID);

        ticketService.transitionTicket(TICKET_ID, request("ACCEPT", ASSIGNEE_ID, 0, null));
        assertEquals(TicketStatus.ACCEPTED.name(), ticket.getStatus());
        assertEquals(1, ticket.getVersion());
        assertNotNull(ticket.getFirstResponseAt());
        assertEquals("MET", ticket.getResponseSlaStatus());

        ticketService.transitionTicket(TICKET_ID, request("START_PROCESS", ASSIGNEE_ID, 1, null));
        assertEquals(TicketStatus.PROCESSING.name(), ticket.getStatus());

        ticketService.transitionTicket(TICKET_ID, request("SUBMIT_RESOLUTION", ASSIGNEE_ID, 2, "重置权限后恢复正常"));
        assertEquals(TicketStatus.PENDING_CONFIRMATION.name(), ticket.getStatus());
        assertEquals("重置权限后恢复正常", ticket.getResolution());
        assertNotNull(ticket.getResolutionSubmittedAt());

        ticketService.transitionTicket(TICKET_ID, request("CONFIRM", CREATOR_ID, 3, null));
        assertEquals(TicketStatus.RESOLVED.name(), ticket.getStatus());
        assertNotNull(ticket.getResolvedAt());
        assertEquals("MET", ticket.getResolutionSlaStatus());

        ticketService.transitionTicket(TICKET_ID, request("CLOSE", CREATOR_ID, 4, null));
        assertEquals(TicketStatus.CLOSED.name(), ticket.getStatus());
        assertEquals(5, ticket.getVersion());
        assertNotNull(ticket.getClosedAt());
        verify(transitionMapper, times(5)).insert(any(TicketTransition.class));
    }

    @Test
    @DisplayName("OPEN 状态不能直接接单")
    void shouldRejectAcceptFromOpen() {
        Ticket ticket = ticket(TicketStatus.OPEN, 0);
        stubTicket(ticket);
        stubActiveUser(ASSIGNEE_ID);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> ticketService.transitionTicket(TICKET_ID, request("ACCEPT", ASSIGNEE_ID, 0, null))
        );

        assertEquals(30001, exception.getCode());
        assertEquals(TicketStatus.OPEN.name(), ticket.getStatus());
        verify(transitionMapper, never()).insert(any(TicketTransition.class));
    }

    @Test
    @DisplayName("不是当前处理人的用户不能接单")
    void shouldRejectAcceptByNonAssignee() {
        Long otherUserId = 30L;
        stubActiveUser(otherUserId);
        Ticket ticket = ticket(TicketStatus.ASSIGNED, 0);
        stubTicket(ticket);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> ticketService.transitionTicket(TICKET_ID, request("ACCEPT", otherUserId, 0, null))
        );

        assertEquals(30001, exception.getCode());
        assertTrue(exception.getMessage().contains("当前处理人"));
        assertEquals(TicketStatus.ASSIGNED.name(), ticket.getStatus());
    }

    @Test
    @DisplayName("不是创建人的用户不能确认解决")
    void shouldRejectConfirmByNonCreator() {
        Ticket ticket = ticket(TicketStatus.PENDING_CONFIRMATION, 3);
        stubTicket(ticket);
        stubActiveUser(ASSIGNEE_ID);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> ticketService.transitionTicket(TICKET_ID, request("CONFIRM", ASSIGNEE_ID, 3, null))
        );

        assertEquals(30001, exception.getCode());
        assertTrue(exception.getMessage().contains("创建人"));
        assertEquals(TicketStatus.PENDING_CONFIRMATION.name(), ticket.getStatus());
    }

    @Test
    @DisplayName("客户端使用旧版本时必须拒绝操作")
    void shouldRejectStaleVersion() {
        Ticket ticket = ticket(TicketStatus.ASSIGNED, 2);
        stubTicket(ticket);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> ticketService.transitionTicket(TICKET_ID, request("ACCEPT", ASSIGNEE_ID, 1, null))
        );

        assertEquals(30002, exception.getCode());
        assertTrue(exception.getMessage().contains("版本"));
        verify(userAccountService, never()).getUser(TENANT_ID, ASSIGNEE_ID);
        verify(transitionMapper, never()).insert(any(TicketTransition.class));
    }

    @Test
    @DisplayName("创建人驳回解决方案后应回到 PROCESSING")
    void shouldReturnToProcessingWhenResolutionRejected() {
        Ticket ticket = ticket(TicketStatus.PENDING_CONFIRMATION, 4);
        stubTicket(ticket);
        stubSuccessfulPersistence();
        stubActiveUser(CREATOR_ID);

        ticketService.transitionTicket(TICKET_ID, request("REJECT", CREATOR_ID, 4, null));

        assertEquals(TicketStatus.PROCESSING.name(), ticket.getStatus());
        assertEquals(5, ticket.getVersion());
        ArgumentCaptor<TicketTransition> captor = ArgumentCaptor.forClass(TicketTransition.class);
        verify(transitionMapper).insert(captor.capture());
        assertEquals("REJECT", captor.getValue().getAction());
        assertEquals(TicketStatus.PENDING_CONFIRMATION.name(), captor.getValue().getFromStatus());
        assertEquals(TicketStatus.PROCESSING.name(), captor.getValue().getToStatus());
    }

    private void stubTicket(Ticket ticket) {
        doReturn(ticket).when(ticketService).getTicket(TENANT_ID, TICKET_ID);
    }

    private void stubSuccessfulPersistence() {
        // 模拟 MyBatis-Plus 乐观锁更新成功，并让实体版本与真实运行行为一致地递增。
        doAnswer(invocation -> {
            Ticket ticket = invocation.getArgument(0);
            ticket.setVersion(ticket.getVersion() + 1);
            return true;
        }).when(ticketService).updateById(any(Ticket.class));
        when(transitionMapper.insert(any(TicketTransition.class))).thenReturn(1);
    }

    private void stubActiveUser(Long userId) {
        when(userAccountService.getUser(TENANT_ID, userId)).thenReturn(activeUser(userId));
    }

    private Ticket ticket(TicketStatus status, int version) {
        Ticket ticket = new Ticket();
        ticket.setId(TICKET_ID);
        ticket.setTenantId(TENANT_ID);
        ticket.setCreatorId(CREATOR_ID);
        ticket.setCurrentAssigneeId(ASSIGNEE_ID);
        ticket.setStatus(status.name());
        ticket.setVersion(version);
        ticket.setResponseDueAt(LocalDateTime.now().plusHours(1));
        ticket.setResolutionDueAt(LocalDateTime.now().plusHours(4));
        ticket.setResponseSlaStatus("PENDING");
        ticket.setResolutionSlaStatus("PENDING");
        return ticket;
    }

    private UserAccount activeUser(Long userId) {
        UserAccount user = new UserAccount();
        user.setId(userId);
        user.setTenantId(TENANT_ID);
        user.setStatus("ACTIVE");
        return user;
    }

    private TicketTransitionRequest request(String action, Long operatorId, int version, String resolution) {
        when(tenantContext.userId()).thenReturn(operatorId);
        TicketTransitionRequest request = new TicketTransitionRequest();
        request.setAction(action);
        request.setVersion(version);
        request.setResolution(resolution);
        request.setRemark("自动化测试");
        return request;
    }
}
