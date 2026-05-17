package com.trainbooking.notification.service;

import com.trainbooking.notification.domain.entity.Notification;
import com.trainbooking.notification.domain.enums.NotificationStatus;
import com.trainbooking.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class RetryWorker {

    private static final Logger log = LoggerFactory.getLogger(RetryWorker.class);
    private static final int BATCH_SIZE = 50;

    private final NotificationRepository notificationRepo;
    private final NotificationService notificationService;

    @Value("${notification.retry.max-attempts:5}")
    private int maxAttempts;

    @Value("${notification.retry.stuck-after-seconds:120}")
    private int stuckAfterSeconds;

    public RetryWorker(
            NotificationRepository notificationRepo,
            NotificationService notificationService) {
        this.notificationRepo = notificationRepo;
        this.notificationService = notificationService;
    }

    @Scheduled(fixedDelayString = "${notification.retry.scan-interval-ms:30000}")
    public void retryStuck() {
        Instant cutoff = Instant.now().minusSeconds(stuckAfterSeconds);
        List<Notification> stuck = notificationRepo.findStuckNotifications(
                List.of(NotificationStatus.PENDING, NotificationStatus.FAILED),
                cutoff,
                maxAttempts,
                PageRequest.of(0, BATCH_SIZE));
        if (stuck.isEmpty()) {
            return;
        }
        log.info("Retrying {} stuck notifications", stuck.size());
        for (Notification n : stuck) {
            try {
                notificationService.retry(n);
            } catch (RuntimeException e) {
                log.error("Retry failed for notification {}: {}", n.getId(), e.getMessage(), e);
            }
        }
    }
}
