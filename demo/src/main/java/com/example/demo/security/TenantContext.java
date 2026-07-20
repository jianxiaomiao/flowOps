package com.example.demo.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 业务代码使用的可信租户/操作人入口，不保存额外 ThreadLocal。 */
@Component
@RequiredArgsConstructor
public class TenantContext {
    private final CurrentUserProvider currentUserProvider;

    /**
     * 获取当前登录用户所属的可信租户 ID。
     *
     * @return 当前 JWT 中的 tenantId。
     */
    public Long tenantId() {
        return currentUserProvider.requireCurrentUser().tenantId();
    }

    /**
     * 获取当前登录用户的可信用户 ID。
     *
     * @return 当前 JWT 中的 userId。
     */
    public Long userId() {
        return currentUserProvider.requireCurrentUser().userId();
    }
}
