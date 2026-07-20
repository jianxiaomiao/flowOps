package com.example.demo.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Size(max = 32) String tenantCode,
        @NotBlank @Size(max = 64) String username,
        @NotBlank @Size(max = 72) String password
) {
}
