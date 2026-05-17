package com.trainbooking.notification.channel.email;

import com.trainbooking.notification.channel.DispatchResult;
import com.trainbooking.notification.channel.NotificationChannel;
import com.trainbooking.notification.channel.NotificationRequest;
import com.trainbooking.notification.domain.enums.Channel;
import com.trainbooking.notification.domain.enums.ProviderName;
import com.trainbooking.notification.provider.ProviderResult;
import com.trainbooking.notification.provider.email.EmailProvider;
import com.trainbooking.notification.provider.email.EmailRecipient;
import com.trainbooking.notification.provider.email.TemplatedEmail;
import com.trainbooking.notification.template.ProviderTemplateRef;
import com.trainbooking.notification.template.TemplateDescriptor;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class EmailChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(EmailChannel.class);

    private final Map<ProviderName, EmailProvider> providers;
    private final List<ProviderName> orderedProviders;
    private final CircuitBreakerRegistry breakerRegistry;
    private final EmailRecipient defaultFrom;

    public EmailChannel(
            List<EmailProvider> emailProviders,
            CircuitBreakerRegistry breakerRegistry,
            @Value("${notification.channels.email.primary-provider}") String primary,
            @Value("${notification.channels.email.fallback-providers:}") List<String> fallbacks,
            @Value("${notification.from.email}") String fromEmail,
            @Value("${notification.from.email-name:}") String fromName) {
        EnumMap<ProviderName, EmailProvider> map = new EnumMap<>(ProviderName.class);
        for (EmailProvider p : emailProviders) {
            map.put(p.name(), p);
        }
        this.providers = Map.copyOf(map);

        List<ProviderName> order = new ArrayList<>();
        order.add(ProviderName.fromWireName(primary));
        if (fallbacks != null) {
            for (String f : fallbacks) {
                if (f == null || f.isBlank()) continue;
                ProviderName name = ProviderName.fromWireName(f.trim());
                if (!order.contains(name)) {
                    order.add(name);
                }
            }
        }
        this.orderedProviders = List.copyOf(order);

        this.breakerRegistry = breakerRegistry;
        this.defaultFrom = new EmailRecipient(fromEmail, fromName == null || fromName.isBlank() ? null : fromName);

        log.info("EmailChannel ready: providers={}, order={}, registeredBeans={}",
                providers.keySet(), orderedProviders,
                emailProviders.stream().map(EmailProvider::name).toList());
    }

    @Override
    public Channel channel() {
        return Channel.EMAIL;
    }

    @Override
    public DispatchResult dispatch(NotificationRequest request, TemplateDescriptor descriptor) {
        String lastError = null;

        for (ProviderName providerName : orderedProviders) {
            EmailProvider provider = providers.get(providerName);
            if (provider == null) {
                log.debug("Email provider {} not available (no bean); skipping", providerName);
                lastError = "Provider " + providerName + " unavailable";
                continue;
            }
            ProviderTemplateRef ref = descriptor.refFor(providerName);
            if (ref == null) {
                log.warn("Template '{}' has no mapping for provider {}; skipping",
                        descriptor.key(), providerName);
                lastError = "Template '" + descriptor.key() + "' missing " + providerName + " mapping";
                continue;
            }

            CircuitBreaker breaker = breakerRegistry.circuitBreaker(
                    "email-" + providerName.wireName());
            if (!breaker.tryAcquirePermission()) {
                log.warn("Circuit breaker OPEN for {}; failing over", providerName);
                lastError = "Circuit OPEN for " + providerName;
                continue;
            }

            TemplatedEmail payload = new TemplatedEmail(
                    EmailRecipient.of(request.recipient()),
                    defaultFrom,
                    ref,
                    request.variables());

            long start = System.nanoTime();
            ProviderResult result;
            try {
                result = provider.send(payload);
            } catch (Throwable t) {
                breaker.onError(System.nanoTime() - start, TimeUnit.NANOSECONDS, t);
                log.error("Unexpected exception from provider {}", providerName, t);
                lastError = "Unhandled exception from " + providerName + ": " + t.getMessage();
                continue;
            }
            long elapsed = System.nanoTime() - start;

            switch (result) {
                case ProviderResult.Success s -> {
                    breaker.onSuccess(elapsed, TimeUnit.NANOSECONDS);
                    log.info("Sent notification {} via {} (providerMessageId={})",
                            request.id(), providerName, s.providerMessageId());
                    return DispatchResult.sent(request.id(), providerName, s.providerMessageId());
                }
                case ProviderResult.RetryableFailure r -> {
                    Throwable cause = r.cause() != null ? r.cause() : new RuntimeException(r.reason());
                    breaker.onError(elapsed, TimeUnit.NANOSECONDS, cause);
                    lastError = providerName + " retryable: " + r.reason();
                    log.warn("Provider {} retryable failure for {}; will try next provider: {}",
                            providerName, request.id(), r.reason());
                }
                case ProviderResult.PermanentFailure p -> {
                    // Permanent failures (bad recipient, missing template, etc.) are not the
                    // provider's fault — treat as success for breaker purposes but fail the send.
                    breaker.onSuccess(elapsed, TimeUnit.NANOSECONDS);
                    log.warn("Provider {} permanent failure for {}; aborting failover: {}",
                            providerName, request.id(), p.reason());
                    return DispatchResult.failed(request.id(),
                            providerName + ": " + p.reason());
                }
            }
        }

        return DispatchResult.failed(request.id(),
                lastError == null ? "No email providers available" : lastError);
    }
}
