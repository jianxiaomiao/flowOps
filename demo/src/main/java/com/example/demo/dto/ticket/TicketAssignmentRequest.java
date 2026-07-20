package com.example.demo.dto.ticket;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TicketAssignmentRequest {
    @NotNull @Positive private Long toDepartmentId;
    @Positive private Long toAssigneeId;
    @Size(max = 500) private String reason;
    @NotNull private Integer version;
}
