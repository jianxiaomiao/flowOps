package com.example.demo.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.example.demo.entity.UserRole;
import java.util.List;

public interface UserRoleService extends IService<UserRole> {
    UserRole grant(Long tenantId, Long userId, Long roleId, Long operatorId);
    void revoke(Long tenantId, Long userId, Long roleId);
    List<UserRole> listByUser(Long tenantId, Long userId);
}
