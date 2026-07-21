package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/** 租户内的知识分类；分类本身不跨租户共享。 */
@Data
@TableName("knowledge_category")
public class KnowledgeCategory {
    @TableId(type = IdType.AUTO) private Long id;
    private Long tenantId;
    private Long parentId;
    private String categoryCode;
    private String categoryName;
    private String description;
    private Integer sortOrder;
    private String status;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
    private Long updatedBy;
    @Version @TableField(fill = FieldFill.INSERT) private Integer version;
}
