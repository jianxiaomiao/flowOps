package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.demo.common.BusinessException;
import com.example.demo.config.RegistrationProperties;
import com.example.demo.dto.auth.SendEmailCodeRequest;
import com.example.demo.entity.EmailVerificationCode;
import com.example.demo.mapper.EmailVerificationCodeMapper;
import com.example.demo.security.CaptchaService;
import com.example.demo.service.EmailVerificationService;
import com.example.demo.vo.EmailCodeSendResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {
    static final String PURPOSE_TENANT_REGISTER = "TENANT_REGISTER";
    static final String STATUS_PENDING = "PENDING";
    static final String STATUS_EXPIRED = "EXPIRED";
    static final String STATUS_LOCKED = "LOCKED";
    static final long COOLDOWN_SECONDS = 60L;
    static final long EXPIRE_MINUTES = 5L;
    static final int MAX_ATTEMPTS = 5;
    static final long MAX_DAILY_EMAIL_SENDS = 10L;
    static final long MAX_IP_SENDS_PER_10_MINUTES = 10L;

    private final EmailVerificationCodeMapper codeMapper;
    private final EmailVerificationAttemptRecorder attemptRecorder;
    private final CaptchaService captchaService;
    private final VerificationMailSender mailSender;
    private final RegistrationProperties registrationProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public EmailCodeSendResponse sendRegistrationCode(SendEmailCodeRequest request, String clientIp) {
        if (!registrationProperties.enabled()) {
            throw new BusinessException(10003, "当前环境未开放企业注册");
        }
        captchaService.verifyAndConsume(request.captchaId(), request.captchaAnswer());

        String normalizedEmail = normalizeEmail(request.email());
        String safeIp = normalizeIp(clientIp);
        LocalDateTime now = LocalDateTime.now();

        enforceSendRateLimits(normalizedEmail, safeIp, now);

        codeMapper.replacePendingByEmailAndPurpose(normalizedEmail, PURPOSE_TENANT_REGISTER, now);

        String code = String.format("%06d", secureRandom.nextInt(1_000_000));
        EmailVerificationCode entity = new EmailVerificationCode();
        entity.setEmail(normalizedEmail);
        entity.setPurpose(PURPOSE_TENANT_REGISTER);
        entity.setCodeHash(hashCode(normalizedEmail, code));
        entity.setStatus(STATUS_PENDING);
        entity.setFailedAttempts(0);
        entity.setMaxAttempts(MAX_ATTEMPTS);
        entity.setRequestIp(safeIp);
        entity.setExpiresAt(now.plusMinutes(EXPIRE_MINUTES));
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        codeMapper.insert(entity);

        try {
            mailSender.sendRegistrationCode(normalizedEmail, code, EXPIRE_MINUTES);
        } catch (RuntimeException exception) {
            log.error("注册验证码邮件发送失败: ip={}", safeIp, exception);
            throw new BusinessException(50001, "验证码邮件发送失败，请稍后重试");
        }
        return new EmailCodeSendResponse(COOLDOWN_SECONDS, EXPIRE_MINUTES * 60);
    }

    @Override
    public void verifyAndConsumeRegistrationCode(String rawEmail, String rawCode) {
        if (!registrationProperties.emailVerificationEnabled()) {
            return;
        }
        if (rawEmail == null || rawEmail.isBlank()) {
            throw new BusinessException(10001, "请输入联系邮箱");
        }
        if (rawCode == null || rawCode.isBlank()) {
            throw new BusinessException(10001, "请输入邮箱验证码");
        }
        String normalizedEmail = normalizeEmail(rawEmail);
        String trimmedCode = rawCode.trim();
        LocalDateTime now = LocalDateTime.now();

        EmailVerificationCode latest = codeMapper.selectOne(Wrappers.lambdaQuery(EmailVerificationCode.class)
                .eq(EmailVerificationCode::getEmail, normalizedEmail)
                .eq(EmailVerificationCode::getPurpose, PURPOSE_TENANT_REGISTER)
                .eq(EmailVerificationCode::getStatus, STATUS_PENDING)
                .orderByDesc(EmailVerificationCode::getId)
                .last("LIMIT 1"));
        if (latest == null) {
            throw new BusinessException(10001, "邮箱验证码无效或已过期，请重新获取");
        }
        if (!latest.getExpiresAt().isAfter(now)) {
            attemptRecorder.markStatus(latest, STATUS_EXPIRED, now);
            throw new BusinessException(10001, "邮箱验证码已过期，请重新获取");
        }
        int maxAttempts = latest.getMaxAttempts() == null ? MAX_ATTEMPTS : latest.getMaxAttempts();
        int failedAttempts = latest.getFailedAttempts() == null ? 0 : latest.getFailedAttempts();
        if (failedAttempts >= maxAttempts) {
            attemptRecorder.markStatus(latest, STATUS_LOCKED, now);
            throw new BusinessException(10001, "邮箱验证码错误次数过多，请重新获取");
        }

        byte[] expectedHash = latest.getCodeHash().getBytes(StandardCharsets.UTF_8);
        byte[] actualHash = hashCode(normalizedEmail, trimmedCode).getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expectedHash, actualHash)) {
            int updatedFailures = attemptRecorder.recordFailure(latest, maxAttempts, now);
            if (updatedFailures >= maxAttempts) {
                throw new BusinessException(10001, "邮箱验证码错误次数过多，请重新获取");
            }
            throw new BusinessException(10001, "邮箱验证码不正确");
        }

        int updated = codeMapper.markVerifiedIfPending(latest.getId(), now);
        if (updated != 1) {
            throw new BusinessException(10001, "邮箱验证码已被使用或已过期，请重新获取");
        }
    }

    private void enforceSendRateLimits(String email, String ip, LocalDateTime now) {
        long recentForEmail = codeMapper.selectCount(Wrappers.lambdaQuery(EmailVerificationCode.class)
                .eq(EmailVerificationCode::getEmail, email)
                .eq(EmailVerificationCode::getPurpose, PURPOSE_TENANT_REGISTER)
                .ge(EmailVerificationCode::getCreatedAt, now.minusSeconds(COOLDOWN_SECONDS)));
        if (recentForEmail >= 1) {
            throw new BusinessException(10004, "验证码发送过于频繁，请 60 秒后再试");
        }

        long dailyForEmail = codeMapper.selectCount(Wrappers.lambdaQuery(EmailVerificationCode.class)
                .eq(EmailVerificationCode::getEmail, email)
                .eq(EmailVerificationCode::getPurpose, PURPOSE_TENANT_REGISTER)
                .ge(EmailVerificationCode::getCreatedAt, now.minusHours(24)));
        if (dailyForEmail >= MAX_DAILY_EMAIL_SENDS) {
            throw new BusinessException(10004, "该邮箱 24 小时内发送验证码次数已达上限");
        }

        long recentForIp = codeMapper.selectCount(Wrappers.lambdaQuery(EmailVerificationCode.class)
                .eq(EmailVerificationCode::getRequestIp, ip)
                .ge(EmailVerificationCode::getCreatedAt, now.minusMinutes(10)));
        if (recentForIp >= MAX_IP_SENDS_PER_10_MINUTES) {
            throw new BusinessException(10004, "当前网络请求验证码过于频繁，请稍后再试");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeIp(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return "unknown";
        }
        String trimmed = clientIp.trim();
        return trimmed.length() > 64 ? trimmed.substring(0, 64) : trimmed;
    }

    static String hashCode(String normalizedEmail, String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((normalizedEmail + ":" + code).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前 JRE 不支持 SHA-256", exception);
        }
    }
}
