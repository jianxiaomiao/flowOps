package com.example.demo.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 发送注册邮箱验证码请求。 */
public record SendEmailCodeRequest(
        @NotBlank(message = "请输入邮箱地址")
        @Email(message = "邮箱格式不正确")
        @Size(max = 128, message = "邮箱长度不能超过 128 位")
        String email,

        @NotBlank(message = "请先获取图形验证码")
        @Size(max = 64, message = "图形验证码标识无效")
        String captchaId,

        @NotBlank(message = "请输入图形验证码答案")
        @Size(max = 16, message = "图形验证码答案无效")
        String captchaAnswer
) {
}
