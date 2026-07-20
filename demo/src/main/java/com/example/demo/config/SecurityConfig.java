package com.example.demo.config;

import com.example.demo.security.ApiAccessDeniedHandler;
import com.example.demo.security.ApiAuthenticationEntryPoint;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties({JwtProperties.class, RegistrationProperties.class})
/**
 * 配置 FlowOps 的无状态 JWT 安全机制。
 *
 * <p>它负责定义哪些路径公开、JWT 如何签发和验证，以及认证或授权失败时返回什么响应。
 * 具体的登录业务在 {@code AuthServiceImpl} 中完成。</p>
 */
public class SecurityConfig {

    /**
     * 创建所有 HTTP 请求必须经过的 Spring Security 过滤器链。
     *
     * @param http Spring Security 提供的 HTTP 安全规则构建器。
     * @param authenticationEntryPoint 未认证或 JWT 无效时的 401 响应处理器。
     * @param accessDeniedHandler 已认证但权限不足时的 403 响应处理器。
     * @param jwtAuthenticationConverter 将 JWT 中的 authorities 声明转换为 Spring 权限。
     * @return 配置完成的安全过滤器链。
     * @throws Exception 配置过滤器链时 Spring Security 可能抛出的异常。
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            ApiAuthenticationEntryPoint authenticationEntryPoint,
                                            ApiAccessDeniedHandler accessDeniedHandler,
                                            JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        return http
                //CSRF 是浏览器 Cookie 登录场景中的防跨站请求伪造保护。采用 Authorization: Bearer <JWT>，不依赖浏览器自动携带 Cookie，所以关闭 CSRF 是合适的。
                .csrf(csrf -> csrf.disable())
                //服务器不会保存“用户 A 已登录”的 Session；每次请求都必须自己带 JWT。这样后端扩容时，多个实例不需要共享 Session。
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                //这是第一层、粗粒度的访问规则：
                ///auth/login：所有人都能访问，否则无法登录。
                ///error：放行 Spring 的错误路径，避免错误处理又被安全拦截。
                //其余所有接口：必须先认证，即带有效 JWT。
                //注意：authenticated() 只代表“你是谁已经确认”，不代表“你能做什么”。
                //例如处理人已经登录，所以能通过这里；但他访问角色管理接口时，会在下一层 @PreAuthorize 被拒绝。
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/auth/login", "/auth/register", "/error").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .build();
    }

    /**
     * 指定 JWT 权限声明与 Spring Security GrantedAuthority 之间的转换规则。
     *
     * @return 从 {@code authorities} 声明读取、且不添加 {@code SCOPE_} 前缀的转换器。
     */
    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("authorities");
        authoritiesConverter.setAuthorityPrefix("");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }

    /**
     * 将本地配置中的 JWT 密钥转换为 HS256 使用的密钥对象。
     *
     * @param properties 已绑定的 JWT 配置，密钥通常来自未提交到 Git 的本地配置。
     * @return 用于 JWT 签名和验签的 HMAC-SHA256 密钥。
     * @throws IllegalStateException 当密钥缺失或少于 32 字节时抛出，避免以弱密钥启动。
     */
    @Bean
    SecretKey jwtSecretKey(JwtProperties properties) {
        String secret = properties.secret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("FLOWOPS_JWT_SECRET 必须至少 32 字节");
        }
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    /**
     * 创建登录成功后签发 JWT 的编码器。
     *
     * @param secretKey 用于 JWT 签名的 HS256 密钥。
     * @return JWT 编码器。
     */
    @Bean
    JwtEncoder jwtEncoder(SecretKey secretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(secretKey));
    }

    /**
     * 创建每次请求验证 Bearer Token 时使用的 JWT 解码器。
     *
     * <p>解码器验证 HS256 签名、标准有效期和发行者；验证失败后请求会进入 401 处理器。</p>
     *
     * @param secretKey 用于验签的 HS256 密钥，必须与签发时使用的密钥一致。
     * @param properties JWT 发行者等校验配置。
     * @return 已配置签名算法和校验器的 JWT 解码器。
     */
    @Bean
    JwtDecoder jwtDecoder(SecretKey secretKey, JwtProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(properties.issuer()));
        decoder.setJwtValidator(validator);
        return decoder;
    }
}
