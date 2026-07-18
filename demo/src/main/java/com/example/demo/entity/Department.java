package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("`department`")
public class Department {
    /**
     * 部门ID 主键自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属租户ID
     */
    private Long tenantId;

    /**
     * 上级部门ID；根部门为空
     */
    private Long parentId;

    /**
     * 租户内部部门代码
     */
    private String departmentCode;

    /**
     * 部门名称
     */
    private String departmentName;

    /**
     * 部门负责人用户ID
     */
    private Long leaderUserId;

    /**
     * 展示排序
     */
    private Integer sortOrder;

    /**
     * 状态：ACTIVE 启用 / DISABLED 禁用
     */
    private String status;

    /**
     * 创建时间（毫秒精度datetime(3)）
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 创建人用户ID
     */
    private Long createdBy;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 修改人用户ID
     */
    private Long updatedBy;

    /**
     * 乐观锁版本号
     */
    @Version
    @TableField(value = "version", fill = FieldFill.INSERT)
    private Integer version;
}
