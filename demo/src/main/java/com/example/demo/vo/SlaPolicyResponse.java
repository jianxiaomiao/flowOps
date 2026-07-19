package com.example.demo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SlaPolicyResponse {
    private Long id;
    private Long tenantId;
    private String policyName;
    private String priority;
    private Integer responseMinutes;
    private Integer resolutionMinutes;
    private Boolean enabled;
    private Integer version;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
