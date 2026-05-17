package com.trainbooking.notification.provider.email;

import com.trainbooking.notification.domain.enums.ProviderName;
import com.trainbooking.notification.provider.ProviderResult;

public interface EmailProvider {

    ProviderResult send(TemplatedEmail payload);

    ProviderName name();
}
