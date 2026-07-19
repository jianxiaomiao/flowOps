package com.example.demo.converter;

import com.example.demo.entity.Permission;
import com.example.demo.vo.PermissionResponse;
import org.springframework.beans.BeanUtils;

public final class PermissionConverter {
    private PermissionConverter() { }
    public static PermissionResponse toResponse(Permission source) {
        PermissionResponse target = new PermissionResponse();
        BeanUtils.copyProperties(source, target);
        return target;
    }
}
