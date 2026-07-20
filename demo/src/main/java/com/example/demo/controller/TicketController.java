package com.example.demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.common.Result;
import com.example.demo.converter.TicketConverter;
import com.example.demo.dto.ticket.*;
import com.example.demo.service.TicketService;
import com.example.demo.security.TenantContext;
import com.example.demo.vo.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 工单 HTTP 接口入口。
 *
 * <p>Controller 只完成请求校验、权限声明、DTO/VO 转换；状态机、事务和历史记录均交给 {@code TicketService}。</p>
 */
@Validated
@RestController
@RequestMapping("/ticket")
@RequiredArgsConstructor
public class TicketController {
    private final TicketService ticketService;
    private final TenantContext tenantContext;

    /**
     * 创建一张状态为 OPEN 的工单，并快照当前 SLA 截止时间。
     *
     * @param request 创建工单所需的标题、类型、优先级和处理部门。
     * @return 新建工单的响应数据。
     */
    @PostMapping
    @PreAuthorize("hasAuthority('ticket:create')")
    public Result<TicketResponse> create(@Valid @RequestBody TicketCreateRequest request) {
        return Result.success(TicketConverter.toResponse(ticketService.createTicket(request)));
    }
    /**
     * 按 ID 查询一张当前用户有数据范围权限的工单。
     *
     * @param id 工单 ID。
     * @return 工单详情。
     */
    @GetMapping("/{id}")
    @PreAuthorize("@ticketReadScope.canRead(#id)")
    public Result<TicketResponse> get(@PathVariable @Positive Long id) {
        return Result.success(TicketConverter.toResponse(ticketService.getTicket(tenantContext.tenantId(), id)));
    }
    /**
     * 分页查询当前用户数据范围内的工单。
     *
     * @param query 分页、状态、优先级等可选查询条件。
     * @return 分页后的工单响应数据。
     */
    @GetMapping("/page")
    @PreAuthorize("hasAnyAuthority('ticket:read','ticket:read:own','ticket:read:department')")
    public Result<IPage<TicketResponse>> page(@Valid @ModelAttribute TicketPageQuery query) {
        return Result.success(ticketService.pageTickets(query).convert(TicketConverter::toResponse));
    }
    /**
     * 将工单分派或重新分派给目标部门和可选处理人。
     *
     * @param id 工单 ID。
     * @param request 目标部门、目标处理人、备注和乐观锁版本。
     * @return 分派后的工单当前快照。
     */
    @PostMapping("/{id}/assignments")
    @PreAuthorize("hasAuthority('ticket:assign') and @ticketReadScope.inCurrentDepartment(#id)")
    public Result<TicketResponse> assign(@PathVariable @Positive Long id, @Valid @RequestBody TicketAssignmentRequest request) {
        return Result.success(TicketConverter.toResponse(ticketService.assignTicket(id, request)));
    }
    /**
     * 执行一个受状态机约束的工单动作。
     *
     * @param id 工单 ID。
     * @param request 动作、备注、解决方案和乐观锁版本。
     * @return 流转后的工单当前快照。
     */
    @PostMapping("/{id}/transitions")
    @PreAuthorize("@rbacGuard.canTransition(#request.action)")
    public Result<TicketResponse> transition(@PathVariable @Positive Long id, @Valid @RequestBody TicketTransitionRequest request) {
        return Result.success(TicketConverter.toResponse(ticketService.transitionTicket(id, request)));
    }
    /**
     * 为当前用户可见的工单追加评论。
     *
     * @param id 工单 ID。
     * @param request 评论正文和可见性。
     * @return 新增评论。
     */
    @PostMapping("/{id}/comments")
    @PreAuthorize("hasAuthority('ticket:update') and @ticketReadScope.canRead(#id)")
    public Result<TicketCommentResponse> comment(@PathVariable @Positive Long id, @Valid @RequestBody TicketCommentCreateRequest request) {
        return Result.success(TicketConverter.toResponse(ticketService.addComment(id, request)));
    }
    /**
     * 查询工单的不可变状态流转历史。
     *
     * @param id 工单 ID。
     * @return 按发生时间升序排列的流转记录。
     */
    @GetMapping("/{id}/transitions")
    @PreAuthorize("@ticketReadScope.canRead(#id)")
    public Result<List<TicketTransitionResponse>> transitions(@PathVariable @Positive Long id) {
        return Result.success(ticketService.listTransitions(tenantContext.tenantId(), id).stream().map(TicketConverter::toResponse).toList());
    }
    /**
     * 查询工单的分派历史。
     *
     * @param id 工单 ID。
     * @return 按发生时间升序排列的分派记录。
     */
    @GetMapping("/{id}/assignments")
    @PreAuthorize("@ticketReadScope.canRead(#id)")
    public Result<List<TicketAssignmentResponse>> assignments(@PathVariable @Positive Long id) {
        return Result.success(ticketService.listAssignments(tenantContext.tenantId(), id).stream().map(TicketConverter::toResponse).toList());
    }
    /**
     * 查询工单评论。
     *
     * @param id 工单 ID。
     * @return 按发表时间升序排列的评论记录。
     */
    @GetMapping("/{id}/comments")
    @PreAuthorize("@ticketReadScope.canRead(#id)")
    public Result<List<TicketCommentResponse>> comments(@PathVariable @Positive Long id) {
        return Result.success(ticketService.listComments(tenantContext.tenantId(), id).stream().map(TicketConverter::toResponse).toList());
    }
}
