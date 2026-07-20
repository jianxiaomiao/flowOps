package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("processed_event")
public class ProcessedEvent {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String eventId;
    private String consumerName;
    private LocalDateTime processedAt;
}
