package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.entity.UserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {
    @Select("SELECT user_id FROM user_role WHERE tenant_id = #{tenantId} AND role_id = #{roleId}")
    List<Long> selectUserIdsByRole(@Param("tenantId") Long tenantId, @Param("roleId") Long roleId);
}
