package com.trainbooking.notification.exception;

public class NotificationException extends RuntimeException {

    public NotificationException(String message) {
        super(message);
    }

    public NotificationException(String message, Throwable cause) {
        super(message, cause);
    }

    public static class NotFoundException extends NotificationException {
        public NotFoundException(String message) {
            super(message);
        }
    }

    public static class DispatchException extends NotificationException {
        public DispatchException(String message) {
            super(message);
        }

        public DispatchException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
