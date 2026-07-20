package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("audit_log")
public class AuditLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String module;
    private String operation;
    private String resourceType;
    private Long resourceId;
    private Long operatorId;
    private String traceId;
    private String requestIp;
    private String result;
    private String beforeData;
    private String afterData;
    private String errorMessage;
    private LocalDateTime occurredAt;
}
