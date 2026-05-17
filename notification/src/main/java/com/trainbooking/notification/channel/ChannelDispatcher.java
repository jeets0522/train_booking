package com.trainbooking.notification.channel;

import com.trainbooking.notification.domain.enums.Channel;
import com.trainbooking.notification.exception.NotificationException;
import com.trainbooking.notification.template.TemplateDescriptor;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class ChannelDispatcher {

    private final Map<Channel, NotificationChannel> byChannel;

    public ChannelDispatcher(List<NotificationChannel> channels) {
        EnumMap<Channel, NotificationChannel> map = new EnumMap<>(Channel.class);
        for (NotificationChannel c : channels) {
            map.put(c.channel(), c);
        }
        this.byChannel = Map.copyOf(map);
    }

    public DispatchResult dispatch(NotificationRequest request, TemplateDescriptor descriptor) {
        NotificationChannel channel = byChannel.get(request.channel());
        if (channel == null) {
            throw new NotificationException(
                    "No channel registered for: " + request.channel());
        }
        return channel.dispatch(request, descriptor);
    }
}
