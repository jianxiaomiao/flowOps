package com.example.demo.dto.sla;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SlaPolicyCreateRequest {
    @NotNull @Positive
    private Long tenantId;
    @NotBlank @Size(max = 100)
    private String policyName;
    @NotBlank
    private String priority;
    @NotNull @Positive
    private Integer responseMinutes;
    @NotNull @Positive
    private Integer resolutionMinutes;
    @Positive
    private Long operatorId;
}
