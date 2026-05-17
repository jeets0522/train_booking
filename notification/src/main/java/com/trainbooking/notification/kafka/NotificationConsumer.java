package com.trainbooking.notification.kafka;

import com.trainbooking.notification.channel.NotificationRequest;
import com.trainbooking.notification.domain.enums.Channel;
import com.trainbooking.notification.kafka.events.EmailVerificationEvent;
import com.trainbooking.notification.kafka.events.WelcomeEmailEvent;
import com.trainbooking.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    private final NotificationService notificationService;

    public NotificationConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(
            topics = "${kafka.topics.email-verification}",
            groupId = "${spring.kafka.consumer.group-id}",
            properties = {"spring.json.value.default.type=com.trainbooking.notification.kafka.events.EmailVerificationEvent"})
    public void onEmailVerification(EmailVerificationEvent event) {
        log.info("Received EmailVerificationEvent userId={} email={}", event.getUserId(), event.getEmail());

        Map<String, Object> vars = new HashMap<>();
        vars.put("otp", event.getRawToken());
        vars.put("firstName", event.getFirstName());
        vars.put("expiresAt", event.getExpiresAt() == null ? null : event.getExpiresAt().toString());

        NotificationRequest request = new NotificationRequest(
                deterministicId("EMAIL_VERIFICATION", event.getUserId(), event.getRawToken()),
                Channel.EMAIL,
                event.getEmail(),
                "EMAIL_VERIFICATION",
                vars,
                event.getUserId());
        notificationService.send(request);
    }

    @KafkaListener(
            topics = "${kafka.topics.welcome-email}",
            groupId = "${spring.kafka.consumer.group-id}",
            properties = {"spring.json.value.default.type=com.trainbooking.notification.kafka.events.WelcomeEmailEvent"})
    public void onWelcomeEmail(WelcomeEmailEvent event) {
        log.info("Received WelcomeEmailEvent userId={} email={}", event.getUserId(), event.getEmail());

        Map<String, Object> vars = new HashMap<>();
        vars.put("firstName", event.getFirstName());

        NotificationRequest request = new NotificationRequest(
                deterministicId("WELCOME", event.getUserId(), null),
                Channel.EMAIL,
                event.getEmail(),
                "WELCOME_EMAIL",
                vars,
                event.getUserId());
        notificationService.send(request);
    }

    private static UUID deterministicId(String prefix, Long userId, String discriminator) {
        StringBuilder sb = new StringBuilder(prefix).append(':').append(userId);
        if (discriminator != null) {
            sb.append(':').append(discriminator);
        }
        return UUID.nameUUIDFromBytes(sb.toString().getBytes(StandardCharsets.UTF_8));
    }
}
