package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.example.demo.common.BusinessException;
import com.example.demo.config.JwtProperties;
import com.example.demo.dto.auth.LoginRequest;
import com.example.demo.entity.Tenant;
import com.example.demo.entity.UserAccount;
import com.example.demo.mapper.RolePermissionMapper;
import com.example.demo.mapper.TenantMapper;
import com.example.demo.mapper.UserAccountMapper;
import com.example.demo.security.TokenSessionService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
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
    @Mock TokenSessionService tokenSessionService;

    @BeforeAll
    static void registerUserAccountMapperMetadata() {
        var assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "auth-unit-test");
        assistant.setCurrentNamespace(UserAccountMapper.class.getName());
        TableInfoHelper.initTableInfo(assistant, UserAccount.class);
    }

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
                passwordEncoder, jwtEncoder, new JwtProperties("flowops", Duration.ofMinutes(30), "unused"), tokenSessionService);
        var response = service.login(new LoginRequest("ACME", "ADMIN", "password123"));

        assertEquals("signed-token", response.accessToken());
        ArgumentCaptor<JwtEncoderParameters> captor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(jwtEncoder).encode(captor.capture());
        assertEquals(Long.valueOf(1L), captor.getValue().getClaims().getClaim("tenant_id"));
        assertEquals(Long.valueOf(9L), captor.getValue().getClaims().getClaim("user_id"));
        assertNotNull(captor.getValue().getClaims().getId());
        assertEquals(Long.valueOf(0L), captor.getValue().getClaims().getClaim("session_version"));
        assertEquals(List.of("ticket:create", "role:manage"), captor.getValue().getClaims().getClaim("authorities"));
    }

    @Test
    void shouldNotRevealWhetherTenantOrUserExists() {
        when(tenantMapper.selectOne(any())).thenReturn(null);
        AuthServiceImpl service = new AuthServiceImpl(tenantMapper, userAccountMapper, rolePermissionMapper,
                passwordEncoder, jwtEncoder, new JwtProperties("flowops", Duration.ofMinutes(30), "unused"), tokenSessionService);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.login(new LoginRequest("UNKNOWN", "nobody", "password123")));
        assertEquals(10002, error.getCode());
        verifyNoInteractions(userAccountMapper, passwordEncoder, jwtEncoder);
    }

    @Test
    void shouldRejectUnknownUserWithSameGenericError() {
        when(tenantMapper.selectOne(any())).thenReturn(activeTenant());
        when(userAccountMapper.selectOne(any())).thenReturn(null);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service().login(new LoginRequest("ACME", "unknown", "password123")));

        assertEquals(10002, error.getCode());
        verifyNoInteractions(passwordEncoder, rolePermissionMapper, jwtEncoder);
    }

    @Test
    void shouldRejectDisabledUserWithoutCheckingPassword() {
        UserAccount disabledUser = activeUser();
        disabledUser.setStatus("DISABLED");
        when(tenantMapper.selectOne(any())).thenReturn(activeTenant());
        when(userAccountMapper.selectOne(any())).thenReturn(disabledUser);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service().login(new LoginRequest("ACME", "disabled", "password123")));

        assertEquals(10002, error.getCode());
        verifyNoInteractions(passwordEncoder, rolePermissionMapper, jwtEncoder);
        verify(userAccountMapper, never()).update(any(), any());
    }

    @Test
    void shouldRejectMachineAccountWithoutCheckingPassword() {
        UserAccount machine = activeUser();
        machine.setAccountKind("MACHINE");
        when(tenantMapper.selectOne(any())).thenReturn(activeTenant());
        when(userAccountMapper.selectOne(any())).thenReturn(machine);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service().login(new LoginRequest("ACME", "xiaomiao", "password123")));

        assertEquals(10002, error.getCode());
        verifyNoInteractions(passwordEncoder, rolePermissionMapper, jwtEncoder, tokenSessionService);
        verify(userAccountMapper, never()).update(any(), any());
    }

    @Test
    void shouldRejectWrongPasswordWithoutIssuingToken() {
        when(tenantMapper.selectOne(any())).thenReturn(activeTenant());
        when(userAccountMapper.selectOne(any())).thenReturn(activeUser());
        when(passwordEncoder.matches("wrong-password", "hash")).thenReturn(false);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service().login(new LoginRequest("ACME", "xiaomiao", "wrong-password")));

        assertEquals(10002, error.getCode());
        verifyNoInteractions(rolePermissionMapper, jwtEncoder);
        verify(userAccountMapper, never()).update(any(), any());
    }

    @Test
    void shouldIssueTokenEvenWhenActiveUserHasNoPermissions() {
        when(tenantMapper.selectOne(any())).thenReturn(activeTenant());
        when(userAccountMapper.selectOne(any())).thenReturn(activeUser());
        when(passwordEncoder.matches("password123", "hash")).thenReturn(true);
        when(rolePermissionMapper.selectActivePermissionCodes(1L, 1L)).thenReturn(List.of());
        Jwt encoded = Jwt.withTokenValue("signed-token").header("alg", "HS256")
                .subject("xiaomiao").issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        when(jwtEncoder.encode(any())).thenReturn(encoded);

        var response = service().login(new LoginRequest("ACME", "xiaomiao", "password123"));

        assertEquals("signed-token", response.accessToken());
        ArgumentCaptor<JwtEncoderParameters> captor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(jwtEncoder).encode(captor.capture());
        assertEquals(List.of(), captor.getValue().getClaims().getClaim("authorities"));
        verify(userAccountMapper).update(isNull(), any());
    }

    private AuthServiceImpl service() {
        return new AuthServiceImpl(tenantMapper, userAccountMapper, rolePermissionMapper,
                passwordEncoder, jwtEncoder, new JwtProperties("flowops", Duration.ofMinutes(30), "unused"), tokenSessionService);
    }

    private Tenant activeTenant() {
        Tenant tenant = new Tenant();
        tenant.setId(1L);
        tenant.setTenantCode("ACME");
        tenant.setStatus("ACTIVE");
        return tenant;
    }

    private UserAccount activeUser() {
        UserAccount user = new UserAccount();
        user.setId(1L);
        user.setTenantId(1L);
        user.setUsername("xiaomiao");
        user.setStatus("ACTIVE");
        user.setPasswordHash("hash");
        return user;
    }
}
