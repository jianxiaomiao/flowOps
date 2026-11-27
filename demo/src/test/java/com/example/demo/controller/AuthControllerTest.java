package com.example.demo.controller;

import com.example.demo.dto.auth.LoginRequest;
import com.example.demo.security.CaptchaService;
import com.example.demo.security.CurrentUser;
import com.example.demo.security.CurrentUserProvider;
import com.example.demo.security.AuthenticationRateLimiter;
import com.example.demo.security.TokenSessionService;
import com.example.demo.service.AuthService;
import com.example.demo.service.EmailVerificationService;
import com.example.demo.service.OnboardingService;
import com.example.demo.vo.LoginResponse;
import org.junit.jupiter.api.Test;
import jakarta.servlet.http.HttpServletRequest;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    private final AuthService authService = mock(AuthService.class);
    private final CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
    private final OnboardingService onboardingService = mock(OnboardingService.class);
    private final AuthenticationRateLimiter rateLimiter = mock(AuthenticationRateLimiter.class);
    private final TokenSessionService tokenSessionService = mock(TokenSessionService.class);
    private final CaptchaService captchaService = mock(CaptchaService.class);
    private final EmailVerificationService emailVerificationService = mock(EmailVerificationService.class);
    private final HttpServletRequest servletRequest = mock(HttpServletRequest.class);
    private final AuthController controller = new AuthController(
            authService, currentUserProvider, onboardingService, rateLimiter, tokenSessionService,
            captchaService, emailVerificationService);

    @Test
    void shouldReturnLoginServiceResult() {
        LoginRequest request = new LoginRequest("ACME", "xiaomiao", "password123");
        LoginResponse loginResponse = new LoginResponse(
                "signed-token", "Bearer", Instant.parse("2026-07-20T12:00:00Z"));
        when(authService.login(request)).thenReturn(loginResponse);

        when(servletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        var result = controller.login(request, servletRequest);

        assertEquals(0, result.getCode());
        assertEquals(loginResponse, result.getData());
        verify(authService).login(request);
        verify(rateLimiter).assertLoginAllowed(request, "127.0.0.1");
        verify(rateLimiter).resetLoginAccount(request);
    }

    @Test
    void shouldReturnTrustedCurrentUserWithSortedAuthorities() {
        when(currentUserProvider.requireCurrentUser()).thenReturn(new CurrentUser(
                1L,
                1L,
                "xiaomiao",
                Set.of("user:manage", "ticket:create", "role:manage")));

        var result = controller.currentUser();

        assertEquals(0, result.getCode());
        assertEquals(1L, result.getData().userId());
        assertEquals(1L, result.getData().tenantId());
        assertEquals("xiaomiao", result.getData().username());
        assertEquals(
                java.util.List.of("role:manage", "ticket:create", "user:manage"),
                result.getData().authorities());
    }
}
