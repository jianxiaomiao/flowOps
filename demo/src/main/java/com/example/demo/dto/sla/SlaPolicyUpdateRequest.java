package com.example.demo.dto.sla;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SlaPolicyUpdateRequest {
    @NotNull @Positive
    private Long id;
    @Size(max = 100)
    private String policyName;
    @Positive
    private Integer responseMinutes;
    @Positive
    private Integer resolutionMinutes;
    private Boolean enabled;
    @NotNull
    private Integer version;
}
