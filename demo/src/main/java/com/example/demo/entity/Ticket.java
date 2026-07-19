package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/** 工单当前快照；历史操作保存在 transition、assignment、comment 表。 */
@Data
@TableName("ticket")
public class Ticket {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String ticketNo;

    private String title;

    private String description;

    private String ticketType;

    private String priority;

    private String status;

    private Long creatorId;

    private Long creatorDepartmentId;

    private Long handlingDepartmentId;

    private Long currentAssigneeId;

    private LocalDateTime firstResponseAt;

    private LocalDateTime resolutionSubmittedAt;

    private LocalDateTime resolvedAt;

    private LocalDateTime closedAt;

    private LocalDateTime responseDueAt;

    private LocalDateTime resolutionDueAt;

    private String responseSlaStatus;

    private String resolutionSlaStatus;

    private LocalDateTime responseBreachedAt;

    private LocalDateTime resolutionBreachedAt;

    private String resolution;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    private Long updatedBy;

    @Version @TableField(fill = FieldFill.INSERT)
    private Integer version;
}
