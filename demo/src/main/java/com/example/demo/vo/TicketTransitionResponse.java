package com.example.demo.vo;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;
@Data public class TicketTransitionResponse {
    private Long id; private String action; private String fromStatus; private String toStatus; private Long operatorId; private String remark; private Integer ticketVersion;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") private LocalDateTime createdAt;
}
