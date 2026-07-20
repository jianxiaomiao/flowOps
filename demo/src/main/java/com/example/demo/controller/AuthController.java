package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.dto.auth.LoginRequest;
import com.example.demo.service.AuthService;
import com.example.demo.vo.LoginResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
/**
 * 认证接口入口；只负责接收登录请求并返回访问令牌。
 */
public class AuthController {
    private final AuthService authService;

    /**
     * 使用租户编码、用户名和密码创建 JWT。
     *
     * @param request 已通过格式校验的登录请求。
     * @return 含访问令牌、令牌类型和过期时间的统一成功响应。
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }
}
