package com.example.demo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TicketResponse {
    private Long id; private Long tenantId; private String ticketNo; private String title; private String description;
    private String ticketType; private String priority; private String status;
    private Long creatorId; private Long creatorDepartmentId; private Long handlingDepartmentId; private Long currentAssigneeId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") private LocalDateTime firstResponseAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") private LocalDateTime resolutionSubmittedAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") private LocalDateTime resolvedAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") private LocalDateTime closedAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") private LocalDateTime responseDueAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") private LocalDateTime resolutionDueAt;
    private String responseSlaStatus; private String resolutionSlaStatus; private String resolution; private Integer version;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") private LocalDateTime updatedAt;
}
