package com.example.demo.security;

import com.example.demo.common.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
/**
 * 将“未认证”统一转换为 FlowOps JSON 响应。
 *
 * <p>它由 Spring Security 在请求缺少 Token、Token 无效或已过期时调用，
 * 不参与用户名和密码的登录校验。</p>
 */
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper;


    @Override
    /**
     * 启动当前请求的认证失败响应。
     *
     * @param request 这次原始请求，例如请求路径、请求头、方法。
     * @param response 要写回给 Postman/前端的响应。
     * @param authException 认证失败的原因，例如 Token 失效、签名错误等。
     * @throws IOException 向 HTTP 响应输出 JSON 时可能发生的 I/O 异常。
     */
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), Result.failure(10002, "登录凭证无效或已过期"));
    }
}
