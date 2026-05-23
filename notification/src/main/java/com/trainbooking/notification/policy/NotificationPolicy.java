package com.trainbooking.notification.policy;

import com.trainbooking.notification.domain.enums.Channel;
import com.trainbooking.notification.domain.enums.NotificationPriority;
import com.trainbooking.notification.kafka.NotificationCommand;
import com.trainbooking.notification.kafka.events.domain.DomainEvent;
import com.trainbooking.notification.kafka.events.domain.EmailVerificationRequested;
import com.trainbooking.notification.kafka.events.domain.PhoneOtpRequested;
import com.trainbooking.notification.kafka.events.domain.UserRegistered;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The single place that decides which notification(s) a domain event produces. Maps each
 * {@link DomainEvent} to zero or more {@link NotificationCommand}s (template, channel, priority,
 * recipient, variables). Adding a new notification for an existing event is a one-line change here
 * plus a template-catalog entry — no new topic, listener, or event type.
 */
@Component
public class NotificationPolicy {

    public List<NotificationCommand> apply(DomainEvent event) {
        return switch (event) {
            case EmailVerificationRequested e -> {
                Map<String, Object> vars = new HashMap<>();
                vars.put("otp", e.rawToken());
                vars.put("firstName", e.firstName());
                vars.put("expiresAt", e.expiresAt() == null ? null : e.expiresAt().toString());
                yield List.of(command(
                        e, "EMAIL_VERIFICATION", Channel.EMAIL, NotificationPriority.TRANSACTIONAL,
                        e.email(), vars));
            }
            case UserRegistered e -> {
                Map<String, Object> vars = new HashMap<>();
                vars.put("firstName", e.firstName());
                yield List.of(command(
                        e, "WELCOME_EMAIL", Channel.EMAIL, NotificationPriority.BULK,
                        e.email(), vars));
            }
            case PhoneOtpRequested e -> {
                Map<String, Object> vars = new HashMap<>();
                vars.put("otp", e.otp());
                vars.put("expiresAt", e.expiresAt() == null ? null : e.expiresAt().toString());
                yield List.of(command(
                        e, "PHONE_OTP_GLOBAL", Channel.SMS, NotificationPriority.TRANSACTIONAL,
                        e.phoneNumber(), vars));
            }
        };
    }

    private static NotificationCommand command(
            DomainEvent event, String templateKey, Channel channel,
            NotificationPriority priority, String recipient, Map<String, Object> variables) {
        return new NotificationCommand(
                deterministicId(event.eventId(), templateKey),
                channel, priority, recipient, templateKey, variables, event.userId());
    }

    /**
     * Stable id from (eventId, templateKey) so one event fanning out to N notifications yields N
     * stable ids — redelivery of the event maps to the same notification rows, preserving
     * {@code NotificationService.send} idempotency.
     */
    static UUID deterministicId(UUID eventId, String templateKey) {
        String seed = eventId + ":" + templateKey;
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
    }
}
