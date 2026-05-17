package com.trainbooking.notification.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "notification_service", name = "delivery_events")
@Getter
@Setter
public class DeliveryEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "provider_message_id", length = 255)
    private String providerMessageId;

    @Column(name = "notification_id")
    private UUID notificationId;

    @Column(name = "raw_payload", columnDefinition = "jsonb", nullable = false)
    private String rawPayload;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "processing_error", columnDefinition = "text")
    private String processingError;

    @PrePersist
    protected void onCreate() {
        if (receivedAt == null) {
            receivedAt = Instant.now();
        }
    }
}
