package com.trainbooking.accounts.exception;

public class AuthException extends RuntimeException {

    public AuthException(String message) {
        super(message);
    }

    public static class AccountLockedException extends AuthException {
        public AccountLockedException(String message) {
            super(message);
        }
    }

    public static class InvalidCredentialsException extends AuthException {
        public InvalidCredentialsException(String message) {
            super(message);
        }
    }

    public static class UserAlreadyExistsException extends AuthException {
        public UserAlreadyExistsException(String message) {
            super(message);
        }
    }
}
