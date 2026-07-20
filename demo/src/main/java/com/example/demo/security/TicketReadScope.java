package com.example.demo.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.demo.entity.Ticket;
import com.example.demo.entity.UserAccount;
import com.example.demo.mapper.TicketMapper;
import com.example.demo.mapper.UserAccountMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Set;

/**
 * 把 RBAC 的“是否能读”进一步落实为 OWN、DEPARTMENT、TENANT 三种数据范围。
 */
@Component("ticketReadScope")
@RequiredArgsConstructor
public class TicketReadScope {
    private final CurrentUserProvider currentUserProvider;
    private final TicketMapper ticketMapper;
    private final UserAccountMapper userAccountMapper;

    /** @param ticketId 工单 ID。@return 当前用户拥有此工单数据范围时返回 {@code true}。 */
    public boolean canRead(Long ticketId) {
        CurrentUser user = currentUserProvider.requireCurrentUser();
        Ticket ticket = ticketMapper.selectOne(Wrappers.lambdaQuery(Ticket.class)
                .eq(Ticket::getTenantId, user.tenantId())
                .eq(Ticket::getId, ticketId));
        return ticket != null && canRead(user, ticket);
    }

    /** @param ticketId 工单 ID。@return 工单位于当前用户部门范围内时返回 {@code true}。 */
    public boolean inCurrentDepartment(Long ticketId) {
        CurrentUser user = currentUserProvider.requireCurrentUser();
        Ticket ticket = ticketMapper.selectOne(Wrappers.lambdaQuery(Ticket.class)
                .eq(Ticket::getTenantId, user.tenantId())
                .eq(Ticket::getId, ticketId));
        return ticket != null && currentDepartmentId(user).equals(ticket.getHandlingDepartmentId());
    }

    /** @param wrapper 已包含筛选条件的查询构造器；本方法会追加当前用户数据范围 SQL。 */
    public void applyTo(LambdaQueryWrapper<Ticket> wrapper) {
        CurrentUser user = currentUserProvider.requireCurrentUser();
        Set<String> authorities = user.authorities();
        if (authorities.contains("ticket:read")) return;
        boolean own = authorities.contains("ticket:read:own");
        boolean department = authorities.contains("ticket:read:department");
        if (!own && !department) {
            wrapper.eq(Ticket::getId, -1L);
            return;
        }
        Long departmentId = department ? currentDepartmentId(user) : null;
        wrapper.and(scope -> {
            if (own) {
                scope.and(value -> value.eq(Ticket::getCreatorId, user.userId())
                        .or().eq(Ticket::getCurrentAssigneeId, user.userId()));
            }
            if (department) {
                if (own) scope.or();
                scope.and(value -> value.eq(Ticket::getCreatorDepartmentId, departmentId)
                        .or().eq(Ticket::getHandlingDepartmentId, departmentId));
            }
        });
    }

    /** @param user 当前用户。@param ticket 待判断工单。@return 用户是否拥有该工单读取范围。 */
    private boolean canRead(CurrentUser user, Ticket ticket) {
        if (user.authorities().contains("ticket:read")) return true;
        if (user.authorities().contains("ticket:read:own")
                && (user.userId().equals(ticket.getCreatorId())
                || user.userId().equals(ticket.getCurrentAssigneeId()))) return true;
        if (user.authorities().contains("ticket:read:department")) {
            Long departmentId = currentDepartmentId(user);
            return departmentId.equals(ticket.getCreatorDepartmentId())
                    || departmentId.equals(ticket.getHandlingDepartmentId());
        }
        return false;
    }

    /** @param user 当前用户。@return 当前部门 ID；无法找到时返回 -1，确保不误匹配真实部门。 */
    private Long currentDepartmentId(CurrentUser user) {
        UserAccount account = userAccountMapper.selectOne(Wrappers.lambdaQuery(UserAccount.class)
                .select(UserAccount::getDepartmentId)
                .eq(UserAccount::getTenantId, user.tenantId())
                .eq(UserAccount::getId, user.userId()));
        return account == null || account.getDepartmentId() == null ? -1L : account.getDepartmentId();
    }
}
