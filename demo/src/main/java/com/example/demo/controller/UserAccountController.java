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
import com.example.demo.vo.UserAccountResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

/** 用户账号接口。 */
@RestController
@RequestMapping("/user-account")
@RequiredArgsConstructor
public class UserAccountController {

    private final UserAccountService userAccountService;

    @PostMapping
    public Result<UserAccountResponse> create(@Valid @RequestBody UserAccountCreateRequest request) {
        UserAccount userAccount = userAccountService.createUser(request);
        return Result.success(UserAccountConverter.toResponse(userAccount));
    }

    @PutMapping
    public Result<UserAccountResponse> update(
            @RequestParam Long tenantId,
            @Valid @RequestBody UserAccountUpdateRequest request
    ) {
        UserAccount userAccount = userAccountService.updateUser(tenantId, request);
        return Result.success(UserAccountConverter.toResponse(userAccount));
    }

    @GetMapping("/{id}")
    public Result<UserAccountResponse> getOne(
            @PathVariable Long id,
            @RequestParam Long tenantId
    ) {
        UserAccount userAccount = userAccountService.getUser(tenantId, id);
        return Result.success(UserAccountConverter.toResponse(userAccount));
    }

    @GetMapping("/page")
    public Result<IPage<UserAccountResponse>> findPage(
            @Valid @ModelAttribute UserAccountPageQuery query
    ) {
        Page<UserAccount> userPage = userAccountService.pageUsers(query);
        return Result.success(userPage.convert(UserAccountConverter::toResponse));
    }

    @DeleteMapping("/{id}")
    public Result<Void> disable(
            @PathVariable Long id,
            @RequestParam Long tenantId
    ) {
        userAccountService.disableUser(tenantId, id);
        return Result.success();
    }
}
