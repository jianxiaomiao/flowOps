package com.example.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;

@ConfigurationProperties(prefix = "flowops.security.jwt")
public record JwtProperties(String issuer, Duration accessTokenTtl, String secret) {
}
