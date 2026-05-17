package com.trainbooking.notification.channel;

import com.trainbooking.notification.domain.enums.Channel;
import com.trainbooking.notification.template.TemplateDescriptor;

public interface NotificationChannel {

    Channel channel();

    DispatchResult dispatch(NotificationRequest request, TemplateDescriptor descriptor);
}
