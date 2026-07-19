package com.example.demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.common.Result;
import com.example.demo.converter.TicketConverter;
import com.example.demo.dto.ticket.*;
import com.example.demo.service.TicketService;
import com.example.demo.vo.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/** 工单入口：接口提交动作，状态决定与历史写入交给 Service。 */
@Validated
@RestController
@RequestMapping("/ticket")
@RequiredArgsConstructor
public class TicketController {
    private final TicketService ticketService;

    @PostMapping
    public Result<TicketResponse> create(@Valid @RequestBody TicketCreateRequest request) {
        return Result.success(TicketConverter.toResponse(ticketService.createTicket(request)));
    }
    @GetMapping("/{id}")
    public Result<TicketResponse> get(@PathVariable @Positive Long id, @RequestParam @Positive Long tenantId) {
        return Result.success(TicketConverter.toResponse(ticketService.getTicket(tenantId, id)));
    }
    @GetMapping("/page")
    public Result<IPage<TicketResponse>> page(@Valid @ModelAttribute TicketPageQuery query) {
        return Result.success(ticketService.pageTickets(query).convert(TicketConverter::toResponse));
    }
    @PostMapping("/{id}/assignments")
    public Result<TicketResponse> assign(@PathVariable @Positive Long id, @Valid @RequestBody TicketAssignmentRequest request) {
        return Result.success(TicketConverter.toResponse(ticketService.assignTicket(id, request)));
    }
    @PostMapping("/{id}/transitions")
    public Result<TicketResponse> transition(@PathVariable @Positive Long id, @Valid @RequestBody TicketTransitionRequest request) {
        return Result.success(TicketConverter.toResponse(ticketService.transitionTicket(id, request)));
    }
    @PostMapping("/{id}/comments")
    public Result<TicketCommentResponse> comment(@PathVariable @Positive Long id, @Valid @RequestBody TicketCommentCreateRequest request) {
        return Result.success(TicketConverter.toResponse(ticketService.addComment(id, request)));
    }
    @GetMapping("/{id}/transitions")
    public Result<List<TicketTransitionResponse>> transitions(@PathVariable @Positive Long id, @RequestParam @Positive Long tenantId) {
        return Result.success(ticketService.listTransitions(tenantId, id).stream().map(TicketConverter::toResponse).toList());
    }
    @GetMapping("/{id}/assignments")
    public Result<List<TicketAssignmentResponse>> assignments(@PathVariable @Positive Long id, @RequestParam @Positive Long tenantId) {
        return Result.success(ticketService.listAssignments(tenantId, id).stream().map(TicketConverter::toResponse).toList());
    }
    @GetMapping("/{id}/comments")
    public Result<List<TicketCommentResponse>> comments(@PathVariable @Positive Long id, @RequestParam @Positive Long tenantId) {
        return Result.success(ticketService.listComments(tenantId, id).stream().map(TicketConverter::toResponse).toList());
    }
}
