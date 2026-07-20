package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.example.demo.common.BusinessException;
import com.example.demo.audit.AuditOperation;
import com.example.demo.converter.TicketConverter;
import com.example.demo.dto.ticket.*;
import com.example.demo.entity.*;
import com.example.demo.mapper.*;
import com.example.demo.service.SlaPolicyService;
import com.example.demo.service.TicketService;
import com.example.demo.service.UserAccountService;
import com.example.demo.security.TenantContext;
import com.example.demo.security.TicketReadScope;
import com.example.demo.messaging.OutboxEventService;
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
    private final TenantContext tenantContext;
    private final OutboxEventService outboxEventService;
    private final TicketReadScope ticketReadScope;

    public TicketServiceImpl(TenantScopeValidator tenantScopeValidator, UserAccountService userAccountService, SlaPolicyService slaPolicyService,
                             TicketNumberGenerator ticketNumberGenerator, TicketTransitionMapper transitionMapper,
                             TicketAssignmentMapper assignmentMapper, TicketCommentMapper commentMapper,
                             TenantContext tenantContext, OutboxEventService outboxEventService,
                             TicketReadScope ticketReadScope) {
        this.tenantScopeValidator = tenantScopeValidator; this.userAccountService = userAccountService; this.slaPolicyService = slaPolicyService;
        this.ticketNumberGenerator = ticketNumberGenerator; this.transitionMapper = transitionMapper; this.assignmentMapper = assignmentMapper; this.commentMapper = commentMapper;
        this.tenantContext = tenantContext;
        this.outboxEventService = outboxEventService;
        this.ticketReadScope = ticketReadScope;
    }

    @Override @Transactional
    @AuditOperation(module = "TICKET", operation = "CREATE", resourceType = "TICKET")
    /**
     * 创建工单，并在同一事务内完成编号分配、SLA 快照、初始历史和 Outbox 事件写入。
     *
     * @param request 来自创建接口的可控输入；租户和创建人始终取自可信 JWT。
     * @return 已持久化的工单当前快照。
     * @throws BusinessException 当租户、处理部门、创建人状态、SLA 策略或工单类型不合法时抛出。
     */
    public Ticket createTicket(TicketCreateRequest request) {
        Long tenantId = tenantContext.tenantId();
        Long operatorId = tenantContext.userId();
        tenantScopeValidator.requireActiveTenant(tenantId);
        UserAccount creator = userAccountService.getUser(tenantId, operatorId);
        if (!ACTIVE.equals(creator.getStatus())) throw new BusinessException(30001, "已停用用户不能创建工单");
        tenantScopeValidator.requireActiveDepartment(tenantId, request.getHandlingDepartmentId());
        SlaPolicy policy = slaPolicyService.getEnabledPolicy(tenantId, request.getPriority());
        Ticket ticket = TicketConverter.toEntity(request);
        ticket.setTenantId(tenantId); ticket.setCreatorId(operatorId);
        LocalDateTime now = LocalDateTime.now();
        ticket.setTicketNo(ticketNumberGenerator.nextTicketNo(tenantId));
        ticket.setTicketType(normalizeType(request.getTicketType())); ticket.setPriority(policy.getPriority()); ticket.setStatus(TicketStatus.OPEN.name());
        ticket.setCreatorDepartmentId(creator.getDepartmentId()); ticket.setCurrentAssigneeId(null);
        ticket.setResponseDueAt(now.plusMinutes(policy.getResponseMinutes())); ticket.setResolutionDueAt(now.plusMinutes(policy.getResolutionMinutes()));
        ticket.setResponseSlaStatus("PENDING"); ticket.setResolutionSlaStatus("PENDING"); ticket.setUpdatedBy(creator.getId());
        if (!save(ticket)) throw new IllegalStateException("创建工单失败");
        writeTransition(ticket, TicketAction.CREATE, "NONE", creator.getId(), "创建工单");
        outboxEventService.enqueueTicketEvent(ticket, "TICKET_CREATED", List.of(creator.getId()));
        return ticket;
    }

    @Override
    /**
     * 在指定租户范围内读取工单，防止仅凭 ID 跨租户查询。
     *
     * @param tenantId 可信租户 ID。
     * @param ticketId 工单 ID。
     * @return 指定工单。
     * @throws BusinessException 当参数不是正整数或工单不存在时抛出。
     */
    public Ticket getTicket(Long tenantId, Long ticketId) {
        Ticket ticket = lambdaQuery().eq(Ticket::getTenantId, requirePositive(tenantId, "tenantId")).eq(Ticket::getId, requirePositive(ticketId, "ticketId")).one();
        if (ticket == null) throw new BusinessException(20001, "工单不存在");
        return ticket;
    }

    @Override
    /**
     * 使用筛选条件和当前用户的数据范围构建分页查询。
     *
     * @param query 分页及可选筛选条件。
     * @return 仅包含当前用户有权读取数据的工单分页。
     */
    public Page<Ticket> pageTickets(TicketPageQuery query) {
        var wrapper = Wrappers.lambdaQuery(Ticket.class)
                .eq(Ticket::getTenantId, tenantContext.tenantId())
                .eq(StringUtils.hasText(query.getStatus()), Ticket::getStatus, query.getStatus())
                .eq(StringUtils.hasText(query.getPriority()), Ticket::getPriority, query.getPriority())
                .eq(query.getCreatorId() != null, Ticket::getCreatorId, query.getCreatorId())
                .eq(query.getAssigneeId() != null, Ticket::getCurrentAssigneeId, query.getAssigneeId())
                .eq(query.getHandlingDepartmentId() != null, Ticket::getHandlingDepartmentId, query.getHandlingDepartmentId())
                .like(StringUtils.hasText(query.getTitle()), Ticket::getTitle, query.getTitle()).orderByDesc(Ticket::getUpdatedAt);
        ticketReadScope.applyTo(wrapper);
        return page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
    }

    @Override @Transactional
    @AuditOperation(module = "TICKET", operation = "ASSIGN", resourceType = "TICKET")
    /**
     * 分派工单，同时记录分派历史、状态历史并写入通知 Outbox 事件。
     *
     * @param ticketId 工单 ID。
     * @param request 目标部门、可选处理人、原因和当前乐观锁版本。
     * @return 分派后的工单。
     * @throws BusinessException 当版本过期、状态不允许分派或处理人不属于目标部门时抛出。
     */
    public Ticket assignTicket(Long ticketId, TicketAssignmentRequest request) {
        Long tenantId = tenantContext.tenantId(); Long operatorId = tenantContext.userId();
        Ticket ticket = getTicket(tenantId, ticketId); assertVersion(ticket, request.getVersion());
        requireActiveOperator(tenantId, operatorId); tenantScopeValidator.requireActiveDepartment(tenantId, request.getToDepartmentId());
        if (!(TicketStatus.OPEN.name().equals(ticket.getStatus()) || TicketStatus.ASSIGNED.name().equals(ticket.getStatus()))) throw new BusinessException(30001, "当前状态不能分派工单");
        UserAccount assignee = null;
        if (request.getToAssigneeId() != null) {
            assignee = userAccountService.getUser(tenantId, request.getToAssigneeId());
            if (!ACTIVE.equals(assignee.getStatus()) || !request.getToDepartmentId().equals(assignee.getDepartmentId())) throw new BusinessException(30001, "处理人必须是目标部门中的启用用户");
        }
        String fromStatus = ticket.getStatus(); Long fromDepartmentId = ticket.getHandlingDepartmentId(); Long fromAssigneeId = ticket.getCurrentAssigneeId();
        ticket.setHandlingDepartmentId(request.getToDepartmentId()); ticket.setCurrentAssigneeId(request.getToAssigneeId()); ticket.setStatus(TicketStatus.ASSIGNED.name()); ticket.setUpdatedBy(operatorId);
        updateTicket(ticket);
        TicketAssignment assignment = new TicketAssignment(); assignment.setTenantId(ticket.getTenantId()); assignment.setTicketId(ticket.getId());
        assignment.setAssignmentType(fromAssigneeId == null ? "ASSIGN" : "REASSIGN"); assignment.setFromDepartmentId(fromDepartmentId); assignment.setToDepartmentId(request.getToDepartmentId());
        assignment.setFromAssigneeId(fromAssigneeId); assignment.setToAssigneeId(request.getToAssigneeId()); assignment.setAssignedBy(operatorId); assignment.setReason(request.getReason()); assignment.setTicketVersion(ticket.getVersion()); assignment.setCreatedAt(LocalDateTime.now());
        assignmentMapper.insert(assignment);
        if (!fromStatus.equals(ticket.getStatus())) writeTransition(ticket, TicketAction.ASSIGN, fromStatus, operatorId, request.getReason());
        outboxEventService.enqueueTicketEvent(ticket, "TICKET_ASSIGNED",
                List.of(request.getToAssigneeId() == null ? ticket.getCreatorId() : request.getToAssigneeId()));
        return ticket;
    }

    @Override @Transactional
    @AuditOperation(module = "TICKET", operation = "TRANSITION", resourceType = "TICKET")
    /**
     * 根据请求动作驱动工单状态机，而非允许客户端直接提交目标状态。
     *
     * @param ticketId 工单 ID。
     * @param request 动作、可选解决方案、备注和乐观锁版本。
     * @return 流转后的工单。
     * @throws BusinessException 当动作不支持、状态不匹配、操作人不正确或版本冲突时抛出。
     */
    public Ticket transitionTicket(Long ticketId, TicketTransitionRequest request) {
        Long tenantId = tenantContext.tenantId(); Long operatorId = tenantContext.userId();
        Ticket ticket = getTicket(tenantId, ticketId); assertVersion(ticket, request.getVersion()); requireActiveOperator(tenantId, operatorId);
        TicketAction action = parseAction(request.getAction()); String fromStatus = ticket.getStatus(); LocalDateTime now = LocalDateTime.now();
        switch (action) {
            case ACCEPT -> { requireStatus(ticket, TicketStatus.ASSIGNED); requireAssignee(ticket, operatorId); ticket.setStatus(TicketStatus.ACCEPTED.name()); ticket.setFirstResponseAt(now); markResponseSla(ticket, now); }
            case START_PROCESS -> { requireStatus(ticket, TicketStatus.ACCEPTED); requireAssignee(ticket, operatorId); ticket.setStatus(TicketStatus.PROCESSING.name()); }
            case SUBMIT_RESOLUTION -> { requireStatus(ticket, TicketStatus.PROCESSING); requireAssignee(ticket, operatorId); if (!StringUtils.hasText(request.getResolution())) throw new BusinessException(10001, "提交解决方案时 resolution 不能为空"); ticket.setStatus(TicketStatus.PENDING_CONFIRMATION.name()); ticket.setResolution(request.getResolution().trim()); ticket.setResolutionSubmittedAt(now); }
            case CONFIRM -> { requireStatus(ticket, TicketStatus.PENDING_CONFIRMATION); requireCreator(ticket, operatorId); ticket.setStatus(TicketStatus.RESOLVED.name()); ticket.setResolvedAt(now); markResolutionSla(ticket, now); }
            case REJECT -> { requireStatus(ticket, TicketStatus.PENDING_CONFIRMATION); requireCreator(ticket, operatorId); ticket.setStatus(TicketStatus.PROCESSING.name()); }
            case CLOSE -> { requireStatus(ticket, TicketStatus.RESOLVED); requireCreator(ticket, operatorId); ticket.setStatus(TicketStatus.CLOSED.name()); ticket.setClosedAt(now); }
            case CREATE, ASSIGN -> throw new BusinessException(10001, "该动作不能通过状态流转接口执行");
        }
        ticket.setUpdatedBy(operatorId); updateTicket(ticket); writeTransition(ticket, action, fromStatus, operatorId, request.getRemark());
        outboxEventService.enqueueTicketEvent(ticket, "TICKET_STATUS_CHANGED",
                ticket.getCurrentAssigneeId() == null
                        ? List.of(ticket.getCreatorId())
                        : List.of(ticket.getCreatorId(), ticket.getCurrentAssigneeId()));
        return ticket;
    }

    @Override @Transactional
    @AuditOperation(module = "TICKET", operation = "COMMENT", resourceType = "TICKET_COMMENT")
    /**
     * 在工单下新增公开或内部评论。
     *
     * @param ticketId 工单 ID。
     * @param request 评论内容和可见性。
     * @return 已持久化的评论。
     * @throws BusinessException 当评论人停用或可见性非法时抛出。
     */
    public TicketComment addComment(Long ticketId, TicketCommentCreateRequest request) {
        Long tenantId = tenantContext.tenantId(); Long authorId = tenantContext.userId();
        Ticket ticket = getTicket(tenantId, ticketId); UserAccount author = userAccountService.getUser(tenantId, authorId);
        if (!ACTIVE.equals(author.getStatus())) throw new BusinessException(30001, "已停用用户不能评论");
        String visibility = request.getVisibility().trim().toUpperCase(Locale.ROOT);
        if (!("PUBLIC".equals(visibility) || "INTERNAL".equals(visibility))) throw new BusinessException(10001, "visibility 仅支持 PUBLIC 或 INTERNAL");
        TicketComment comment = new TicketComment(); comment.setTenantId(ticket.getTenantId()); comment.setTicketId(ticket.getId()); comment.setAuthorId(author.getId()); comment.setContent(request.getContent().trim()); comment.setVisibility(visibility); comment.setCreatedAt(LocalDateTime.now());
        if (commentMapper.insert(comment) != 1) throw new IllegalStateException("发表评论失败"); return comment;
    }

    @Override
    /** @param tenantId 租户 ID。@param ticketId 工单 ID。@return 按时间升序的状态流转历史。 */
    public List<TicketTransition> listTransitions(Long tenantId, Long ticketId) { getTicket(tenantId, ticketId); return transitionMapper.selectList(Wrappers.lambdaQuery(TicketTransition.class).eq(TicketTransition::getTenantId, tenantId).eq(TicketTransition::getTicketId, ticketId).orderByAsc(TicketTransition::getCreatedAt)); }
    @Override
    /** @param tenantId 租户 ID。@param ticketId 工单 ID。@return 按时间升序的分派历史。 */
    public List<TicketAssignment> listAssignments(Long tenantId, Long ticketId) { getTicket(tenantId, ticketId); return assignmentMapper.selectList(Wrappers.lambdaQuery(TicketAssignment.class).eq(TicketAssignment::getTenantId, tenantId).eq(TicketAssignment::getTicketId, ticketId).orderByAsc(TicketAssignment::getCreatedAt)); }
    @Override
    /** @param tenantId 租户 ID。@param ticketId 工单 ID。@return 按时间升序的评论列表。 */
    public List<TicketComment> listComments(Long tenantId, Long ticketId) { getTicket(tenantId, ticketId); return commentMapper.selectList(Wrappers.lambdaQuery(TicketComment.class).eq(TicketComment::getTenantId, tenantId).eq(TicketComment::getTicketId, ticketId).orderByAsc(TicketComment::getCreatedAt)); }

    /**
     * 通过 MyBatis-Plus 的版本字段更新工单。
     *
     * @param ticket 待更新的工单快照。
     * @throws BusinessException 当数据库版本已经变化，说明出现并发修改时抛出。
     */
    private void updateTicket(Ticket ticket) { if (!updateById(ticket)) throw new BusinessException(30002, "工单已被其他操作修改，请刷新后重试"); }
    /** @param ticket 当前工单。@param action 已执行动作。@param fromStatus 流转前状态。@param operatorId 操作人。@param remark 操作备注。 */
    private void writeTransition(Ticket ticket, TicketAction action, String fromStatus, Long operatorId, String remark) { TicketTransition transition = new TicketTransition(); transition.setTenantId(ticket.getTenantId()); transition.setTicketId(ticket.getId()); transition.setAction(action.name()); transition.setFromStatus(fromStatus); transition.setToStatus(ticket.getStatus()); transition.setOperatorId(operatorId); transition.setRemark(remark); transition.setTicketVersion(ticket.getVersion()); transition.setCreatedAt(LocalDateTime.now()); transitionMapper.insert(transition); }
    /** @param tenantId 当前租户。@param operatorId 当前操作人。@throws BusinessException 当操作人已停用时抛出。 */
    private void requireActiveOperator(Long tenantId, Long operatorId) { UserAccount operator = userAccountService.getUser(tenantId, requirePositive(operatorId, "operatorId")); if (!ACTIVE.equals(operator.getStatus())) throw new BusinessException(30001, "已停用用户不能操作工单"); }
    /** @param ticket 当前工单。@param version 客户端提交的版本。@throws BusinessException 当版本不一致时抛出。 */
    private void assertVersion(Ticket ticket, Integer version) { if (version == null || !version.equals(ticket.getVersion())) throw new BusinessException(30002, "工单版本已变化，请刷新后重试"); }
    /** @param ticket 当前工单。@param expected 本动作允许的唯一前置状态。@throws BusinessException 当状态不匹配时抛出。 */
    private void requireStatus(Ticket ticket, TicketStatus expected) { if (!expected.name().equals(ticket.getStatus())) throw new BusinessException(30001, "当前工单状态不允许执行此操作"); }
    /** @param ticket 当前工单。@param operatorId 操作人 ID。@throws BusinessException 当操作人不是当前处理人时抛出。 */
    private void requireAssignee(Ticket ticket, Long operatorId) { if (!operatorId.equals(ticket.getCurrentAssigneeId())) throw new BusinessException(30001, "只有当前处理人可以执行此操作"); }
    /** @param ticket 当前工单。@param operatorId 操作人 ID。@throws BusinessException 当操作人不是创建人时抛出。 */
    private void requireCreator(Ticket ticket, Long operatorId) { if (!operatorId.equals(ticket.getCreatorId())) throw new BusinessException(30001, "只有工单创建人可以执行此操作"); }
    /** @param ticket 需更新 SLA 响应状态的工单。@param now 首次响应发生的时间。 */
    private void markResponseSla(Ticket ticket, LocalDateTime now) { if (now.isAfter(ticket.getResponseDueAt())) { ticket.setResponseSlaStatus("BREACHED"); ticket.setResponseBreachedAt(now); } else ticket.setResponseSlaStatus("MET"); }
    /** @param ticket 需更新 SLA 解决状态的工单。@param now 确认解决发生的时间。 */
    private void markResolutionSla(Ticket ticket, LocalDateTime now) { if (now.isAfter(ticket.getResolutionDueAt())) { ticket.setResolutionSlaStatus("BREACHED"); ticket.setResolutionBreachedAt(now); } else ticket.setResolutionSlaStatus("MET"); }
    /** @param raw 客户端传入的动作字符串。@return 标准化后的状态机动作。@throws BusinessException 当动作未定义时抛出。 */
    private TicketAction parseAction(String raw) { try { return TicketAction.valueOf(raw.trim().toUpperCase(Locale.ROOT)); } catch (Exception ex) { throw new BusinessException(10001, "不支持的工单动作"); } }
    /** @param raw 客户端传入的工单类型。@return 标准化且在白名单中的工单类型。@throws BusinessException 当类型为空或未支持时抛出。 */
    private String normalizeType(String raw) { if (!StringUtils.hasText(raw)) throw new BusinessException(10001, "ticketType 不能为空"); String value = raw.trim().toUpperCase(Locale.ROOT); if (!TICKET_TYPES.contains(value)) throw new BusinessException(10001, "不支持的 ticketType"); return value; }
    /** @param value 待校验数值。@param field 字段名。@return 原数值。@throws BusinessException 当数值不是正整数时抛出。 */
    private Long requirePositive(Long value, String field) { if (value == null || value <= 0) throw new BusinessException(10001, field + " 必须是正整数"); return value; }
}
