package com.trainbooking.notification.provider.email;

public record EmailRecipient(String email, String name) {

    public static EmailRecipient of(String email) {
        return new EmailRecipient(email, null);
    }
}
