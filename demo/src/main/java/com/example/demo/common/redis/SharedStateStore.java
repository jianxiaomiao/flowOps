package com.example.demo.common.redis;

import java.time.Duration;
import java.util.Optional;

/** 多实例间共享、且必须带 TTL 的短期键值状态端口。 */
public interface SharedStateStore {
    Optional<String> get(String key);
    void put(String key, String value, Duration ttl);
    long incrementAndGet(String key, Duration ttl);
    void delete(String key);
}
