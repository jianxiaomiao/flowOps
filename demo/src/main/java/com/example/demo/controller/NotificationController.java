package com.example.demo.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.Result;
import com.example.demo.dto.notification.NotificationPageQuery;
import com.example.demo.entity.Notification;
import com.example.demo.service.NotificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
/**
 * 当前用户的站内通知收件箱接口。
 */
public class NotificationController {
    private final NotificationService notificationService;

    /** @param query 分页和已读状态筛选条件。@return 当前用户的通知分页。 */
    @GetMapping("/page")
    public Result<Page<Notification>> page(@Valid @ModelAttribute NotificationPageQuery query) {
        return Result.success(notificationService.pageInbox(query));
    }

    /** @param id 要标记为已读的通知 ID。@return 空的成功响应。 */
    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable @Positive Long id) {
        notificationService.markRead(id);
        return Result.success();
    }
}
