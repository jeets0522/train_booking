package com.trainbooking.notification.provider.email;

import com.trainbooking.notification.template.ProviderTemplateRef;

import java.util.Map;

public record TemplatedEmail(
        EmailRecipient to,
        EmailRecipient from,
        ProviderTemplateRef templateRef,
        Map<String, Object> variables) {
}
