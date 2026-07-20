package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.example.demo.common.BusinessException;
import com.example.demo.dto.notification.NotificationPageQuery;
import com.example.demo.entity.Notification;
import com.example.demo.mapper.NotificationMapper;
import com.example.demo.security.TenantContext;
import com.example.demo.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
/**
 * 当前用户站内通知收件箱的查询和已读操作实现。
 */
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements NotificationService {
    private final TenantContext tenantContext;

    @Override
    /**
     * 分页查询当前登录用户收到的通知。
     *
     * @param query 分页及“只看未读”筛选条件。
     * @return 已按创建时间倒序排列的当前用户通知分页。
     */
    public Page<Notification> pageInbox(NotificationPageQuery query) {
        return page(new Page<>(query.getPageNum(), query.getPageSize()), Wrappers.lambdaQuery(Notification.class)
                .eq(Notification::getTenantId, tenantContext.tenantId())
                .eq(Notification::getRecipientId, tenantContext.userId())
                .isNull(Boolean.TRUE.equals(query.getUnreadOnly()), Notification::getReadAt)
                .orderByDesc(Notification::getCreatedAt));
    }

    @Override
    /**
     * 将当前用户的一条未读通知标记为已读。
     *
     * @param notificationId 通知 ID。
     * @throws BusinessException 当通知不存在或不属于当前用户时抛出。
     */
    public void markRead(Long notificationId) {
        int changed = baseMapper.update(null, Wrappers.lambdaUpdate(Notification.class)
                .eq(Notification::getId, notificationId)
                .eq(Notification::getTenantId, tenantContext.tenantId())
                .eq(Notification::getRecipientId, tenantContext.userId())
                .isNull(Notification::getReadAt)
                .set(Notification::getReadAt, LocalDateTime.now()));
        if (changed == 0 && !lambdaQuery()
                .eq(Notification::getId, notificationId)
                .eq(Notification::getTenantId, tenantContext.tenantId())
                .eq(Notification::getRecipientId, tenantContext.userId()).exists()) {
            throw new BusinessException(20001, "通知不存在");
        }
    }
}
