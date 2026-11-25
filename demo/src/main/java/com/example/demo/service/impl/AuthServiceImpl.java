package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.demo.common.BusinessException;
import com.example.demo.config.JwtProperties;
import com.example.demo.dto.auth.LoginRequest;
import com.example.demo.entity.Tenant;
import com.example.demo.entity.UserAccount;
import com.example.demo.mapper.RolePermissionMapper;
import com.example.demo.mapper.TenantMapper;
import com.example.demo.mapper.UserAccountMapper;
import com.example.demo.service.AuthService;
import com.example.demo.security.TokenSessionService;
import com.example.demo.vo.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
/**
 * 登录用例的实现：校验租户和账号、比较 BCrypt 摘要、加载权限并签发 JWT。
 */
public class AuthServiceImpl implements AuthService {
    private final TenantMapper tenantMapper;
    private final UserAccountMapper userAccountMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;
    private final TokenSessionService tokenSessionService;

    @Override
    @Transactional
    /**
     * 执行一次完整登录。
     *
     * <p>为避免账号枚举攻击，租户不存在、用户不存在、账号停用和密码错误都会返回同一种认证失败信息。</p>
     *
     * @param request 包含租户编码、用户名和明文密码的登录请求。
     * @return 已签名 JWT 及其有效期。
     * @throws BusinessException 当租户、账号、密码或账号状态不满足登录条件时抛出。
     */
    public LoginResponse login(LoginRequest request) {
        Tenant tenant = tenantMapper.selectOne(Wrappers.lambdaQuery(Tenant.class)
                .eq(Tenant::getTenantCode, request.tenantCode().trim())
                .eq(Tenant::getStatus, "ACTIVE"));
        if (tenant == null) {
            throw invalidCredentials();
        }
        String username = request.username().trim().toLowerCase(Locale.ROOT);
        UserAccount user = userAccountMapper.selectOne(Wrappers.lambdaQuery(UserAccount.class)
                .eq(UserAccount::getTenantId, tenant.getId())
                .eq(UserAccount::getUsername, username));
        if (user == null || !"ACTIVE".equals(user.getStatus()) || "MACHINE".equals(user.getAccountKind())
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }

        //确定签发与过期时间
        //→ 组装 JWT 身份与权限数据（Claims）
        //→ 指定签名算法（Header）
        //→ 编码并签名，得到 Token 字符串
        List<String> authorities = rolePermissionMapper.selectActivePermissionCodes(tenant.getId(), user.getId());
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(jwtProperties.accessTokenTtl());
        long sessionVersion = tokenSessionService.currentVersion(tenant.getId(), user.getId());
        //.issuer(...)	iss	谁签发了 Token；这里是 FlowOps
        //.issuedAt(...)	iat	何时签发
        //.expiresAt(...)	exp	何时过期
        //.subject(username)	sub	这张 Token 的主体是谁；这里用登录名
        //.claim("user_id", ...)	自定义字段	当前用户 ID
        //.claim("tenant_id", ...)	自定义字段	当前用户属于哪个租户
        //.claim("authorities", ...)	自定义字段	当前拥有的权限编码
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .subject(username)
                .claim("user_id", user.getId())
                .claim("tenant_id", tenant.getId())
                .claim("session_version", sessionVersion)
                .claim("authorities", authorities)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        //把 header 转成 JSON；
        //把 claims 转成 JSON；
        //使用 SecurityConfig 中的密钥，对前两部分进行 HS256 签名。
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        userAccountMapper.update(null, Wrappers.lambdaUpdate(UserAccount.class)
                .eq(UserAccount::getId, user.getId())
                .eq(UserAccount::getTenantId, tenant.getId())
                .set(UserAccount::getLastLoginAt, LocalDateTime.now()));
        return new LoginResponse(token, "Bearer", expiresAt);
    }

    /**
     * 创建不暴露具体失败细节的认证异常。
     *
     * @return 业务错误码为 10002 的认证失败异常。
     */
    private BusinessException invalidCredentials() {
        return new BusinessException(10002, "租户、用户名或密码不正确，或者账号已停用");
    }
}
