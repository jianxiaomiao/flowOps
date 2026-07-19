package com.example.demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.common.Result;
import com.example.demo.converter.SlaPolicyConverter;
import com.example.demo.dto.sla.SlaPolicyCreateRequest;
import com.example.demo.dto.sla.SlaPolicyPageQuery;
import com.example.demo.dto.sla.SlaPolicyUpdateRequest;
import com.example.demo.service.SlaPolicyService;
import com.example.demo.vo.SlaPolicyResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/sla-policy")
@RequiredArgsConstructor
public class SlaPolicyController {
    private final SlaPolicyService slaPolicyService;

    @PostMapping
    public Result<SlaPolicyResponse> create(@Valid @RequestBody SlaPolicyCreateRequest request) {
        return Result.success(SlaPolicyConverter.toResponse(slaPolicyService.createPolicy(request)));
    }
    @PutMapping
    public Result<SlaPolicyResponse> update(@RequestParam @Positive Long tenantId, @Valid @RequestBody SlaPolicyUpdateRequest request) {
        return Result.success(SlaPolicyConverter.toResponse(slaPolicyService.updatePolicy(tenantId, request)));
    }
    @GetMapping("/{id}")
    public Result<SlaPolicyResponse> get(@PathVariable @Positive Long id, @RequestParam @Positive Long tenantId) {
        return Result.success(SlaPolicyConverter.toResponse(slaPolicyService.getPolicy(tenantId, id)));
    }
    @GetMapping("/page")
    public Result<IPage<SlaPolicyResponse>> page(@Valid @ModelAttribute SlaPolicyPageQuery query) {
        return Result.success(slaPolicyService.pagePolicies(query).convert(SlaPolicyConverter::toResponse));
    }
}
