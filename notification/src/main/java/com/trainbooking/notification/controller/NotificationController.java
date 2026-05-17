package com.trainbooking.notification.controller;

import com.trainbooking.notification.dto.response.NotificationResponse;
import com.trainbooking.notification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationResponse> getStatus(@PathVariable UUID id) {
        return ResponseEntity.ok(NotificationResponse.from(notificationService.getById(id)));
    }
}
