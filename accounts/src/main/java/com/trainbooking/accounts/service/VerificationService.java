package com.trainbooking.accounts.service;

import com.trainbooking.accounts.domain.entity.User;
import com.trainbooking.accounts.domain.entity.VerificationToken;
import com.trainbooking.accounts.dto.request.ResendEmailRequest;
import com.trainbooking.accounts.dto.request.VerifyEmailRequest;
import com.trainbooking.accounts.dto.request.VerifyPhoneRequest;
import com.trainbooking.accounts.dto.response.ApiResponse;
import com.trainbooking.accounts.exception.VerificationException;
import com.trainbooking.accounts.kafka.DomainEventPublisher;
import com.trainbooking.accounts.kafka.events.domain.EmailVerificationRequested;
import com.trainbooking.accounts.kafka.events.domain.PhoneOtpRequested;
import com.trainbooking.accounts.repository.UserRepository;
import com.trainbooking.accounts.repository.VerificationTokenRepository;
import com.trainbooking.accounts.util.HashUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

@Service
public class VerificationService {

    private final VerificationTokenRepository verificationTokenRepository;
    private final UserRepository userRepository;
    private final DomainEventPublisher domainEventPublisher;

    public VerificationService(
            VerificationTokenRepository verificationTokenRepository,
            UserRepository userRepository,
            DomainEventPublisher domainEventPublisher) {
        this.verificationTokenRepository = verificationTokenRepository;
        this.userRepository = userRepository;
        this.domainEventPublisher = domainEventPublisher;
    }

    @Transactional
    public void sendEmailVerification(User user) {
        String rawToken = UUID.randomUUID().toString();
        String tokenHash = HashUtil.sha256(rawToken);

        Instant expiresAt = Instant.now().plusSeconds(86400); // 24 hours

        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setTokenHash(tokenHash);
        verificationToken.setUserId(user.getId());
        verificationToken.setTokenType("EMAIL_VERIFICATION");
        verificationToken.setExpiresAt(expiresAt);

        verificationTokenRepository.save(verificationToken);

        domainEventPublisher.publish(EmailVerificationRequested.of(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                rawToken,
                expiresAt));
    }

    @Transactional
    public ApiResponse verifyEmail(VerifyEmailRequest request) {
        String tokenHash = HashUtil.sha256(request.getToken());

        VerificationToken token = verificationTokenRepository
                .findByTokenHashAndTokenTypeAndConsumedAtIsNull(tokenHash, "EMAIL_VERIFICATION")
                .orElseThrow(() -> new VerificationException("Verification token not found or already used"));

        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new VerificationException("Verification token has expired");
        }

        token.setConsumedAt(Instant.now());
        verificationTokenRepository.save(token);

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new VerificationException("User not found"));
        user.setEmailVerified(true);
        userRepository.save(user);

        return ApiResponse.success("Email verified successfully");
    }

    @Transactional
    public ApiResponse resendEmailVerification(ResendEmailRequest request) {
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);

        if (user == null || user.isEmailVerified()) {
            // Don't expose whether user exists or is already verified
            return ApiResponse.success("Verification email sent");
        }

        sendEmailVerification(user);
        return ApiResponse.success("Verification email sent");
    }

    @Transactional
    public void sendPhoneOtp(User user) {
        String otp = String.format("%06d", new SecureRandom().nextInt(1_000_000));
        String otpHash = HashUtil.sha256(otp);

        Instant expiresAt = Instant.now().plusSeconds(600); // 10 minutes

        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setTokenHash(otpHash);
        verificationToken.setUserId(user.getId());
        verificationToken.setTokenType("PHONE_VERIFICATION");
        verificationToken.setExpiresAt(expiresAt);

        verificationTokenRepository.save(verificationToken);

        domainEventPublisher.publish(PhoneOtpRequested.of(
                user.getId(),
                user.getPhoneNumber(),
                otp,
                expiresAt));
    }

    @Transactional
    public ApiResponse verifyPhone(VerifyPhoneRequest request, User authenticatedUser) {
        String otpHash = HashUtil.sha256(request.getOtp());

        VerificationToken token = verificationTokenRepository
                .findByTokenHashAndTokenTypeAndConsumedAtIsNull(otpHash, "PHONE_VERIFICATION")
                .orElseThrow(() -> new VerificationException("OTP not found or already used"));

        if (!token.getUserId().equals(authenticatedUser.getId())) {
            throw new VerificationException("OTP does not belong to the authenticated user");
        }

        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new VerificationException("OTP has expired");
        }

        token.setConsumedAt(Instant.now());
        verificationTokenRepository.save(token);

        authenticatedUser.setPhoneVerified(true);
        userRepository.save(authenticatedUser);

        return ApiResponse.success("Phone number verified successfully");
    }
}
