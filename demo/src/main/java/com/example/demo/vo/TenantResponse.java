package com.example.demo.vo;

import lombok.Data;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 租户接口返回给客户端的数据。
 */
@Data
public class TenantResponse {

    private Long id;

    private String tenantCode;

    private String tenantName;

    private String status;

    private String contactName;

    private String contactEmail;

    private String contactPhone;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
