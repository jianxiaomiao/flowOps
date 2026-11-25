package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

/** 租户内用户账号。 */
@Data
@TableName("`user_account`")
public class UserAccount {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long departmentId;
    private String username;

    /** 仅保存 BCrypt 生成的摘要，绝不保存明文密码。 */
    private String passwordHash;

    private String displayName;
    private String email;
    private String phone;

    /** ACTIVE、LOCKED、DISABLED。 */
    private String status;

    private String accountKind;

    /** 仅在认证成功后更新。 */
    private LocalDateTime lastLoginAt;

    /** 仅在密码修改成功后更新。 */
    private LocalDateTime passwordChangedAt;

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
