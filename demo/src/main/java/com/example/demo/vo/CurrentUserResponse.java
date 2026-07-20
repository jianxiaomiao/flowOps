package com.example.demo.vo;

import java.util.List;

/**
 * 当前登录用户响应。
 *
 * <p>这里只暴露前端恢复登录状态所需的可信身份与权限快照，
 * 不直接返回 {@code UserAccount} 数据库实体。</p>
 */
public record CurrentUserResponse(
        Long userId,
        Long tenantId,
        String username,
        List<String> authorities
) {
}
