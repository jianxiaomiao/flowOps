package com.example.demo.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** 负责将验证码邮件通过 SMTP 发送；本地未配置发信账号时仅在非生产环境提示。 */
@Slf4j
@Component
public class VerificationMailSender {
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final Environment environment;
    private final String fromAddress;

    public VerificationMailSender(ObjectProvider<JavaMailSender> mailSenderProvider,
                                  Environment environment,
                                  @Value("${spring.mail.username:}") String fromAddress) {
        this.mailSenderProvider = mailSenderProvider;
        this.environment = environment;
        this.fromAddress = fromAddress == null ? "" : fromAddress.trim();
    }

    public void sendRegistrationCode(String targetEmail, String code, long expiresMinutes) {
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null || fromAddress.isBlank()) {
            if (environment.acceptsProfiles(Profiles.of("prod"))) {
                throw new IllegalStateException("邮件服务未配置 FLOWOPS_MAIL_USERNAME / FLOWOPS_MAIL_PASSWORD");
            }
            log.warn("本地未配置 FLOWOPS_MAIL_USERNAME，跳过真实 SMTP 发信，仅开发调试日志输出验证码: email={}, code={}",
                    maskEmail(targetEmail), code);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(targetEmail);
        message.setSubject("FlowOps 企业注册验证码");
        message.setText("您好，您正在注册 FlowOps 企业工作空间。\n\n"
                + "本次邮箱验证码为：" + code + "\n"
                + "有效期 " + expiresMinutes + " 分钟，请勿向他人泄露。\n\n"
                + "如非本人操作，请忽略此邮件。");
        sender.send(message);
    }

    private String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 2) {
            return "***" + (at >= 0 ? email.substring(at) : "");
        }
        return email.substring(0, 2) + "***" + email.substring(at);
    }
}
