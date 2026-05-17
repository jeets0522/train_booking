package com.trainbooking.notification.repository;

import com.trainbooking.notification.domain.entity.Notification;
import com.trainbooking.notification.domain.enums.NotificationStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Optional<Notification> findByProviderUsedAndProviderMessageId(String providerUsed, String providerMessageId);

    @Query("""
        SELECT n FROM Notification n
        WHERE n.status IN :statuses
          AND n.updatedAt < :olderThan
          AND n.attempts < :maxAttempts
        ORDER BY n.updatedAt ASC
        """)
    List<Notification> findStuckNotifications(
            @Param("statuses") List<NotificationStatus> statuses,
            @Param("olderThan") Instant olderThan,
            @Param("maxAttempts") int maxAttempts,
            Pageable pageable);
}
