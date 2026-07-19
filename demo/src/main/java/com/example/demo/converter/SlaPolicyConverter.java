package com.example.demo.converter;

import com.example.demo.dto.sla.SlaPolicyCreateRequest;
import com.example.demo.dto.sla.SlaPolicyUpdateRequest;
import com.example.demo.entity.SlaPolicy;
import com.example.demo.vo.SlaPolicyResponse;
import org.springframework.beans.BeanUtils;

public final class SlaPolicyConverter {
    private SlaPolicyConverter() { }
    public static SlaPolicy toEntity(SlaPolicyCreateRequest source) {
        SlaPolicy target = new SlaPolicy();
        BeanUtils.copyProperties(source, target);
        return target;
    }
    public static void copyToEntity(SlaPolicyUpdateRequest source, SlaPolicy target) {
        if (source.getPolicyName() != null) {
            target.setPolicyName(source.getPolicyName());
        }
        if (source.getResponseMinutes() != null) {
            target.setResponseMinutes(source.getResponseMinutes());
        }
        if (source.getResolutionMinutes() != null) {
            target.setResolutionMinutes(source.getResolutionMinutes());
        }
        if (source.getEnabled() != null) {
            target.setEnabled(source.getEnabled());
        }
    }
    public static SlaPolicyResponse toResponse(SlaPolicy source) {
        SlaPolicyResponse target = new SlaPolicyResponse();
        BeanUtils.copyProperties(source, target);
        return target;
    }
}
