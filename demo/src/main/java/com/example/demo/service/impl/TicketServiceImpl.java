package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.example.demo.common.BusinessException;
import com.example.demo.converter.TicketConverter;
import com.example.demo.dto.ticket.*;
import com.example.demo.entity.*;
import com.example.demo.mapper.*;
import com.example.demo.service.SlaPolicyService;
import com.example.demo.service.TicketService;
import com.example.demo.service.UserAccountService;
import com.example.demo.ticket.TicketAction;
import com.example.demo.ticket.TicketNumberGenerator;
import com.example.demo.ticket.TicketStatus;
import com.example.demo.validator.TenantScopeValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** 工单用例编排：修改当前快照时，同一事务写入不可变历史。 */
@Service
public class TicketServiceImpl extends ServiceImpl<TicketMapper, Ticket> implements TicketService {
    private static final String ACTIVE = "ACTIVE";
    private static final Set<String> TICKET_TYPES = Set.of("INCIDENT", "ACCESS_REQUEST", "REPAIR", "CUSTOMER_ISSUE", "BUG", "APPROVAL");
    private final TenantScopeValidator tenantScopeValidator;
    private final UserAccountService userAccountService;
    private final SlaPolicyService slaPolicyService;
    private final TicketNumberGenerator ticketNumberGenerator;
    private final TicketTransitionMapper transitionMapper;
    private final TicketAssignmentMapper assignmentMapper;
    private final TicketCommentMapper commentMapper;

    public TicketServiceImpl(TenantScopeValidator tenantScopeValidator, UserAccountService userAccountService, SlaPolicyService slaPolicyService,
                             TicketNumberGenerator ticketNumberGenerator, TicketTransitionMapper transitionMapper,
                             TicketAssignmentMapper assignmentMapper, TicketCommentMapper commentMapper) {
        this.tenantScopeValidator = tenantScopeValidator; this.userAccountService = userAccountService; this.slaPolicyService = slaPolicyService;
        this.ticketNumberGenerator = ticketNumberGenerator; this.transitionMapper = transitionMapper; this.assignmentMapper = assignmentMapper; this.commentMapper = commentMapper;
    }

    @Override @Transactional
    public Ticket createTicket(TicketCreateRequest request) {
        Long tenantId = requirePositive(request.getTenantId(), "tenantId");
        tenantScopeValidator.requireActiveTenant(tenantId);
        UserAccount creator = userAccountService.getUser(tenantId, requirePositive(request.getCreatorId(), "creatorId"));
        if (!ACTIVE.equals(creator.getStatus())) throw new BusinessException(30001, "已停用用户不能创建工单");
        tenantScopeValidator.requireActiveDepartment(tenantId, request.getHandlingDepartmentId());
        SlaPolicy policy = slaPolicyService.getEnabledPolicy(tenantId, request.getPriority());
        Ticket ticket = TicketConverter.toEntity(request);
        LocalDateTime now = LocalDateTime.now();
        ticket.setTicketNo(ticketNumberGenerator.nextTicketNo(tenantId));
        ticket.setTicketType(normalizeType(request.getTicketType())); ticket.setPriority(policy.getPriority()); ticket.setStatus(TicketStatus.OPEN.name());
        ticket.setCreatorDepartmentId(creator.getDepartmentId()); ticket.setCurrentAssigneeId(null);
        ticket.setResponseDueAt(now.plusMinutes(policy.getResponseMinutes())); ticket.setResolutionDueAt(now.plusMinutes(policy.getResolutionMinutes()));
        ticket.setResponseSlaStatus("PENDING"); ticket.setResolutionSlaStatus("PENDING"); ticket.setUpdatedBy(creator.getId());
        if (!save(ticket)) throw new IllegalStateException("创建工单失败");
        writeTransition(ticket, TicketAction.CREATE, "NONE", creator.getId(), "创建工单");
        return ticket;
    }

