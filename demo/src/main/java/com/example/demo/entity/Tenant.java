package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("`tenant`")
public class Tenant {
    /**
     * 租户ID，自增主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户代码，如 ACME
     */
    private String tenantCode;

    /**
     * 企业名称
     */
    private String tenantName;

    /**
     * 状态：ACTIVE、DISABLED
     */
    private String status;

    /**
     * 联系人
     */
    private String contactName;

    /**
     * 联系邮箱
     */
    private String contactEmail;

    /**
     * 联系电话
     */
    private String contactPhone;

    /**
     * 创建时间 DATETIME(3)
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 平台操作人（创建人ID）
     */
    private Long createdBy;

    /**
     * 更新时间 DATETIME(3)
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 平台操作人（修改人ID）
     */
    private Long updatedBy;

    /**
     * 乐观锁版本号
     */
    @Version
    @TableField(value = "version", fill = FieldFill.INSERT)
    private Integer version;
}
