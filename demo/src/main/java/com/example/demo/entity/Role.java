package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

/** 租户内的角色定义。 */
@Data
@TableName("`role`")
public class Role {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private String roleCode;
    private String roleName;
    private String description;

    /** 系统预置角色；普通接口创建的角色固定为 false。 */
    private Boolean builtIn;

    /** ACTIVE、DISABLED。 */
    private String status;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    private Long createdBy;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    private Long updatedBy;

    @Version
    @TableField(value = "version", fill = FieldFill.INSERT)
    private Integer version;
}
