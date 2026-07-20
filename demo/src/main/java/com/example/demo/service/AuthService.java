package com.example.demo.service;

import com.example.demo.dto.auth.LoginRequest;
import com.example.demo.vo.LoginResponse;

/**
 * 身份认证应用服务的对外契约。
 */
public interface AuthService {
    /**
     * 校验登录凭证并签发访问令牌。
     *
     * @param request 登录请求。
     * @return 已签发的 JWT 登录结果。
     */
    LoginResponse login(LoginRequest request);
}
