package com.example.demo.security;

import com.example.demo.common.BusinessException;
import com.example.demo.common.redis.SharedStateStore;
import com.example.demo.config.FlowOpsRedisProperties;
import com.example.demo.dto.auth.LoginRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

/** 登录失败和公开注册的固定窗口分布式限流。 */
@Slf4j
@Component
public class AuthenticationRateLimiter {
    private final SharedStateStore stateStore;
    private final FlowOpsRedisProperties properties;

    public AuthenticationRateLimiter(SharedStateStore stateStore, FlowOpsRedisProperties properties) {
        this.stateStore = stateStore;
        this.properties = properties;
    }

    /** 登录前检查账号和来源 IP 的失败次数；Redis 异常时登录链路降级放行。 */
    public void assertLoginAllowed(LoginRequest request, String clientIp) {
        try {
            if (count(accountKey(request)) >= properties.getLoginMaxFailures()
                    || count(ipKey("login", clientIp)) >= properties.getLoginMaxFailures()) {
                throw tooManyRequests("登录失败次数过多，请稍后再试");
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.warn("登录限流状态读取失败，本次降级放行", exception);
        }
    }

    /** 只统计认证失败，不让正常登录消耗额度。 */
    public void recordLoginFailure(LoginRequest request, String clientIp) {
        try {
            stateStore.incrementAndGet(accountKey(request), properties.getLoginWindow());
            stateStore.incrementAndGet(ipKey("login", clientIp), properties.getLoginWindow());
        } catch (RuntimeException exception) {
            log.warn("登录失败次数写入失败", exception);
        }
    }

    /** 登录成功后清除该账号维度的失败次数；IP 维度保留以约束批量撞库。 */
    public void resetLoginAccount(LoginRequest request) {
        try {
            stateStore.delete(accountKey(request));
        } catch (RuntimeException exception) {
            log.warn("登录失败次数清理失败", exception);
        }
    }

    /** 公开注册是高风险写操作，状态服务异常时采用失败关闭。 */
    public void consumeRegistration(String clientIp) {
        try {
            long current = stateStore.incrementAndGet(
                    ipKey("register", clientIp), properties.getRegistrationWindow());
            if (current > properties.getRegistrationMaxRequests()) {
                throw tooManyRequests("注册请求过于频繁，请稍后再试");
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.warn("注册限流状态访问失败，拒绝本次公开注册", exception);
            throw tooManyRequests("注册保护服务暂不可用，请稍后再试");
        }
    }

    private long count(String key) {
        return stateStore.get(key).map(Long::parseLong).orElse(0L);
    }

    private String accountKey(LoginRequest request) {
        String identity = request.tenantCode().trim().toLowerCase(Locale.ROOT)
                + ":" + request.username().trim().toLowerCase(Locale.ROOT);
        return "rate:login:account:" + sha256(identity);
    }

    private String ipKey(String action, String clientIp) {
        String safeIp = clientIp == null || clientIp.isBlank() ? "unknown" : clientIp;
        return "rate:" + action + ":ip:" + sha256(safeIp);
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前 JRE 不支持 SHA-256", exception);
        }
    }

    private BusinessException tooManyRequests(String message) {
        return new BusinessException(10004, message);
    }
}
