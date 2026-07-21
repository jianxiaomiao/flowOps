package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/** 知识文章的事实来源；向量索引未来只能引用其 ID，不取代此表的权限和状态。 */
@Data
@TableName("knowledge_article")
public class KnowledgeArticle {
    @TableId(type = IdType.AUTO) private Long id;
    private Long tenantId;
    private Long categoryId;
    private String title;
    private String summary;
    private String content;
    private String status;
    private String visibility;
    private Long departmentId;
    private Long authorId;
    private LocalDateTime publishedAt;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
    private Long updatedBy;
    @Version @TableField(fill = FieldFill.INSERT) private Integer version;
}
