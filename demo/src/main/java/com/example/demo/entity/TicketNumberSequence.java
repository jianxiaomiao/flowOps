package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 每租户每日的工单展示编号序列。 */
@Data @TableName("ticket_number_sequence")
public class TicketNumberSequence {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private LocalDate sequenceDate;

    private Integer currentValue;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @Version @TableField(fill = FieldFill.INSERT)
    private Integer version;
}
