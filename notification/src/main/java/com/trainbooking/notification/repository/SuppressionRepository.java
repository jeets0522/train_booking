package com.trainbooking.notification.repository;

import com.trainbooking.notification.domain.entity.SuppressionEntry;
import com.trainbooking.notification.domain.enums.Channel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SuppressionRepository extends JpaRepository<SuppressionEntry, Long> {

    boolean existsByChannelAndRecipient(Channel channel, String recipient);

    Optional<SuppressionEntry> findByChannelAndRecipient(Channel channel, String recipient);
}
