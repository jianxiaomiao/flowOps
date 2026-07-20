package com.example.demo.security;

import com.example.demo.common.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import java.util.Set;
import java.util.stream.Collectors;

@Component
/**
 * 从 Spring Security 上下文提取当前 JWT 用户，屏蔽 JWT 访问细节。
 */
public class CurrentUserProvider {

    /**
     * 获取当前已登录用户；缺少可信身份信息时拒绝继续执行业务。
     *
     * @return 从已验证 JWT 组装出的当前用户快照。
     * @throws BusinessException 当没有已认证 JWT 或 JWT 缺少用户、租户声明时抛出。
     */
    public CurrentUser requireCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken token) || !authentication.isAuthenticated()) {
            throw new BusinessException(10002, "请先登录");
        }
        Number userIdClaim = token.getToken().getClaim("user_id");
        Number tenantIdClaim = token.getToken().getClaim("tenant_id");
        if (userIdClaim == null || tenantIdClaim == null) {
            throw new BusinessException(10002, "登录凭证缺少身份信息");
        }
        Long userId = userIdClaim.longValue();
        Long tenantId = tenantIdClaim.longValue();
        Set<String> authorities = token.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .collect(Collectors.toUnmodifiableSet());
        return new CurrentUser(userId, tenantId, token.getName(), authorities);
    }

    /**
     * 尝试获取当前用户，允许匿名场景以 {@code null} 表示不存在。
     *
     * @return 当前用户；若请求未认证则返回 {@code null}。
     */
    public CurrentUser currentUserOrNull() {
        try {
            return requireCurrentUser();
        } catch (BusinessException exception) {
            return null;
        }
    }
}
