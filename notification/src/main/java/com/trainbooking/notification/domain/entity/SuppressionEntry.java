package com.trainbooking.notification.domain.entity;

import com.trainbooking.notification.domain.enums.Channel;
import com.trainbooking.notification.domain.enums.SuppressionReason;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(schema = "notification_service", name = "suppression_list")
@Getter
@Setter
public class SuppressionEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private Channel channel;

    @Column(name = "recipient", nullable = false, length = 320)
    private String recipient;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 30)
    private SuppressionReason reason;

    @Column(name = "source_provider", length = 50)
    private String sourceProvider;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "added_at", nullable = false, updatable = false)
    private Instant addedAt;

    @PrePersist
    protected void onCreate() {
        if (addedAt == null) {
            addedAt = Instant.now();
        }
    }
}
