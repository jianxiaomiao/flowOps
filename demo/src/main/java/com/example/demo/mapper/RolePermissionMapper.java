package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.entity.RolePermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface RolePermissionMapper extends BaseMapper<RolePermission> {
    @Select("""
            SELECT DISTINCT p.permission_code
            FROM user_role ur
            JOIN role r ON r.id = ur.role_id AND r.tenant_id = ur.tenant_id AND r.status = 'ACTIVE'
            JOIN role_permission rp ON rp.role_id = r.id AND rp.tenant_id = r.tenant_id
            JOIN permission p ON p.id = rp.permission_id AND p.status = 'ACTIVE'
            WHERE ur.tenant_id = #{tenantId} AND ur.user_id = #{userId}
            ORDER BY p.permission_code
            """)
    List<String> selectActivePermissionCodes(@Param("tenantId") Long tenantId,
                                             @Param("userId") Long userId);
}
