package com.example.demo.controller;



import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.Result;
import com.example.demo.converter.DepartmentConverter;
import com.example.demo.dto.department.DepartmentCreateRequest;
import com.example.demo.dto.department.DepartmentPageQuery;
import com.example.demo.dto.department.DepartmentUpdateRequest;
import com.example.demo.entity.Department;
import com.example.demo.service.DepartmentService;
import com.example.demo.vo.DepartmentResponse;
import jakarta.annotation.Resource;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/department")
public class DepartmentController {
    @Resource
    private DepartmentService departmentService;

    /**
     * 新增部门
     * DTO 只接收客户端允许填写的字段；状态和审计字段由后端设置。
     */
    @PostMapping
    public Result<DepartmentResponse> save(@RequestBody DepartmentCreateRequest request) {
        Department department = DepartmentConverter.toEntity(request);
        department.setStatus("ACTIVE");
        departmentService.save(department);
        return Result.success(DepartmentConverter.toResponse(department));
    }

    /**
     * 修改部门
     * departmentCode 和 status 不在本接口中修改，避免普通资料修改影响部门
     */
    @PutMapping
    public Result<DepartmentResponse> update(@RequestBody DepartmentUpdateRequest request){
        Department department = departmentService.getById(request.getId());
        if (department == null) {
            return Result.failure(20001, "部门不存在");
        }
        DepartmentConverter.copyToEntity(request, department);
        departmentService.updateById(department);
        return Result.success(DepartmentConverter.toResponse(department));
    }

    /**
     * 获取单个部门
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public Result<DepartmentResponse> getOne(@PathVariable Long id){
        Department department = departmentService.getById(id);
        if (department == null) {
            return Result.failure(20001, "部门不存在");
        }
        return Result.success(DepartmentConverter.toResponse(department));
    }

    /**
     * 获取全部部门
     * @return
     */
    @GetMapping
    public Result<List<DepartmentResponse>> list() {
        List<DepartmentResponse> responseList = departmentService.list().stream()
                .map(DepartmentConverter::toResponse)
                .toList();
        return Result.success(responseList);
    }


    @GetMapping("/page")
    public Result<Page<DepartmentResponse>> findPage(
            @ModelAttribute DepartmentPageQuery query) {

        // 当前还没有登录上下文，tenantId 必须由请求参数传入。
        // 不加这个限制可能会查出所有租户的部门。
        if (query.getTenantId() == null) {
            return Result.failure(10001, "tenantId 不能为空");
        }

        LambdaQueryWrapper<Department> wrapper = Wrappers.lambdaQuery(Department.class)
                // tenantId 必须参与查询，保证当前阶段的租户隔离
                .eq(Department::getTenantId, query.getTenantId())

                // parentId 可选：传了就查询该父部门的直接子部门
                .eq(query.getParentId() != null,
                        Department::getParentId, query.getParentId())

                // 部门编码精确匹配
                .eq(StringUtils.hasText(query.getDepartmentCode()),
                        Department::getDepartmentCode, query.getDepartmentCode())

                // 部门名称模糊匹配
                .like(StringUtils.hasText(query.getDepartmentName()),
                        Department::getDepartmentName, query.getDepartmentName())

                // 状态精确匹配
                .eq(StringUtils.hasText(query.getStatus()),
                        Department::getStatus, query.getStatus())

                // 同级数据按排序值升序；排序值相同时按创建时间倒序
                .orderByAsc(Department::getSortOrder)
                .orderByDesc(Department::getCreatedAt);

        Page<Department> departmentPage = departmentService.page(
                new Page<>(query.getPageNum(), query.getPageSize()),
                wrapper
        );

        Page<DepartmentResponse> responsePage =
                (Page<DepartmentResponse>) departmentPage.convert(DepartmentConverter::toResponse);

        return Result.success(responsePage);
    }

    /**
     * 软删除租户
     * @param id
     * @return
     */
    @DeleteMapping("/{id}")
    public Result<Void> disable(@PathVariable Long id){
        boolean updated = departmentService.lambdaUpdate()
                .eq(Department::getId, id)
                .set(Department::getStatus, "DISABLED")
                .update();
        return Result.success();
    }
}
