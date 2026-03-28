package com.vicky.smartpay.notification.controller;

import com.vicky.smartpay.notification.model.Notification;
import com.vicky.smartpay.notification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<Notification>> getNotifications(
            @PathVariable Long userId) {
        return ResponseEntity.ok(notificationService.getNotifications(userId));
    }
}