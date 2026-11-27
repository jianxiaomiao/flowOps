package com.example.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 控制公开企业注册入口与邮箱验证码开关。 */
@ConfigurationProperties(prefix = "flowops.registration")
public record RegistrationProperties(boolean enabled, boolean emailVerificationEnabled) {
}
