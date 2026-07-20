package com.example.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** FlowOps 对 Redis 限流、缓存和短期共享状态的统一配置。 */
@ConfigurationProperties(prefix = "flowops.redis")
public class FlowOpsRedisProperties {
    private boolean enabled;
    private String keyPrefix = "flowops";
    private Duration defaultTtl = Duration.ofMinutes(10);
    private Duration notificationUnreadTtl = Duration.ofSeconds(30);
    private int loginMaxFailures = 5;
    private Duration loginWindow = Duration.ofMinutes(5);
    private int registrationMaxRequests = 3;
    private Duration registrationWindow = Duration.ofHours(1);

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getKeyPrefix() { return keyPrefix; }
    public void setKeyPrefix(String keyPrefix) { this.keyPrefix = keyPrefix; }
    public Duration getDefaultTtl() { return defaultTtl; }
    public void setDefaultTtl(Duration defaultTtl) { this.defaultTtl = defaultTtl; }
    public Duration getNotificationUnreadTtl() { return notificationUnreadTtl; }
    public void setNotificationUnreadTtl(Duration notificationUnreadTtl) { this.notificationUnreadTtl = notificationUnreadTtl; }
    public int getLoginMaxFailures() { return loginMaxFailures; }
    public void setLoginMaxFailures(int loginMaxFailures) { this.loginMaxFailures = loginMaxFailures; }
    public Duration getLoginWindow() { return loginWindow; }
    public void setLoginWindow(Duration loginWindow) { this.loginWindow = loginWindow; }
    public int getRegistrationMaxRequests() { return registrationMaxRequests; }
    public void setRegistrationMaxRequests(int registrationMaxRequests) { this.registrationMaxRequests = registrationMaxRequests; }
    public Duration getRegistrationWindow() { return registrationWindow; }
    public void setRegistrationWindow(Duration registrationWindow) { this.registrationWindow = registrationWindow; }
}
