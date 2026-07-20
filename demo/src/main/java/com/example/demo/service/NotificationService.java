package com.example.demo.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.example.demo.dto.notification.NotificationPageQuery;
import com.example.demo.entity.Notification;

public interface NotificationService extends IService<Notification> {
    Page<Notification> pageInbox(NotificationPageQuery query);
    long unreadCount();
    void markRead(Long notificationId);
}
