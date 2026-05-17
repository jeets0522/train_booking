package com.trainbooking.notification.template;

import com.trainbooking.notification.domain.enums.Channel;
import com.trainbooking.notification.domain.enums.ProviderName;
import com.trainbooking.notification.domain.enums.SmsMode;

import java.util.List;
import java.util.Map;

public record TemplateDescriptor(
        String key,
        Channel channel,
        SmsMode mode,
        Map<ProviderName, ProviderTemplateRef> providers,
        List<String> variables,
        String body,
        String titleKey,
        String bodyKey) {

    public ProviderTemplateRef refFor(ProviderName provider) {
        return providers == null ? null : providers.get(provider);
    }

    public boolean hasProvider(ProviderName provider) {
        return providers != null && providers.containsKey(provider);
    }
}
