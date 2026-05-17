package com.trainbooking.notification.domain.enums;

public enum ProviderName {
    SES("ses"),
    SENDGRID("sendgrid"),
    TWILIO("twilio"),
    SNS("sns"),
    META("meta"),
    FCM("fcm");

    private final String wireName;

    ProviderName(String wireName) {
        this.wireName = wireName;
    }

    public String wireName() {
        return wireName;
    }

    public static ProviderName fromWireName(String wireName) {
        for (ProviderName p : values()) {
            if (p.wireName.equalsIgnoreCase(wireName)) {
                return p;
            }
        }
        throw new IllegalArgumentException("Unknown provider: " + wireName);
    }
}
