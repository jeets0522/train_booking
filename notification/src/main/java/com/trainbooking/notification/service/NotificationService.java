package com.trainbooking.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trainbooking.notification.channel.ChannelDispatcher;
import com.trainbooking.notification.channel.DispatchResult;
import com.trainbooking.notification.channel.NotificationRequest;
import com.trainbooking.notification.domain.entity.Notification;
import com.trainbooking.notification.domain.enums.NotificationStatus;
import com.trainbooking.notification.exception.NotificationException;
import com.trainbooking.notification.repository.NotificationRepository;
import com.trainbooking.notification.template.TemplateCatalog;
import com.trainbooking.notification.template.TemplateDescriptor;
import com.trainbooking.notification.template.VariableValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepo;
    private final SuppressionService suppressionService;
    private final TemplateCatalog templateCatalog;
    private final ChannelDispatcher dispatcher;
    private final ObjectMapper objectMapper;

    @Value("${notification.retry.max-attempts:5}")
    private int maxAttempts;

    public NotificationService(
            NotificationRepository notificationRepo,
            SuppressionService suppressionService,
            TemplateCatalog templateCatalog,
            ChannelDispatcher dispatcher,
            ObjectMapper objectMapper) {
        this.notificationRepo = notificationRepo;
        this.suppressionService = suppressionService;
        this.templateCatalog = templateCatalog;
        this.dispatcher = dispatcher;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public DispatchResult send(NotificationRequest request) {
        Notification existing = notificationRepo.findById(request.id()).orElse(null);

        if (existing != null
                && (existing.getStatus() == NotificationStatus.SENT
                    || existing.getStatus() == NotificationStatus.DELIVERED)) {
            log.info("Notification {} already in status {}; skipping (idempotent)",
                    request.id(), existing.getStatus());
            return new DispatchResult(
                    existing.getId(),
                    existing.getStatus(),
                    existing.getProviderUsed() != null
                            ? com.trainbooking.notification.domain.enums.ProviderName
                                .fromWireName(existing.getProviderUsed())
                            : null,
                    existing.getProviderMessageId(),
                    null);
        }

        Notification notification = (existing != null)
                ? existing
                : newNotificationFrom(request);

        if (suppressionService.isSuppressed(request.channel(), request.recipient())) {
            notification.setStatus(NotificationStatus.SUPPRESSED);
            notificationRepo.save(notification);
            log.info("Notification {} suppressed: {} on {} is in suppression list",
                    request.id(), request.recipient(), request.channel());
            return DispatchResult.suppressed(request.id());
        }

        TemplateDescriptor descriptor;
        try {
            descriptor = templateCatalog.lookup(request.templateKey());
            VariableValidator.validate(descriptor, request.variables());
        } catch (RuntimeException e) {
            return persistFailure(notification, "Template error: " + e.getMessage());
        }
        if (descriptor.channel() != request.channel()) {
            return persistFailure(notification,
                    "Template '" + request.templateKey() + "' is for " + descriptor.channel()
                            + " but request was for " + request.channel());
        }

        notification.setAttempts(notification.getAttempts() + 1);
        notificationRepo.save(notification);

        DispatchResult result;
        try {
            result = dispatcher.dispatch(request, descriptor);
        } catch (Throwable t) {
            log.error("Unhandled dispatch exception for {}", request.id(), t);
            return persistFailure(notification, "Dispatch exception: " + t.getMessage());
        }

        applyDispatchResult(notification, result);
        notificationRepo.save(notification);
        return result;
    }

    public Notification getById(UUID id) {
        return notificationRepo.findById(id)
                .orElseThrow(() -> new NotificationException.NotFoundException(
                        "Notification not found: " + id));
    }

    @Transactional
    public DispatchResult retry(Notification notification) {
        if (notification.getAttempts() >= maxAttempts) {
            log.warn("Notification {} exceeded max attempts ({}); not retrying",
                    notification.getId(), maxAttempts);
            return DispatchResult.failed(notification.getId(),
                    "Max attempts (" + maxAttempts + ") reached");
        }

        Map<String, Object> vars;
        try {
            vars = notification.getTemplateVariables() == null
                    ? Map.of()
                    : objectMapper.readValue(
                            notification.getTemplateVariables(),
                            new TypeReference<Map<String, Object>>() {});
        } catch (IOException e) {
            throw new NotificationException("Failed to deserialize variables for retry", e);
        }

        NotificationRequest request = new NotificationRequest(
                notification.getId(),
                notification.getChannel(),
                notification.getRecipient(),
                notification.getTemplateKey(),
                vars,
                notification.getUserId());
        return send(request);
    }

    private Notification newNotificationFrom(NotificationRequest request) {
        Notification n = new Notification();
        n.setId(request.id());
        n.setChannel(request.channel());
        n.setRecipient(request.recipient());
        n.setTemplateKey(request.templateKey());
        n.setUserId(request.userId());
        n.setStatus(NotificationStatus.PENDING);
        try {
            n.setTemplateVariables(
                    request.variables() == null
                            ? null
                            : objectMapper.writeValueAsString(request.variables()));
        } catch (JsonProcessingException e) {
            throw new NotificationException("Failed to serialize template variables", e);
        }
        return notificationRepo.save(n);
    }

    private DispatchResult persistFailure(Notification n, String error) {
        n.setStatus(NotificationStatus.FAILED);
        n.setLastError(error);
        n.setFailedAt(Instant.now());
        notificationRepo.save(n);
        return DispatchResult.failed(n.getId(), error);
    }

    private void applyDispatchResult(Notification n, DispatchResult result) {
        n.setStatus(result.status());
        if (result.providerUsed() != null) {
            n.setProviderUsed(result.providerUsed().wireName());
        }
        if (result.providerMessageId() != null) {
            n.setProviderMessageId(result.providerMessageId());
        }
        if (result.errorMessage() != null) {
            n.setLastError(result.errorMessage());
        }
        Instant now = Instant.now();
        if (result.status() == NotificationStatus.SENT) {
            n.setSentAt(now);
        } else if (result.status() == NotificationStatus.FAILED) {
            n.setFailedAt(now);
        }
    }
}