    @Override public Ticket getTicket(Long tenantId, Long ticketId) {
        Ticket ticket = lambdaQuery().eq(Ticket::getTenantId, requirePositive(tenantId, "tenantId")).eq(Ticket::getId, requirePositive(ticketId, "ticketId")).one();
        if (ticket == null) throw new BusinessException(20001, "工单不存在");
        return ticket;
    }

    @Override public Page<Ticket> pageTickets(TicketPageQuery query) {
        return page(new Page<>(query.getPageNum(), query.getPageSize()), Wrappers.lambdaQuery(Ticket.class)
                .eq(Ticket::getTenantId, requirePositive(query.getTenantId(), "tenantId"))
                .eq(StringUtils.hasText(query.getStatus()), Ticket::getStatus, query.getStatus())
                .eq(StringUtils.hasText(query.getPriority()), Ticket::getPriority, query.getPriority())
                .eq(query.getCreatorId() != null, Ticket::getCreatorId, query.getCreatorId())
                .eq(query.getAssigneeId() != null, Ticket::getCurrentAssigneeId, query.getAssigneeId())
                .eq(query.getHandlingDepartmentId() != null, Ticket::getHandlingDepartmentId, query.getHandlingDepartmentId())
                .like(StringUtils.hasText(query.getTitle()), Ticket::getTitle, query.getTitle()).orderByDesc(Ticket::getUpdatedAt));
    }

    @Override @Transactional
    public Ticket assignTicket(Long ticketId, TicketAssignmentRequest request) {
        Ticket ticket = getTicket(request.getTenantId(), ticketId); assertVersion(ticket, request.getVersion());
        requireActiveOperator(request.getTenantId(), request.getOperatorId()); tenantScopeValidator.requireActiveDepartment(request.getTenantId(), request.getToDepartmentId());
        if (!(TicketStatus.OPEN.name().equals(ticket.getStatus()) || TicketStatus.ASSIGNED.name().equals(ticket.getStatus()))) throw new BusinessException(30001, "当前状态不能分派工单");
        UserAccount assignee = null;
        if (request.getToAssigneeId() != null) {
            assignee = userAccountService.getUser(request.getTenantId(), request.getToAssigneeId());
            if (!ACTIVE.equals(assignee.getStatus()) || !request.getToDepartmentId().equals(assignee.getDepartmentId())) throw new BusinessException(30001, "处理人必须是目标部门中的启用用户");
        }
        String fromStatus = ticket.getStatus(); Long fromDepartmentId = ticket.getHandlingDepartmentId(); Long fromAssigneeId = ticket.getCurrentAssigneeId();
        ticket.setHandlingDepartmentId(request.getToDepartmentId()); ticket.setCurrentAssigneeId(request.getToAssigneeId()); ticket.setStatus(TicketStatus.ASSIGNED.name()); ticket.setUpdatedBy(request.getOperatorId());
        updateTicket(ticket);
        TicketAssignment assignment = new TicketAssignment(); assignment.setTenantId(ticket.getTenantId()); assignment.setTicketId(ticket.getId());
        assignment.setAssignmentType(fromAssigneeId == null ? "ASSIGN" : "REASSIGN"); assignment.setFromDepartmentId(fromDepartmentId); assignment.setToDepartmentId(request.getToDepartmentId());
        assignment.setFromAssigneeId(fromAssigneeId); assignment.setToAssigneeId(request.getToAssigneeId()); assignment.setAssignedBy(request.getOperatorId()); assignment.setReason(request.getReason()); assignment.setTicketVersion(ticket.getVersion()); assignment.setCreatedAt(LocalDateTime.now());
        assignmentMapper.insert(assignment);
        if (!fromStatus.equals(ticket.getStatus())) writeTransition(ticket, TicketAction.ASSIGN, fromStatus, request.getOperatorId(), request.getReason());
        return ticket;
    }

