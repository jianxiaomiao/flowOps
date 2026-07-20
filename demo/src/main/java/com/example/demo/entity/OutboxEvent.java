package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("outbox_event")
public class OutboxEvent {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String eventId;
    private Long tenantId;
    private String aggregateType;
    private Long aggregateId;
    private String eventType;
    private String payload;
    private String status;
    private Integer retryCount;
    private LocalDateTime nextRetryAt;
    private LocalDateTime publishedAt;
    private String lastError;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
