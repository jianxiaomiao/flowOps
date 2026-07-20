package com.example.demo.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.example.demo.dto.user.UserAccountCreateRequest;
import com.example.demo.dto.user.UserAccountPageQuery;
import com.example.demo.dto.user.UserAccountUpdateRequest;
import com.example.demo.entity.UserAccount;

/** 用户账号用例服务。 */
public interface UserAccountService extends IService<UserAccount> {

    UserAccount createUser(UserAccountCreateRequest request);

    UserAccount updateUser(Long tenantId, UserAccountUpdateRequest request);

    UserAccount getUser(Long tenantId, Long userId);

    Page<UserAccount> pageUsers(UserAccountPageQuery query);

    void disableUser(Long tenantId, Long userId);

    void enableUser(Long tenantId, Long userId);
}
