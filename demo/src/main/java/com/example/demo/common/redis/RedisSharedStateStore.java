package com.example.demo.common.redis;

import com.example.demo.config.FlowOpsRedisProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/** 基于 Redis 的共享状态实现；INCR 与首次设置 TTL 由 Lua 保证原子性。 */
@Component
@ConditionalOnProperty(prefix = "flowops.redis", name = "enabled", havingValue = "true")
public class RedisSharedStateStore implements SharedStateStore {
    private static final DefaultRedisScript<Long> INCREMENT_SCRIPT = new DefaultRedisScript<>("""
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
                redis.call('PEXPIRE', KEYS[1], ARGV[1])
            end
            return current
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final String keyPrefix;

    public RedisSharedStateStore(StringRedisTemplate redisTemplate, FlowOpsRedisProperties properties) {
        this.redisTemplate = redisTemplate;
        this.keyPrefix = properties.getKeyPrefix();
    }

    @Override
    public Optional<String> get(String key) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(redisKey(key)));
    }

    @Override
    public void put(String key, String value, Duration ttl) {
        redisTemplate.opsForValue().set(redisKey(key), value, requireTtl(ttl));
    }

    @Override
    public long incrementAndGet(String key, Duration ttl) {
        Long value = redisTemplate.execute(
                INCREMENT_SCRIPT,
                List.of(redisKey(key)),
                String.valueOf(requireTtl(ttl).toMillis()));
        if (value == null) {
            throw new IllegalStateException("Redis 未返回计数结果");
        }
        return value;
    }

    @Override
    public void delete(String key) {
        redisTemplate.delete(redisKey(key));
    }

    private String redisKey(String key) {
        return keyPrefix + ":" + key;
    }

    private Duration requireTtl(Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("共享短期状态必须设置正数 TTL");
        }
        return ttl;
    }
}
