package com.example.demo.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.example.demo.dto.ticket.*;
import com.example.demo.entity.*;
import java.util.List;

public interface TicketService extends IService<Ticket> {
    Ticket createTicket(TicketCreateRequest request);
    Ticket getTicket(Long tenantId, Long ticketId);
    Page<Ticket> pageTickets(TicketPageQuery query);
    Ticket assignTicket(Long ticketId, TicketAssignmentRequest request);
    Ticket transitionTicket(Long ticketId, TicketTransitionRequest request);
    TicketComment addComment(Long ticketId, TicketCommentCreateRequest request);
    List<TicketTransition> listTransitions(Long tenantId, Long ticketId);
    List<TicketAssignment> listAssignments(Long tenantId, Long ticketId);
    List<TicketComment> listComments(Long tenantId, Long ticketId);
}
