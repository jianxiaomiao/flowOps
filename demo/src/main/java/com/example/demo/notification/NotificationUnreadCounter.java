package com.example.demo.notification;

import com.example.demo.common.redis.SharedStateStore;
import com.example.demo.config.FlowOpsRedisProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.function.LongSupplier;

/** 通知未读数 Cache Aside 缓存；数据库始终是事实来源。 */
@Slf4j
@Component
public class NotificationUnreadCounter {
    private final SharedStateStore stateStore;
    private final FlowOpsRedisProperties properties;

    public NotificationUnreadCounter(SharedStateStore stateStore, FlowOpsRedisProperties properties) {
        this.stateStore = stateStore;
        this.properties = properties;
    }

    public long getOrLoad(Long tenantId, Long userId, LongSupplier databaseLoader) {
        String key = key(tenantId, userId);
        try {
            var cached = stateStore.get(key);
            if (cached.isPresent()) {
                return Long.parseLong(cached.get());
            }
        } catch (RuntimeException exception) {
            log.warn("读取通知未读数缓存失败，回源数据库 tenantId={}, userId={}", tenantId, userId, exception);
        }

        long count = databaseLoader.getAsLong();
        try {
            stateStore.put(key, String.valueOf(count), properties.getNotificationUnreadTtl());
        } catch (RuntimeException exception) {
            log.warn("写入通知未读数缓存失败 tenantId={}, userId={}", tenantId, userId, exception);
        }
        return count;
    }

    /** 数据库事务提交后删除缓存，下一次读取会获得准确计数。 */
    public void invalidateAfterCommit(Long tenantId, Long userId) {
        Runnable invalidation = () -> {
            try {
                stateStore.delete(key(tenantId, userId));
            } catch (RuntimeException exception) {
                log.warn("通知未读数缓存失效失败 tenantId={}, userId={}", tenantId, userId, exception);
            }
        };
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    invalidation.run();
                }
            });
        } else {
            invalidation.run();
        }
    }

    private String key(Long tenantId, Long userId) {
        return "notification:unread:" + tenantId + ":" + userId;
    }
}
