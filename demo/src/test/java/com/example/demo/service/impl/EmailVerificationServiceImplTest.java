package com.example.demo.service.impl;

import com.example.demo.common.BusinessException;
import com.example.demo.config.RegistrationProperties;
import com.example.demo.dto.auth.SendEmailCodeRequest;
import com.example.demo.entity.EmailVerificationCode;
import com.example.demo.mapper.EmailVerificationCodeMapper;
import com.example.demo.security.CaptchaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceImplTest {
    @Mock EmailVerificationCodeMapper codeMapper;
    @Mock EmailVerificationAttemptRecorder attemptRecorder;
    @Mock CaptchaService captchaService;
    @Mock VerificationMailSender mailSender;

    @Test
    void shouldStoreHashedCodeAndSendEmailAfterCaptchaPasses() {
        when(codeMapper.selectCount(any())).thenReturn(0L, 0L, 0L);
        when(codeMapper.insert(any(EmailVerificationCode.class))).thenReturn(1);

        var response = service(true, true).sendRegistrationCode(
                new SendEmailCodeRequest("User@Example.com", "cid-1", "12"), "127.0.0.1");

        assertEquals(60L, response.cooldownSeconds());
        assertEquals(300L, response.expiresInSeconds());
        verify(captchaService).verifyAndConsume("cid-1", "12");
        verify(codeMapper).replacePendingByEmailAndPurpose(eq("user@example.com"), eq("TENANT_REGISTER"), any());

        ArgumentCaptor<EmailVerificationCode> entityCaptor = ArgumentCaptor.forClass(EmailVerificationCode.class);
        verify(codeMapper).insert(entityCaptor.capture());
        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(mailSender).sendRegistrationCode(eq("user@example.com"), codeCaptor.capture(), eq(5L));

        String rawCode = codeCaptor.getValue();
        assertEquals(6, rawCode.length());
        assertNotEquals(rawCode, entityCaptor.getValue().getCodeHash());
        assertEquals(EmailVerificationServiceImpl.hashCode("user@example.com", rawCode),
                entityCaptor.getValue().getCodeHash());
    }

    @Test
    void shouldRejectWhenWithin60SecondCooldown() {
        when(codeMapper.selectCount(any())).thenReturn(1L);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service(true, true).sendRegistrationCode(
                        new SendEmailCodeRequest("user@example.com", "cid-1", "12"), "127.0.0.1"));

        assertEquals(10004, error.getCode());
        verifyNoInteractions(mailSender);
    }

    @Test
    void shouldLockCodeAfterFifthWrongAttempt() {
        EmailVerificationCode pending = pendingCode("user@example.com", "123456", 4);
        when(codeMapper.selectOne(any())).thenReturn(pending);
        when(attemptRecorder.recordFailure(eq(pending), eq(5), any())).thenReturn(5);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service(true, true).verifyAndConsumeRegistrationCode("user@example.com", "000000"));

        assertEquals(10001, error.getCode());
        verify(attemptRecorder).recordFailure(eq(pending), eq(5), any());
    }

    @Test
    void shouldRejectConcurrentReplayWhenAtomicUpdateReturnsZero() {
        EmailVerificationCode pending = pendingCode("user@example.com", "654321", 0);
        when(codeMapper.selectOne(any())).thenReturn(pending);
        when(codeMapper.markVerifiedIfPending(eq(10L), any())).thenReturn(0);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service(true, true).verifyAndConsumeRegistrationCode("user@example.com", "654321"));

        assertEquals(10001, error.getCode());
    }

    @Test
    void shouldSkipVerificationWhenFlagIsDisabled() {
        service(true, false).verifyAndConsumeRegistrationCode(null, null);
        verifyNoInteractions(codeMapper, attemptRecorder);
    }

    private EmailVerificationServiceImpl service(boolean enabled, boolean verificationEnabled) {
        return new EmailVerificationServiceImpl(
                codeMapper,
                attemptRecorder,
                captchaService,
                mailSender,
                new RegistrationProperties(enabled, verificationEnabled));
    }

    private EmailVerificationCode pendingCode(String email, String code, int failedAttempts) {
        EmailVerificationCode entity = new EmailVerificationCode();
        entity.setId(10L);
        entity.setEmail(email);
        entity.setPurpose("TENANT_REGISTER");
        entity.setCodeHash(EmailVerificationServiceImpl.hashCode(email, code));
        entity.setStatus("PENDING");
        entity.setFailedAttempts(failedAttempts);
        entity.setMaxAttempts(5);
        entity.setExpiresAt(LocalDateTime.now().plusMinutes(3));
        return entity;
    }
}
