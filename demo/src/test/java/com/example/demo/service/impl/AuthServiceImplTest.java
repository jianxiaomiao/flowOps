package com.example.demo.service.impl;

import com.example.demo.common.BusinessException;
import com.example.demo.config.JwtProperties;
import com.example.demo.dto.auth.LoginRequest;
import com.example.demo.entity.Tenant;
import com.example.demo.entity.UserAccount;
import com.example.demo.mapper.RolePermissionMapper;
import com.example.demo.mapper.TenantMapper;
import com.example.demo.mapper.UserAccountMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {
    @Mock TenantMapper tenantMapper;
    @Mock UserAccountMapper userAccountMapper;
    @Mock RolePermissionMapper rolePermissionMapper;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtEncoder jwtEncoder;

    @Test
    void shouldIssueTokenWithTrustedIdentityAndAuthorities() {
        Tenant tenant = new Tenant(); tenant.setId(1L); tenant.setTenantCode("ACME"); tenant.setStatus("ACTIVE");
        UserAccount user = new UserAccount(); user.setId(9L); user.setTenantId(1L); user.setUsername("admin"); user.setStatus("ACTIVE"); user.setPasswordHash("hash");
        when(tenantMapper.selectOne(any())).thenReturn(tenant);
        when(userAccountMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("password123", "hash")).thenReturn(true);
        when(rolePermissionMapper.selectActivePermissionCodes(1L, 9L)).thenReturn(List.of("ticket:create", "role:manage"));
        Jwt encoded = Jwt.withTokenValue("signed-token").header("alg", "HS256")
                .subject("admin").issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        when(jwtEncoder.encode(any())).thenReturn(encoded);

        AuthServiceImpl service = new AuthServiceImpl(tenantMapper, userAccountMapper, rolePermissionMapper,
                passwordEncoder, jwtEncoder, new JwtProperties("flowops", Duration.ofMinutes(30), "unused"));
        var response = service.login(new LoginRequest("ACME", "ADMIN", "password123"));

        assertEquals("signed-token", response.accessToken());
        ArgumentCaptor<JwtEncoderParameters> captor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(jwtEncoder).encode(captor.capture());
        assertEquals(Long.valueOf(1L), captor.getValue().getClaims().getClaim("tenant_id"));
        assertEquals(Long.valueOf(9L), captor.getValue().getClaims().getClaim("user_id"));
        assertEquals(List.of("ticket:create", "role:manage"), captor.getValue().getClaims().getClaim("authorities"));
    }

    @Test
    void shouldNotRevealWhetherTenantOrUserExists() {
        when(tenantMapper.selectOne(any())).thenReturn(null);
        AuthServiceImpl service = new AuthServiceImpl(tenantMapper, userAccountMapper, rolePermissionMapper,
                passwordEncoder, jwtEncoder, new JwtProperties("flowops", Duration.ofMinutes(30), "unused"));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.login(new LoginRequest("UNKNOWN", "nobody", "password123")));
        assertEquals(10002, error.getCode());
        verifyNoInteractions(userAccountMapper, passwordEncoder, jwtEncoder);
    }
}
