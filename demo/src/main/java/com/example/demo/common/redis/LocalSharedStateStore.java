package com.example.demo.common.redis;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Redis 未启用时的单实例降级实现，便于开发和测试；生产多实例不能依赖它。 */
@Component
@ConditionalOnProperty(prefix = "flowops.redis", name = "enabled", havingValue = "false", matchIfMissing = true)
public class LocalSharedStateStore implements SharedStateStore {
    private final ConcurrentHashMap<String, Entry> entries = new ConcurrentHashMap<>();

    @Override
    public Optional<String> get(String key) {
        Entry entry = entries.get(key);
        if (entry == null) {
            return Optional.empty();
        }
        if (!entry.expiresAt().isAfter(Instant.now())) {
            entries.remove(key, entry);
            return Optional.empty();
        }
        return Optional.of(entry.value());
    }

    @Override
    public void put(String key, String value, Duration ttl) {
        entries.put(key, new Entry(value, expiresAt(ttl)));
    }

    @Override
    public long incrementAndGet(String key, Duration ttl) {
        Entry updated = entries.compute(key, (ignored, existing) -> {
            boolean expired = existing == null || !existing.expiresAt().isAfter(Instant.now());
            long current = expired ? 0 : Long.parseLong(existing.value());
            Instant expiry = expired ? expiresAt(ttl) : existing.expiresAt();
            return new Entry(String.valueOf(current + 1), expiry);
        });
        return Long.parseLong(updated.value());
    }

    @Override
    public void delete(String key) {
        entries.remove(key);
    }

    private Instant expiresAt(Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("共享短期状态必须设置正数 TTL");
        }
        return Instant.now().plus(ttl);
    }

    private record Entry(String value, Instant expiresAt) {}
}
