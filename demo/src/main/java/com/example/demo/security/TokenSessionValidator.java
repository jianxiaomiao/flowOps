package com.example.demo.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/** 将 Redis 中的撤销状态和会话版本接入每一次 Bearer Token 验证。 */
@Component
public class TokenSessionValidator implements OAuth2TokenValidator<Jwt> {
    private static final OAuth2Error INVALID_TOKEN = new OAuth2Error(
            "invalid_token", "Token 已撤销或用户会话版本已失效", null);
    private final TokenSessionService tokenSessionService;

    public TokenSessionValidator(TokenSessionService tokenSessionService) {
        this.tokenSessionService = tokenSessionService;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        try {
            return tokenSessionService.isActive(token)
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(INVALID_TOKEN);
        } catch (RuntimeException exception) {
            // 安全状态无法确认时失败关闭，避免 Redis 故障把已撤销 Token 重新放行。
            return OAuth2TokenValidatorResult.failure(INVALID_TOKEN);
        }
    }
}
