package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/** 文章与工单的租户内关联，禁止客户端提供 tenantId。 */
@Data
@TableName("knowledge_article_ticket")
public class KnowledgeArticleTicket {
    @TableId(type = IdType.AUTO) private Long id;
    private Long tenantId;
    private Long articleId;
    private Long ticketId;
    private String relationType;
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
}
