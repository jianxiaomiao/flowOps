package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/** 不可修改的状态流转流水。 */
@Data @TableName("ticket_transition")
public class TicketTransition {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long ticketId;

    private String action;

    private String fromStatus;

    private String toStatus;

    private Long operatorId;

    private String remark;

    private Integer ticketVersion;

    private LocalDateTime createdAt;
}
