package com.trainbooking.notification.provider;

public sealed interface ProviderResult
        permits ProviderResult.Success,
                ProviderResult.RetryableFailure,
                ProviderResult.PermanentFailure {

    record Success(String providerMessageId) implements ProviderResult {}

    record RetryableFailure(String reason, Throwable cause) implements ProviderResult {
        public RetryableFailure(String reason) {
            this(reason, null);
        }
    }

    record PermanentFailure(String reason, Throwable cause) implements ProviderResult {
        public PermanentFailure(String reason) {
            this(reason, null);
        }
    }
}
