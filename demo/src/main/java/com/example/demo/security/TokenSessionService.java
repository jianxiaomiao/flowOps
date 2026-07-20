package com.example.demo.security;

import com.example.demo.common.redis.SharedStateStore;
import com.example.demo.config.JwtProperties;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;

/** 管理 JWT 单令牌撤销和用户会话版本。 */
@Service
public class TokenSessionService {
    private final SharedStateStore stateStore;
    private final Duration tokenStateTtl;

    public TokenSessionService(SharedStateStore stateStore, JwtProperties jwtProperties) {
        this.stateStore = stateStore;
        this.tokenStateTtl = jwtProperties.accessTokenTtl().plus(Duration.ofMinutes(5));
    }

    public long currentVersion(Long tenantId, Long userId) {
        return stateStore.get(versionKey(tenantId, userId)).map(Long::parseLong).orElse(0L);
    }

    /** 返回 Token 是否同时满足 jti 未撤销和用户会话版本最新。 */
    public boolean isActive(Jwt jwt) {
        Number tenantId = jwt.getClaim("tenant_id");
        Number userId = jwt.getClaim("user_id");
        Number tokenVersion = jwt.getClaim("session_version");
        String jti = jwt.getId();
        if (tenantId == null || userId == null || tokenVersion == null || jti == null || jti.isBlank()) {
            return false;
        }
        boolean revoked = stateStore.get(revokedKey(jti)).isPresent();
        return !revoked && tokenVersion.longValue() == currentVersion(tenantId.longValue(), userId.longValue());
    }

    /** 撤销当前 Token，TTL 只保留到 Token 自身过期。 */
    public void revoke(Jwt jwt) {
        if (jwt.getId() == null || jwt.getExpiresAt() == null) {
            return;
        }
        Duration remaining = Duration.between(Instant.now(), jwt.getExpiresAt());
        if (!remaining.isNegative() && !remaining.isZero()) {
            stateStore.put(revokedKey(jwt.getId()), "1", remaining);
        }
    }

    public void bumpVersionAfterCommit(Long tenantId, Long userId) {
        runAfterCommit(() -> stateStore.incrementAndGet(versionKey(tenantId, userId), tokenStateTtl));
    }

    public void bumpVersionsAfterCommit(Long tenantId, Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        runAfterCommit(() -> userIds.stream().distinct()
                .forEach(userId -> stateStore.incrementAndGet(versionKey(tenantId, userId), tokenStateTtl)));
    }

    private void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }

    private String versionKey(Long tenantId, Long userId) {
        return "security:session-version:" + tenantId + ":" + userId;
    }

    private String revokedKey(String jti) {
        return "security:revoked-jti:" + jti;
    }
}
