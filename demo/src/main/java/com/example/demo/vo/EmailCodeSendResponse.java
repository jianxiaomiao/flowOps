package com.example.demo.vo;

/** 发送邮箱验证码成功响应。 */
public record EmailCodeSendResponse(
        long cooldownSeconds,
        long expiresInSeconds
) {
}
