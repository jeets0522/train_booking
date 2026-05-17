package com.trainbooking.notification.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trainbooking.notification.dto.response.ApiResponse;
import com.trainbooking.notification.service.DeliveryEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * Webhook receivers for provider delivery/bounce/complaint events.
 * <p>
 * NOTE: For production, add signature verification:
 *   - SES via SNS: verify the SNS message signature against the cert URL
 *   - SendGrid: verify the X-Twilio-Email-Event-Webhook-Signature header (ed25519)
 * Left out of v1 intentionally; track separately before exposing this endpoint publicly.
 */
@RestController
@RequestMapping("/webhooks")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final DeliveryEventService deliveryEventService;
    private final ObjectMapper objectMapper;

    public WebhookController(DeliveryEventService deliveryEventService, ObjectMapper objectMapper) {
        this.deliveryEventService = deliveryEventService;
        this.objectMapper = objectMapper;
    }

    /**
     * SES → SNS → here. SNS wraps the SES notification in a Message field.
     * The Message field is a JSON string with notificationType + mail.messageId + bounce/complaint details.
     */
    @PostMapping("/ses")
    public ResponseEntity<ApiResponse> ses(@RequestBody String rawBody) {
        try {
            JsonNode root = objectMapper.readTree(rawBody);
            String snsType = text(root, "Type");

            if ("SubscriptionConfirmation".equalsIgnoreCase(snsType)) {
                log.warn("SES SNS SubscriptionConfirmation received; confirm manually by GETting SubscribeURL: {}",
                        text(root, "SubscribeURL"));
                return ResponseEntity.ok(ApiResponse.success("Subscription confirmation logged"));
            }

            JsonNode message = root.path("Message").isMissingNode() || root.path("Message").isNull()
                    ? root  // direct webhook without SNS envelope
                    : objectMapper.readTree(root.get("Message").asText());

            String notificationType = text(message, "notificationType");
            String providerMessageId = text(message.path("mail"), "messageId");
            String recipient = extractFirstSesRecipient(message, notificationType);

            deliveryEventService.record("ses", notificationType, providerMessageId, rawBody, recipient);
            return ResponseEntity.ok(ApiResponse.success("SES event recorded"));
        } catch (IOException e) {
            log.error("Failed to parse SES webhook payload", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid SES payload"));
        }
    }

    /**
     * SendGrid posts an array of event objects to a single webhook URL.
     */
    @PostMapping("/sendgrid")
    public ResponseEntity<ApiResponse> sendgrid(@RequestBody String rawBody) {
        try {
            JsonNode root = objectMapper.readTree(rawBody);
            if (!root.isArray()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Expected JSON array"));
            }
            for (JsonNode event : root) {
                String eventType = text(event, "event");
                String providerMessageId = text(event, "sg_message_id");
                String recipient = text(event, "email");
                deliveryEventService.record("sendgrid", eventType, providerMessageId,
                        event.toString(), recipient);
            }
            return ResponseEntity.ok(ApiResponse.success("SendGrid events recorded"));
        } catch (IOException e) {
            log.error("Failed to parse SendGrid webhook payload", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid SendGrid payload"));
        }
    }

    private String extractFirstSesRecipient(JsonNode message, String notificationType) {
        if (notificationType == null) return null;
        String type = notificationType.toLowerCase();
        JsonNode list;
        if (type.contains("bounce")) {
            list = message.path("bounce").path("bouncedRecipients");
        } else if (type.contains("complaint")) {
            list = message.path("complaint").path("complainedRecipients");
        } else if (type.contains("delivery")) {
            list = message.path("delivery").path("recipients");
            if (list.isArray() && list.size() > 0) {
                return list.get(0).asText();
            }
            return null;
        } else {
            return null;
        }
        if (list.isArray() && list.size() > 0) {
            return text(list.get(0), "emailAddress");
        }
        return null;
    }

    private static String text(JsonNode node, String field) {
        JsonNode child = node.path(field);
        return child.isMissingNode() || child.isNull() ? null : child.asText();
    }
}
