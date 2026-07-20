package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.dto.auth.LoginRequest;
import com.example.demo.dto.auth.TenantRegistrationRequest;
import com.example.demo.security.CurrentUser;
import com.example.demo.security.CurrentUserProvider;
import com.example.demo.service.AuthService;
import com.example.demo.service.OnboardingService;
import com.example.demo.vo.CurrentUserResponse;
import com.example.demo.vo.LoginResponse;
import com.example.demo.vo.TenantRegistrationResponse;
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
    private final CurrentUserProvider currentUserProvider;
    private final OnboardingService onboardingService;

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

    /** 创建新企业、默认部门和首位管理员。 */
    @PostMapping("/register")
    public Result<TenantRegistrationResponse> register(@Valid @RequestBody TenantRegistrationRequest request) {
        return Result.success(onboardingService.registerTenant(request));
    }

    /**
     * 返回已通过 JWT 验签的当前用户及权限快照。
     *
     * <p>用户 ID、租户 ID 和权限均来自安全上下文，不接受客户端传入，
     * 避免前端伪造身份或跨租户访问。</p>
     *
     * @return 当前用户身份和按编码排序的权限列表。
     */
    @GetMapping("/me")
    public Result<CurrentUserResponse> currentUser() {
        CurrentUser currentUser = currentUserProvider.requireCurrentUser();
        CurrentUserResponse response = new CurrentUserResponse(
                currentUser.userId(),
                currentUser.tenantId(),
                currentUser.username(),
                currentUser.authorities().stream().sorted().toList());
        return Result.success(response);
    }
}
