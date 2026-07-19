package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

/** 租户按优先级配置的响应和解决时限。 */
@Data
@TableName("sla_policy")
public class SlaPolicy {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String policyName;
    private String priority;
    private Integer responseMinutes;
    private Integer resolutionMinutes;
    private Boolean enabled;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    private Long updatedBy;
    @Version
    @TableField(fill = FieldFill.INSERT)
    private Integer version;
}
