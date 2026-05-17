package com.trainbooking.notification.service;

import com.trainbooking.notification.domain.entity.DeliveryEvent;
import com.trainbooking.notification.domain.entity.Notification;
import com.trainbooking.notification.domain.enums.NotificationStatus;
import com.trainbooking.notification.domain.enums.SuppressionReason;
import com.trainbooking.notification.repository.DeliveryEventRepository;
import com.trainbooking.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class DeliveryEventService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryEventService.class);

    private final DeliveryEventRepository repo;
    private final NotificationRepository notificationRepo;
    private final SuppressionService suppressionService;

    public DeliveryEventService(
            DeliveryEventRepository repo,
            NotificationRepository notificationRepo,
            SuppressionService suppressionService) {
        this.repo = repo;
        this.notificationRepo = notificationRepo;
        this.suppressionService = suppressionService;
    }

    @Transactional
    public DeliveryEvent record(
            String provider,
            String eventType,
            String providerMessageId,
            String rawPayload,
            String recipient) {

        DeliveryEvent newEvent = new DeliveryEvent();
        newEvent.setProvider(provider);
        newEvent.setEventType(eventType);
        newEvent.setProviderMessageId(providerMessageId);
        newEvent.setRawPayload(rawPayload);
        final DeliveryEvent event = repo.save(newEvent);

        try {
            if (providerMessageId != null) {
                notificationRepo
                        .findByProviderUsedAndProviderMessageId(provider, providerMessageId)
                        .ifPresent(n -> {
                            event.setNotificationId(n.getId());
                            applyEventToNotification(n, eventType, recipient);
                        });
            }
            event.setProcessedAt(Instant.now());
        } catch (RuntimeException e) {
            log.error("Failed to process delivery event {}: {}", event.getId(), e.getMessage(), e);
            event.setProcessingError(e.getMessage());
        }
        return repo.save(event);
    }

    private void applyEventToNotification(Notification n, String eventType, String recipient) {
        String type = eventType == null ? "" : eventType.toUpperCase();
        Instant now = Instant.now();
        String targetRecipient = recipient != null ? recipient : n.getRecipient();

        if (type.contains("DELIVER")) {
            n.setStatus(NotificationStatus.DELIVERED);
            n.setDeliveredAt(now);
        } else if (type.contains("BOUNCE")) {
            n.setStatus(NotificationStatus.BOUNCED);
            n.setFailedAt(now);
            suppressionService.add(n.getChannel(), targetRecipient,
                    SuppressionReason.HARD_BOUNCE, n.getProviderUsed(), eventType);
        } else if (type.contains("COMPLAIN") || type.contains("SPAMREPORT")) {
            suppressionService.add(n.getChannel(), targetRecipient,
                    SuppressionReason.COMPLAINT, n.getProviderUsed(), eventType);
        } else if (type.contains("DROPPED") || type.contains("REJECT")) {
            n.setStatus(NotificationStatus.FAILED);
            n.setFailedAt(now);
            n.setLastError("Provider event: " + eventType);
        } else {
            log.debug("Delivery event '{}' for {} is informational; no status change",
                    eventType, n.getId());
        }
        notificationRepo.save(n);
    }
}
