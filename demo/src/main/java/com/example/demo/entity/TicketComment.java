package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/** 不可编辑的工单沟通记录。 */
@Data @TableName("ticket_comment")
public class TicketComment {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long ticketId;

    private Long authorId;

    private String content;

    private String visibility;

    private LocalDateTime createdAt;
}
