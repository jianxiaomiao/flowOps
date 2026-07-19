package com.example.demo.vo;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;
@Data public class TicketAssignmentResponse {
    private Long id; private String assignmentType; private Long fromDepartmentId; private Long toDepartmentId; private Long fromAssigneeId; private Long toAssigneeId; private Long assignedBy; private String reason; private Integer ticketVersion;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") private LocalDateTime createdAt;
}
