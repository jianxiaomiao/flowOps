package com.example.demo.dto.ticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TicketCreateRequest {
    @NotNull @Positive private Long handlingDepartmentId;
    @NotBlank @Size(max = 200) private String title;
    @NotBlank private String description;
    @NotBlank private String ticketType;
    @NotBlank private String priority;
}