    @Override @Transactional
    public Ticket transitionTicket(Long ticketId, TicketTransitionRequest request) {
        Ticket ticket = getTicket(request.getTenantId(), ticketId); assertVersion(ticket, request.getVersion()); requireActiveOperator(request.getTenantId(), request.getOperatorId());
        TicketAction action = parseAction(request.getAction()); String fromStatus = ticket.getStatus(); LocalDateTime now = LocalDateTime.now();
        switch (action) {
            case ACCEPT -> { requireStatus(ticket, TicketStatus.ASSIGNED); requireAssignee(ticket, request.getOperatorId()); ticket.setStatus(TicketStatus.ACCEPTED.name()); ticket.setFirstResponseAt(now); markResponseSla(ticket, now); }
            case START_PROCESS -> { requireStatus(ticket, TicketStatus.ACCEPTED); requireAssignee(ticket, request.getOperatorId()); ticket.setStatus(TicketStatus.PROCESSING.name()); }
            case SUBMIT_RESOLUTION -> { requireStatus(ticket, TicketStatus.PROCESSING); requireAssignee(ticket, request.getOperatorId()); if (!StringUtils.hasText(request.getResolution())) throw new BusinessException(10001, "提交解决方案时 resolution 不能为空"); ticket.setStatus(TicketStatus.PENDING_CONFIRMATION.name()); ticket.setResolution(request.getResolution().trim()); ticket.setResolutionSubmittedAt(now); }
            case CONFIRM -> { requireStatus(ticket, TicketStatus.PENDING_CONFIRMATION); requireCreator(ticket, request.getOperatorId()); ticket.setStatus(TicketStatus.RESOLVED.name()); ticket.setResolvedAt(now); markResolutionSla(ticket, now); }
            case REJECT -> { requireStatus(ticket, TicketStatus.PENDING_CONFIRMATION); requireCreator(ticket, request.getOperatorId()); ticket.setStatus(TicketStatus.PROCESSING.name()); }
            case CLOSE -> { requireStatus(ticket, TicketStatus.RESOLVED); requireCreator(ticket, request.getOperatorId()); ticket.setStatus(TicketStatus.CLOSED.name()); ticket.setClosedAt(now); }
            case CREATE, ASSIGN -> throw new BusinessException(10001, "该动作不能通过状态流转接口执行");
        }
        ticket.setUpdatedBy(request.getOperatorId()); updateTicket(ticket); writeTransition(ticket, action, fromStatus, request.getOperatorId(), request.getRemark());
        return ticket;
    }

    @Override @Transactional
    public TicketComment addComment(Long ticketId, TicketCommentCreateRequest request) {
        Ticket ticket = getTicket(request.getTenantId(), ticketId); UserAccount author = userAccountService.getUser(request.getTenantId(), request.getAuthorId());
        if (!ACTIVE.equals(author.getStatus())) throw new BusinessException(30001, "已停用用户不能评论");
        String visibility = request.getVisibility().trim().toUpperCase(Locale.ROOT);
        if (!("PUBLIC".equals(visibility) || "INTERNAL".equals(visibility))) throw new BusinessException(10001, "visibility 仅支持 PUBLIC 或 INTERNAL");
        TicketComment comment = new TicketComment(); comment.setTenantId(ticket.getTenantId()); comment.setTicketId(ticket.getId()); comment.setAuthorId(author.getId()); comment.setContent(request.getContent().trim()); comment.setVisibility(visibility); comment.setCreatedAt(LocalDateTime.now());
        if (commentMapper.insert(comment) != 1) throw new IllegalStateException("发表评论失败"); return comment;
    }

