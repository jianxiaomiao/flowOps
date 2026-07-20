package com.example.demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.Result;
import com.example.demo.converter.UserAccountConverter;
import com.example.demo.dto.user.UserAccountCreateRequest;
import com.example.demo.dto.user.UserAccountPageQuery;
import com.example.demo.dto.user.UserAccountUpdateRequest;
import com.example.demo.entity.UserAccount;
import com.example.demo.service.UserAccountService;
import com.example.demo.security.TenantContext;
import com.example.demo.vo.UserAccountResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 当前租户的用户账号管理接口。
 */
@RestController
@RequestMapping("/user-account")
@PreAuthorize("hasAuthority('user:manage')")
@RequiredArgsConstructor
public class UserAccountController {

    private final UserAccountService userAccountService;
    private final TenantContext tenantContext;

    /** @param request 用户基础资料和初始明文密码。@return 新建账号；响应不会含密码摘要。 */
    @PostMapping
    @PreAuthorize("hasAuthority('user:manage')")
    public Result<UserAccountResponse> create(@Valid @RequestBody UserAccountCreateRequest request) {
        UserAccount userAccount = userAccountService.createUser(request);
        return Result.success(UserAccountConverter.toResponse(userAccount));
    }

    /** @param request 可修改的用户资料和版本。@return 修改后的用户。 */
    @PutMapping
    public Result<UserAccountResponse> update(
            @Valid @RequestBody UserAccountUpdateRequest request
    ) {
        UserAccount userAccount = userAccountService.updateUser(tenantContext.tenantId(), request);
        return Result.success(UserAccountConverter.toResponse(userAccount));
    }

    /** @param id 用户 ID。@return 当前租户中的用户详情。 */
    @GetMapping("/{id}")
    public Result<UserAccountResponse> getOne(
            @PathVariable Long id
    ) {
        UserAccount userAccount = userAccountService.getUser(tenantContext.tenantId(), id);
        return Result.success(UserAccountConverter.toResponse(userAccount));
    }

    /** @param query 分页及用户名、部门、状态筛选条件。@return 用户分页。 */
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('user:manage')")
    public Result<IPage<UserAccountResponse>> findPage(
            @Valid @ModelAttribute UserAccountPageQuery query
    ) {
        Page<UserAccount> userPage = userAccountService.pageUsers(query);
        return Result.success(userPage.convert(UserAccountConverter::toResponse));
    }

    /** @param id 要停用的用户 ID。@return 空的成功响应。 */
    @DeleteMapping("/{id}")
    public Result<Void> disable(
            @PathVariable Long id
    ) {
        userAccountService.disableUser(tenantContext.tenantId(), id);
        return Result.success();
    }
}
