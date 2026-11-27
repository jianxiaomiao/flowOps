package com.example.demo.vo;

/** 图形算术验证码返回结果。 */
public record CaptchaResponse(
        String captchaId,
        String imageDataUrl,
        long expiresInSeconds
) {
}
