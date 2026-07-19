package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/** 分派当前值变化的历史流水。 */
@Data @TableName("ticket_assignment")
public class TicketAssignment {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long ticketId;

    private String assignmentType;

    private Long fromDepartmentId;

    private Long toDepartmentId;

    private Long fromAssigneeId;

    private Long toAssigneeId;

    private Long assignedBy;

    private String reason;

    private Integer ticketVersion;

    //"分派动作发生的业务时间"，不是单纯的数据库插入时间。
    private LocalDateTime createdAt;
}
