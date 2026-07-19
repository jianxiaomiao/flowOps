package com.example.demo.converter;

import com.example.demo.dto.ticket.TicketCreateRequest;
import com.example.demo.entity.Ticket;
import com.example.demo.entity.TicketAssignment;
import com.example.demo.entity.TicketComment;
import com.example.demo.entity.TicketTransition;
import com.example.demo.vo.TicketAssignmentResponse;
import com.example.demo.vo.TicketCommentResponse;
import com.example.demo.vo.TicketResponse;
import com.example.demo.vo.TicketTransitionResponse;
import org.springframework.beans.BeanUtils;

public final class TicketConverter {
    private TicketConverter() { }
    public static Ticket toEntity(TicketCreateRequest source) { Ticket target = new Ticket(); BeanUtils.copyProperties(source, target); return target; }
    public static TicketResponse toResponse(Ticket source) { TicketResponse target = new TicketResponse(); BeanUtils.copyProperties(source, target); return target; }
    public static TicketTransitionResponse toResponse(TicketTransition source) { TicketTransitionResponse target = new TicketTransitionResponse(); BeanUtils.copyProperties(source, target); return target; }
    public static TicketAssignmentResponse toResponse(TicketAssignment source) { TicketAssignmentResponse target = new TicketAssignmentResponse(); BeanUtils.copyProperties(source, target); return target; }
    public static TicketCommentResponse toResponse(TicketComment source) { TicketCommentResponse target = new TicketCommentResponse(); BeanUtils.copyProperties(source, target); return target; }
}
