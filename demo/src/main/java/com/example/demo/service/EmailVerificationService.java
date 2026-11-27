package com.example.demo.service;

import com.example.demo.dto.auth.SendEmailCodeRequest;
import com.example.demo.vo.EmailCodeSendResponse;

/** 企业注册邮箱验证码发送与核销用例。 */
public interface EmailVerificationService {
    EmailCodeSendResponse sendRegistrationCode(SendEmailCodeRequest request, String clientIp);

    void verifyAndConsumeRegistrationCode(String email, String emailCode);
}
