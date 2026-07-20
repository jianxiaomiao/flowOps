package com.example.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 控制公开企业注册入口，生产环境默认应关闭或接入验证码与审核。 */
@ConfigurationProperties(prefix = "flowops.registration")
public record RegistrationProperties(boolean enabled) {
}
