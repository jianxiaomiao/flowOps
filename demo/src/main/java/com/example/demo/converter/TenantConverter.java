package com.example.demo.converter;

import com.example.demo.dto.TenantCreateRequest;
import com.example.demo.dto.TenantUpdateRequest;
import com.example.demo.entity.Tenant;
import com.example.demo.vo.TenantResponse;

/**
 * Tenant 模块的数据转换器。
 *
 * <p>这里仅负责字段复制，不放数据库访问、状态流转等业务规则。</p>
 */
public final class TenantConverter {

    private TenantConverter() {
    }

    /** 将创建请求转换为待保存的实体。 */
    public static Tenant toEntity(TenantCreateRequest request) {
        Tenant tenant = new Tenant();
        tenant.setTenantCode(request.getTenantCode());
        tenant.setTenantName(request.getTenantName());
        tenant.setContactName(request.getContactName());
        tenant.setContactEmail(request.getContactEmail());
        tenant.setContactPhone(request.getContactPhone());
        return tenant;
    }

    /** 将可修改的字段覆盖到已存在的实体上。 */
    public static void copyToEntity(TenantUpdateRequest request, Tenant tenant) {
        tenant.setTenantName(request.getTenantName());
        tenant.setContactName(request.getContactName());
        tenant.setContactEmail(request.getContactEmail());
        tenant.setContactPhone(request.getContactPhone());
    }

    /** 将数据库实体转换为接口响应。 */
    public static TenantResponse toResponse(Tenant tenant) {
        TenantResponse response = new TenantResponse();
        response.setId(tenant.getId());
        response.setTenantCode(tenant.getTenantCode());
        response.setTenantName(tenant.getTenantName());
        response.setStatus(tenant.getStatus());
        response.setContactName(tenant.getContactName());
        response.setContactEmail(tenant.getContactEmail());
        response.setContactPhone(tenant.getContactPhone());
        response.setCreatedAt(tenant.getCreatedAt());
        response.setUpdatedAt(tenant.getUpdatedAt());
        return response;
    }
}
