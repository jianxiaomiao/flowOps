package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码摘要策略。
 *
 * <p>应用代码依赖 PasswordEncoder 接口，而非 BCrypt 的具体实现，便于后续升级算法。</p>
 */
@Configuration
public class PasswordConfig {

    /**
     * 创建项目统一使用的密码摘要器。
     *
     * @return BCrypt 实现的密码编码器；它会为每次编码自动生成并保存盐值。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
