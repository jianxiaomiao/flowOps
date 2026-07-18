package com.example.demo.dto.user;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 用户账号分页查询条件。 */
@Data
public class UserAccountPageQuery {

    @Min(value = 1, message = "pageNum 不能小于 1")
    private Integer pageNum = 1;

    @Min(value = 1, message = "pageSize 不能小于 1")
    @Max(value = 100, message = "pageSize 不能超过 100")
    private Integer pageSize = 10;

    /** 当前阶段必传；接入认证后由租户上下文提供。 */
    @NotNull(message = "tenantId 不能为空")
    private Long tenantId;
    private Long departmentId;
    private String username;
    private String displayName;
    private String status;
}