    @Override public List<TicketTransition> listTransitions(Long tenantId, Long ticketId) { getTicket(tenantId, ticketId); return transitionMapper.selectList(Wrappers.lambdaQuery(TicketTransition.class).eq(TicketTransition::getTenantId, tenantId).eq(TicketTransition::getTicketId, ticketId).orderByAsc(TicketTransition::getCreatedAt)); }
    @Override public List<TicketAssignment> listAssignments(Long tenantId, Long ticketId) { getTicket(tenantId, ticketId); return assignmentMapper.selectList(Wrappers.lambdaQuery(TicketAssignment.class).eq(TicketAssignment::getTenantId, tenantId).eq(TicketAssignment::getTicketId, ticketId).orderByAsc(TicketAssignment::getCreatedAt)); }
    @Override public List<TicketComment> listComments(Long tenantId, Long ticketId) { getTicket(tenantId, ticketId); return commentMapper.selectList(Wrappers.lambdaQuery(TicketComment.class).eq(TicketComment::getTenantId, tenantId).eq(TicketComment::getTicketId, ticketId).orderByAsc(TicketComment::getCreatedAt)); }

    private void updateTicket(Ticket ticket) { if (!updateById(ticket)) throw new BusinessException(30002, "工单已被其他操作修改，请刷新后重试"); }
    private void writeTransition(Ticket ticket, TicketAction action, String fromStatus, Long operatorId, String remark) { TicketTransition transition = new TicketTransition(); transition.setTenantId(ticket.getTenantId()); transition.setTicketId(ticket.getId()); transition.setAction(action.name()); transition.setFromStatus(fromStatus); transition.setToStatus(ticket.getStatus()); transition.setOperatorId(operatorId); transition.setRemark(remark); transition.setTicketVersion(ticket.getVersion()); transition.setCreatedAt(LocalDateTime.now()); transitionMapper.insert(transition); }
    private void requireActiveOperator(Long tenantId, Long operatorId) { UserAccount operator = userAccountService.getUser(tenantId, requirePositive(operatorId, "operatorId")); if (!ACTIVE.equals(operator.getStatus())) throw new BusinessException(30001, "已停用用户不能操作工单"); }
    private void assertVersion(Ticket ticket, Integer version) { if (version == null || !version.equals(ticket.getVersion())) throw new BusinessException(30002, "工单版本已变化，请刷新后重试"); }
    private void requireStatus(Ticket ticket, TicketStatus expected) { if (!expected.name().equals(ticket.getStatus())) throw new BusinessException(30001, "当前工单状态不允许执行此操作"); }
    private void requireAssignee(Ticket ticket, Long operatorId) { if (!operatorId.equals(ticket.getCurrentAssigneeId())) throw new BusinessException(30001, "只有当前处理人可以执行此操作"); }
    private void requireCreator(Ticket ticket, Long operatorId) { if (!operatorId.equals(ticket.getCreatorId())) throw new BusinessException(30001, "只有工单创建人可以执行此操作"); }
    private void markResponseSla(Ticket ticket, LocalDateTime now) { if (now.isAfter(ticket.getResponseDueAt())) { ticket.setResponseSlaStatus("BREACHED"); ticket.setResponseBreachedAt(now); } else ticket.setResponseSlaStatus("MET"); }
    private void markResolutionSla(Ticket ticket, LocalDateTime now) { if (now.isAfter(ticket.getResolutionDueAt())) { ticket.setResolutionSlaStatus("BREACHED"); ticket.setResolutionBreachedAt(now); } else ticket.setResolutionSlaStatus("MET"); }
    private TicketAction parseAction(String raw) { try { return TicketAction.valueOf(raw.trim().toUpperCase(Locale.ROOT)); } catch (Exception ex) { throw new BusinessException(10001, "不支持的工单动作"); } }
    private String normalizeType(String raw) { if (!StringUtils.hasText(raw)) throw new BusinessException(10001, "ticketType 不能为空"); String value = raw.trim().toUpperCase(Locale.ROOT); if (!TICKET_TYPES.contains(value)) throw new BusinessException(10001, "不支持的 ticketType"); return value; }
    private Long requirePositive(Long value, String field) { if (value == null || value <= 0) throw new BusinessException(10001, field + " 必须是正整数"); return value; }
}
