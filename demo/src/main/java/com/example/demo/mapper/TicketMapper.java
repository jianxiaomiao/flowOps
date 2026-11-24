package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.entity.Ticket;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TicketMapper extends BaseMapper<Ticket> {
    @Select("SELECT EXISTS(SELECT 1 FROM external_ticket_link WHERE tenant_id = #{tenant} AND ticket_id = #{ticket})")
    boolean isExternal(@Param("tenant") long tenantId, @Param("ticket") long ticketId);

    @Select("""
            SELECT COUNT(DISTINCT CASE WHEN p.permission_code IN ('ticket:read', 'ticket:read:department')
                    THEN 'read' ELSE 'claim' END) = 2
            FROM user_role ur
            JOIN role r ON r.id = ur.role_id AND r.tenant_id = ur.tenant_id AND r.status = 'ACTIVE'
            JOIN role_permission rp ON rp.role_id = r.id AND rp.tenant_id = r.tenant_id
            JOIN permission p ON p.id = rp.permission_id AND p.status = 'ACTIVE'
            WHERE ur.tenant_id = #{tenant} AND ur.user_id = #{user}
              AND p.permission_code IN ('ticket:claim', 'ticket:read', 'ticket:read:department')
            """)
    boolean canClaim(@Param("tenant") long tenantId, @Param("user") long userId);
}
