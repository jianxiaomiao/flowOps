package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("email_verification_code")
public class EmailVerificationCode {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String email;

    private String purpose;

    private String codeHash;

    private String status;

    private Integer failedAttempts;

    private Integer maxAttempts;

    private String requestIp;

    private LocalDateTime expiresAt;

    private LocalDateTime verifiedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
