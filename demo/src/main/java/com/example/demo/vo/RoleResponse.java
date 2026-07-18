package com.example.demo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/** 角色接口响应对象。 */
@Data
public class RoleResponse {

    private Long id;
    private Long tenantId;
    private String roleCode;
    private String roleName;
    private String description;
    private Boolean builtIn;
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
