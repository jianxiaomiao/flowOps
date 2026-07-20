package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("notification")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long recipientId;
    private String notificationType;
    private String title;
    private String content;
    private String businessType;
    private Long businessId;
    private String sourceEventId;
    private LocalDateTime readAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
