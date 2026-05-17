package com.trainbooking.notification.repository;

import com.trainbooking.notification.domain.entity.DeliveryEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliveryEventRepository extends JpaRepository<DeliveryEvent, Long> {
}
