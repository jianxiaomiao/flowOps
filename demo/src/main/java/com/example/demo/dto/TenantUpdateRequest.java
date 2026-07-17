package com.example.demo.dto;

import lombok.Data;

/**
 * 修改租户基本资料时允许客户端提交的数据。
 *
 * <p>tenantCode 是租户的稳定标识，第一版不允许修改；status 会通过单独的启用/停用接口修改。</p>
 */
@Data
public class TenantUpdateRequest {

    private Long id;

    private String tenantName;

    private String contactName;

    private String contactEmail;

    private String contactPhone;
}
