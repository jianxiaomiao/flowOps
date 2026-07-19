package com.example.demo.dto.ticket;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class TicketPageQuery {
    @NotNull @Positive private Long tenantId;
    @Min(1) private long pageNum = 1;
    @Min(1) @Max(100) private long pageSize = 10;
    private String status; private String priority; private Long creatorId; private Long assigneeId; private Long handlingDepartmentId; private String title;
}
