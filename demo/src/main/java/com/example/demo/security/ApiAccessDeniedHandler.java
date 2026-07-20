package com.example.demo.security;

import com.example.demo.common.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
/**
 * 将“已认证但无权限”统一转换为 FlowOps JSON 响应。
 */
public class ApiAccessDeniedHandler implements AccessDeniedHandler {
    private final ObjectMapper objectMapper;

    @Override
    /**
     * 写出权限不足的 403 响应。
     *
     * @param request 这次原始请求，例如请求路径、请求头、方法。
     * @param response 要写回给 Postman/前端的响应。
     * @param accessDeniedException 权限校验拒绝请求时产生的异常。
     * @throws IOException 向 HTTP 响应输出 JSON 时可能发生的 I/O 异常。
     */
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), Result.failure(10003, "没有执行该操作的权限"));
    }
}
