package com.example.demo.security;

import java.util.Set;

/** 已通过 JWT 签名校验的身份快照。 */
/**
 * 当前已认证用户的可信身份快照。
 *
 * <p>数据来自已验签 JWT，而非客户端请求参数，因此可用于租户隔离和权限判断。</p>
 *
 * @param userId 当前登录用户 ID。
 * @param tenantId 当前用户所属租户 ID。
 * @param username 当前用户的登录名。
 * @param authorities 当前 Token 携带的权限编码集合。
 */
public record CurrentUser(Long userId, Long tenantId, String username, Set<String> authorities) {
}
