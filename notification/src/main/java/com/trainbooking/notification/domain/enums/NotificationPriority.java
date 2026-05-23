package com.trainbooking.notification.domain.enums;

/**
 * Delivery priority. Determines which channel/priority topic a {@code NotificationCommand} is
 * routed to, so latency-sensitive sends (OTPs, verification) are isolated from bulk traffic.
 */
public enum NotificationPriority {
    TRANSACTIONAL,
    BULK
}
