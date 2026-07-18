package com.example.demo.dto.tenant;

import lombok.Data;

/**
 * 创建租户时允许客户端提交的数据。
 *
 * <p>这里故意不包含 id、status、创建/更新时间和 version：这些字段由后端控制。</p>
 */
@Data
public class TenantCreateRequest {

    private String tenantCode;

    private String tenantName;

    private String contactName;

    private String contactEmail;

    private String contactPhone;
}
