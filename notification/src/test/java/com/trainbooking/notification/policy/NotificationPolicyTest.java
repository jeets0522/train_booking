package com.trainbooking.notification.policy;

import com.trainbooking.notification.domain.enums.Channel;
import com.trainbooking.notification.domain.enums.NotificationPriority;
import com.trainbooking.notification.kafka.NotificationCommand;
import com.trainbooking.notification.kafka.events.domain.EmailVerificationRequested;
import com.trainbooking.notification.kafka.events.domain.PhoneOtpRequested;
import com.trainbooking.notification.kafka.events.domain.UserRegistered;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationPolicyTest {

    private final NotificationPolicy policy = new NotificationPolicy();

    @Test
    void emailVerificationMapsToTransactionalEmail() {
        UUID eventId = UUID.randomUUID();
        Instant expiresAt = Instant.parse("2026-05-23T10:15:30Z");
        EmailVerificationRequested event = new EmailVerificationRequested(
                eventId, 42L, Instant.now(), "user@example.com", "Ada", "raw-token-123", expiresAt);

        List<NotificationCommand> commands = policy.apply(event);

        assertThat(commands).hasSize(1);
        NotificationCommand c = commands.get(0);
        assertThat(c.templateKey()).isEqualTo("EMAIL_VERIFICATION");
        assertThat(c.channel()).isEqualTo(Channel.EMAIL);
        assertThat(c.priority()).isEqualTo(NotificationPriority.TRANSACTIONAL);
        assertThat(c.recipient()).isEqualTo("user@example.com");
        assertThat(c.userId()).isEqualTo(42L);
        assertThat(c.variables())
                .containsEntry("otp", "raw-token-123")
                .containsEntry("firstName", "Ada")
                .containsEntry("expiresAt", expiresAt.toString());
        assertThat(c.id()).isEqualTo(expectedId(eventId, "EMAIL_VERIFICATION"));
    }

    @Test
    void userRegisteredMapsToBulkWelcomeEmail() {
        UUID eventId = UUID.randomUUID();
        UserRegistered event = new UserRegistered(
                eventId, 7L, Instant.now(), "new@example.com", "Grace");

        List<NotificationCommand> commands = policy.apply(event);

        assertThat(commands).hasSize(1);
        NotificationCommand c = commands.get(0);
        assertThat(c.templateKey()).isEqualTo("WELCOME_EMAIL");
        assertThat(c.channel()).isEqualTo(Channel.EMAIL);
        assertThat(c.priority()).isEqualTo(NotificationPriority.BULK);
        assertThat(c.recipient()).isEqualTo("new@example.com");
        assertThat(c.variables()).containsOnly(java.util.Map.entry("firstName", "Grace"));
        assertThat(c.id()).isEqualTo(expectedId(eventId, "WELCOME_EMAIL"));
    }

    @Test
    void phoneOtpMapsToTransactionalSms() {
        UUID eventId = UUID.randomUUID();
        PhoneOtpRequested event = new PhoneOtpRequested(
                eventId, 9L, Instant.now(), "+15551234567", "123456", Instant.parse("2026-05-23T10:25:30Z"));

        List<NotificationCommand> commands = policy.apply(event);

        assertThat(commands).hasSize(1);
        NotificationCommand c = commands.get(0);
        assertThat(c.channel()).isEqualTo(Channel.SMS);
        assertThat(c.priority()).isEqualTo(NotificationPriority.TRANSACTIONAL);
        assertThat(c.recipient()).isEqualTo("+15551234567");
        assertThat(c.variables()).containsEntry("otp", "123456");
    }

    @Test
    void idIsDeterministicForSameEventAndTemplate() {
        UUID eventId = UUID.randomUUID();
        UserRegistered event = new UserRegistered(eventId, 1L, Instant.now(), "a@b.com", "A");

        UUID first = policy.apply(event).get(0).id();
        UUID second = policy.apply(event).get(0).id();

        assertThat(first).isEqualTo(second);
    }

    @Test
    void differentTemplatesFromSameEventIdYieldDifferentIds() {
        UUID eventId = UUID.randomUUID();
        assertThat(expectedId(eventId, "EMAIL_VERIFICATION"))
                .isNotEqualTo(expectedId(eventId, "WELCOME_EMAIL"));
    }

    private static UUID expectedId(UUID eventId, String templateKey) {
        return UUID.nameUUIDFromBytes((eventId + ":" + templateKey).getBytes(StandardCharsets.UTF_8));
    }
}
