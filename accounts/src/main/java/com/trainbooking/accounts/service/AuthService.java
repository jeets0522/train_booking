package com.trainbooking.accounts.service;

import com.trainbooking.accounts.domain.entity.RefreshToken;
import com.trainbooking.accounts.domain.entity.Role;
import com.trainbooking.accounts.domain.entity.User;
import com.trainbooking.accounts.dto.request.LoginRequest;
import com.trainbooking.accounts.dto.request.RefreshRequest;
import com.trainbooking.accounts.dto.request.RegisterRequest;
import com.trainbooking.accounts.dto.response.ApiResponse;
import com.trainbooking.accounts.dto.response.AuthResponse;
import com.trainbooking.accounts.exception.AuthException;
import com.trainbooking.accounts.kafka.DomainEventPublisher;
import com.trainbooking.accounts.kafka.events.domain.UserRegistered;
import com.trainbooking.accounts.repository.RefreshTokenRepository;
import com.trainbooking.accounts.repository.RoleRepository;
import com.trainbooking.accounts.repository.UserRepository;
import com.trainbooking.accounts.security.JwtProperties;
import com.trainbooking.accounts.security.JwtService;
import com.trainbooking.accounts.util.HashUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final VerificationService verificationService;
    private final DomainEventPublisher domainEventPublisher;

    @Value("${auth.max-failed-attempts}")
    private int maxFailedAttempts;

    @Value("${auth.lock-duration-minutes}")
    private long lockDurationMinutes;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            JwtProperties jwtProperties,
            VerificationService verificationService,
            DomainEventPublisher domainEventPublisher) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.verificationService = verificationService;
        this.domainEventPublisher = domainEventPublisher;
    }

    @Transactional
    public ApiResponse register(RegisterRequest request, HttpServletRequest httpRequest) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AuthException.UserAlreadyExistsException(
                    "Username '" + request.getUsername() + "' is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AuthException.UserAlreadyExistsException(
                    "Email '" + request.getEmail() + "' is already registered");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setEnabled(true);
        user.setEmailVerified(false);
        user.setPhoneVerified(false);
        user.setAccountLocked(false);
        user.setAccountExpired(false);
        user.setCredentialsExpired(false);
        user.setFailedLoginAttempts(0);
        user.setPasswordChangedAt(Instant.now());

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> {
                    Role r = new Role();
                    r.setName("ROLE_USER");
                    r.setDescription("Default user role");
                    return roleRepository.save(r);
                });
        user.setRoles(Set.of(userRole));

        user = userRepository.save(user);

        domainEventPublisher.publish(UserRegistered.of(
                user.getId(),
                user.getEmail(),
                user.getFirstName()));

        verificationService.sendEmailVerification(user);

        return ApiResponse.success("Registration successful. Please verify your email.");
    }

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        User user = userRepository.findByUsernameOrEmail(request.getUsernameOrEmail(), request.getUsernameOrEmail())
                .orElseThrow(() -> new AuthException.InvalidCredentialsException("Invalid credentials"));

        // Check if account is locked
        if (user.isAccountLocked()) {
            Instant lockExpiry = user.getLockTime() != null
                    ? user.getLockTime().plusSeconds(lockDurationMinutes * 60)
                    : Instant.MIN;

            if (lockExpiry.isAfter(Instant.now())) {
                throw new AuthException.AccountLockedException(
                        "Account is locked. Please try again later.");
            } else {
                // Lock has expired — auto-unlock
                user.setAccountLocked(false);
                user.setFailedLoginAttempts(0);
                user.setLockTime(null);
            }
        }

        if (!user.isEnabled()) {
            throw new AuthException("Account is disabled");
        }
        if (user.isAccountExpired()) {
            throw new AuthException("Account has expired");
        }
        if (user.isCredentialsExpired()) {
            throw new AuthException("Credentials have expired");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= maxFailedAttempts) {
                user.setAccountLocked(true);
                user.setLockTime(Instant.now());
            }
            userRepository.save(user);
            throw new AuthException.InvalidCredentialsException("Invalid credentials");
        }

        // Successful login
        user.setFailedLoginAttempts(0);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList());

        String accessToken = jwtService.generateAccessToken(user.getUsername(), roles);

        // Generate refresh token
        String rawRefreshToken = UUID.randomUUID().toString();
        String refreshTokenHash = HashUtil.sha256(rawRefreshToken);

        RefreshToken refreshTokenEntity = new RefreshToken();
        refreshTokenEntity.setTokenHash(refreshTokenHash);
        refreshTokenEntity.setUserId(user.getId());
        refreshTokenEntity.setExpiresAt(
                Instant.now().plusSeconds(jwtProperties.getRefreshTokenExpirySeconds()));
        refreshTokenEntity.setIpAddress(httpRequest.getRemoteAddr());
        refreshTokenEntity.setUserAgent(httpRequest.getHeader("User-Agent"));

        refreshTokenRepository.save(refreshTokenEntity);

        return new AuthResponse(
                accessToken,
                rawRefreshToken,
                jwtProperties.getAccessTokenExpirySeconds()
        );
    }

    @Transactional
    public ApiResponse logout(String rawRefreshToken) {
        String tokenHash = HashUtil.sha256(rawRefreshToken);

        RefreshToken refreshToken = refreshTokenRepository
                .findByTokenHashAndRevokedAtIsNull(tokenHash)
                .orElseThrow(() -> new AuthException("Refresh token not found or already revoked"));

        refreshToken.setRevokedAt(Instant.now());
        refreshTokenRepository.save(refreshToken);

        return ApiResponse.success("Logged out successfully");
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        String tokenHash = HashUtil.sha256(request.getRefreshToken());

        RefreshToken refreshToken = refreshTokenRepository
                .findByTokenHashAndRevokedAtIsNull(tokenHash)
                .orElseThrow(() -> new AuthException("Invalid refresh token"));

        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new AuthException("Refresh token expired");
        }

        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new AuthException("User not found"));

        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList());

        String newAccessToken = jwtService.generateAccessToken(user.getUsername(), roles);

        return new AuthResponse(
                newAccessToken,
                request.getRefreshToken(), // same raw refresh token, no rotation
                jwtProperties.getAccessTokenExpirySeconds()
        );
    }
}
