package com.example.demo.security;

import com.example.demo.ticket.TicketAction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Locale;
import java.util.Map;

/** 仅处理“一个接口对应多个业务动作”的动态权限；普通接口直接用 hasAuthority。 */
@Component("rbacGuard")
@RequiredArgsConstructor
public class RbacGuard {
    private static final Map<TicketAction, String> TRANSITION_PERMISSIONS = Map.of(
            TicketAction.ACCEPT, "ticket:accept",
            TicketAction.START_PROCESS, "ticket:update",
            TicketAction.SUBMIT_RESOLUTION, "ticket:resolve",
            TicketAction.CONFIRM, "ticket:confirm",
            TicketAction.REJECT, "ticket:confirm",
            TicketAction.CLOSE, "ticket:confirm"
    );
    private final CurrentUserProvider currentUserProvider;

    /**
     * 判断当前用户是否拥有执行指定工单状态动作所需的权限。
     *
     * @param rawAction 请求传入的工单动作名称，例如 {@code ACCEPT}。
     * @return 动作名称合法且当前 Token 包含对应权限时返回 {@code true}；否则返回 {@code false}。
     */
    public boolean canTransition(String rawAction) {
        try {
            TicketAction action = TicketAction.valueOf(rawAction.trim().toUpperCase(Locale.ROOT));
            String permission = TRANSITION_PERMISSIONS.get(action);
            return permission != null
                    && currentUserProvider.requireCurrentUser().authorities().contains(permission);
        } catch (Exception exception) {
            return false;
        }
    }
}
