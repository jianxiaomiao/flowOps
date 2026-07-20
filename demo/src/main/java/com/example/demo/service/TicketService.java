package com.example.demo.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.example.demo.dto.ticket.*;
import com.example.demo.entity.*;
import java.util.List;

/**
 * 工单应用服务契约，定义工单查询、分派、状态流转和评论用例。
 */
public interface TicketService extends IService<Ticket> {
    /** @param request 创建数据。@return 新建工单。 */
    Ticket createTicket(TicketCreateRequest request);
    /** @param tenantId 租户 ID。@param ticketId 工单 ID。@return 指定租户中的工单。 */
    Ticket getTicket(Long tenantId, Long ticketId);
    /** @param query 分页及筛选条件。@return 当前用户范围内的工单分页结果。 */
    Page<Ticket> pageTickets(TicketPageQuery query);
    /** @param ticketId 工单 ID。@param request 分派数据。@return 分派后的工单。 */
    Ticket assignTicket(Long ticketId, TicketAssignmentRequest request);
    /** @param ticketId 工单 ID。@param request 状态动作数据。@return 流转后的工单。 */
    Ticket transitionTicket(Long ticketId, TicketTransitionRequest request);
    /** @param ticketId 工单 ID。@param request 评论数据。@return 新建评论。 */
    TicketComment addComment(Long ticketId, TicketCommentCreateRequest request);
    /** @param tenantId 租户 ID。@param ticketId 工单 ID。@return 状态流转历史。 */
    List<TicketTransition> listTransitions(Long tenantId, Long ticketId);
    /** @param tenantId 租户 ID。@param ticketId 工单 ID。@return 分派历史。 */
    List<TicketAssignment> listAssignments(Long tenantId, Long ticketId);
    /** @param tenantId 租户 ID。@param ticketId 工单 ID。@return 评论列表。 */
    List<TicketComment> listComments(Long tenantId, Long ticketId);
}
