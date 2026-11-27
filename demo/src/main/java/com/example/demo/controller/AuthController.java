package com.example.demo.controller;

import com.example.demo.common.BusinessException;
import com.example.demo.common.Result;
import com.example.demo.dto.auth.LoginRequest;
import com.example.demo.dto.auth.SendEmailCodeRequest;
import com.example.demo.dto.auth.TenantRegistrationRequest;
import com.example.demo.security.AuthenticationRateLimiter;
import com.example.demo.security.CaptchaService;
import com.example.demo.security.CurrentUser;
import com.example.demo.security.CurrentUserProvider;
import com.example.demo.security.TokenSessionService;
import com.example.demo.service.AuthService;
import com.example.demo.service.EmailVerificationService;
import com.example.demo.service.OnboardingService;
import com.example.demo.vo.CaptchaResponse;
import com.example.demo.vo.CurrentUserResponse;
import com.example.demo.vo.EmailCodeSendResponse;
import com.example.demo.vo.LoginResponse;
import com.example.demo.vo.TenantRegistrationResponse;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
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
    private final AuthenticationRateLimiter authenticationRateLimiter;
    private final TokenSessionService tokenSessionService;
    private final CaptchaService captchaService;
    private final EmailVerificationService emailVerificationService;

    /**
     * 使用租户编码、用户名和密码创建 JWT。
     *
     * @param request 已通过格式校验的登录请求。
     * @return 含访问令牌、令牌类型和过期时间的统一成功响应。
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                       HttpServletRequest servletRequest) {
        String clientIp = servletRequest.getRemoteAddr();
        authenticationRateLimiter.assertLoginAllowed(request, clientIp);
        try {
            LoginResponse response = authService.login(request);
            authenticationRateLimiter.resetLoginAccount(request);
            return Result.success(response);
        } catch (BusinessException exception) {
            if (exception.getCode() == 10002) {
                authenticationRateLimiter.recordLoginFailure(request, clientIp);
            }
            throw exception;
        }
    }

    /** 获取图形算术验证码，用于前置校验发送邮箱验证码。 */
    @GetMapping("/captcha")
    public Result<CaptchaResponse> captcha() {
        return Result.success(captchaService.generate());
    }

    /** 校验图形验证码并发送注册邮箱验证码。 */
    @PostMapping("/email-codes")
    public Result<EmailCodeSendResponse> sendEmailCode(@Valid @RequestBody SendEmailCodeRequest request,
                                                       HttpServletRequest servletRequest) {
        return Result.success(emailVerificationService.sendRegistrationCode(request, servletRequest.getRemoteAddr()));
    }

    /** 创建新企业、默认部门和首位管理员。 */
    @PostMapping("/register")
    public Result<TenantRegistrationResponse> register(@Valid @RequestBody TenantRegistrationRequest request,
                                                        HttpServletRequest servletRequest) {
        authenticationRateLimiter.consumeRegistration(servletRequest.getRemoteAddr());
        return Result.success(onboardingService.registerTenant(request));
    }

    /** 撤销当前 JWT；前端仍应同时清除本地 Token。 */
    @PostMapping("/logout")
    public Result<Void> logout(JwtAuthenticationToken authentication) {
        tokenSessionService.revoke(authentication.getToken());
        return Result.success();
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
