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
import com.example.demo.security.TenantContext;
import com.example.demo.vo.DepartmentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/department")
@PreAuthorize("hasAuthority('user:manage')")
@RequiredArgsConstructor
/**
 * 当前租户的部门管理接口。
 *
 * <p>所有查询与修改均强制附加当前 JWT 中的 tenantId，避免按部门 ID 跨租户访问。</p>
 */
public class DepartmentController {
    private final DepartmentService departmentService;
    private final TenantContext tenantContext;

    /**
     * 新增一个默认启用的部门。
     *
     * @param request 客户端可填写的部门资料；tenantId 和状态由服务端决定。
     * @return 新建部门。
     */
    @PostMapping
    public Result<DepartmentResponse> save(@RequestBody DepartmentCreateRequest request) {
        Department department = DepartmentConverter.toEntity(request);
        department.setTenantId(tenantContext.tenantId());
        department.setStatus("ACTIVE");
        departmentService.save(department);
        return Result.success(DepartmentConverter.toResponse(department));
    }

    /**
     * 修改部门的可变资料。
     *
     * @param request 部门 ID 与允许修改的资料；编码和状态不在此接口修改。
     * @return 修改后的部门；部门不存在时返回业务码 20001。
     */
    @PutMapping
    public Result<DepartmentResponse> update(@RequestBody DepartmentUpdateRequest request){
        Department department = findCurrentTenantDepartment(request.getId());
        if (department == null) {
            return Result.failure(20001, "部门不存在");
        }
        DepartmentConverter.copyToEntity(request, department);
        departmentService.updateById(department);
        return Result.success(DepartmentConverter.toResponse(department));
    }

    /** @param id 部门 ID。@return 当前租户中的部门详情。 */
    @GetMapping("/{id}")
    public Result<DepartmentResponse> getOne(@PathVariable Long id){
        Department department = findCurrentTenantDepartment(id);
        if (department == null) {
            return Result.failure(20001, "部门不存在");
        }
        return Result.success(DepartmentConverter.toResponse(department));
    }

    /** @return 当前租户的全部部门。 */
    @GetMapping
    public Result<List<DepartmentResponse>> list() {
        List<DepartmentResponse> responseList = departmentService.lambdaQuery()
                .eq(Department::getTenantId, tenantContext.tenantId()).list().stream()
                .map(DepartmentConverter::toResponse)
                .toList();
        return Result.success(responseList);
    }


    /**
     * 按条件分页查询当前租户部门。
     *
     * @param query 父部门、编码、名称、状态和分页条件。
     * @return 部门分页。
     */
    @GetMapping("/page")
    public Result<Page<DepartmentResponse>> findPage(
            @ModelAttribute DepartmentPageQuery query) {

        LambdaQueryWrapper<Department> wrapper = Wrappers.lambdaQuery(Department.class)
                .eq(Department::getTenantId, tenantContext.tenantId())

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
     * 停用部门而非物理删除，保留历史工单中的部门引用。
     *
     * @param id 要停用的部门 ID。
     * @return 空的成功响应。
     */
    @DeleteMapping("/{id}")
    public Result<Void> disable(@PathVariable Long id){
        boolean updated = departmentService.lambdaUpdate()
                .eq(Department::getTenantId, tenantContext.tenantId())
                .eq(Department::getId, id)
                .set(Department::getStatus, "DISABLED")
                .update();
        return Result.success();
    }

    /** @param id 要重新启用的当前租户部门 ID。@return 空的成功响应。 */
    @PutMapping("/{id}/enable")
    public Result<Void> enable(@PathVariable Long id) {
        departmentService.lambdaUpdate()
                .eq(Department::getTenantId, tenantContext.tenantId())
                .eq(Department::getId, id)
                .set(Department::getStatus, "ACTIVE")
                .update();
        return Result.success();
    }

    /**
     * 读取当前租户范围内的一个部门。
     *
     * @param id 部门 ID。
     * @return 部门；不存在或不属于当前租户时返回 {@code null}。
     */
    private Department findCurrentTenantDepartment(Long id) {
        return departmentService.lambdaQuery()
                .eq(Department::getTenantId, tenantContext.tenantId())
                .eq(Department::getId, id)
                .one();
    }
}
