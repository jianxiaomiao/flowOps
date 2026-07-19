package com.example.demo.dto.ticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TicketTransitionRequest {
    @NotNull @Positive private Long tenantId;
    @NotBlank private String action;
    @NotNull @Positive private Long operatorId;
    @NotNull private Integer version;
    @Size(max = 500) private String remark;
    private String resolution;
}
