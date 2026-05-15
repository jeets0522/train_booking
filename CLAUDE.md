# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Java-based train booking application — multi-module monorepo. Each service lives in its own subdirectory with its own `pom.xml` and Maven wrapper.

## Modules

| Directory | Artifact | Description |
|---|---|---|
| `accounts/` | `com.trainbooking:accounts` | Authentication & account management microservice |

---

## accounts — Build & Run

**Stack:** Spring Boot 4.0.6, Java 21, PostgreSQL, Jakarta EE 11, Spring Security 7, JJWT 0.12.x, Spring Kafka, springdoc-openapi 3.x

### Build

```bash
cd accounts
./mvnw clean install          # full build + tests
./mvnw compile                # compile only
./mvnw package -DskipTests    # package JAR without running tests
```

### Run

```bash
cd accounts
./mvnw spring-boot:run
```

The service starts on `http://localhost:8080`.

### Tests

```bash
# All tests
./mvnw test

# Single test class
./mvnw test -Dtest=AuthServiceTest

# Single test method
./mvnw test -Dtest=AuthServiceTest#testLogin
```

### API docs (Swagger UI)

`http://localhost:8080/swagger-ui.html`

OpenAPI JSON: `http://localhost:8080/api-docs`

---

## accounts — Architecture

```
com.trainbooking.accounts
├── AccountsApplication.java          — entry point, @EnableConfigurationProperties
├── config/
│   ├── KafkaConfig.java              — KafkaTemplate<String, Object> bean
│   ├── OpenApiConfig.java            — Swagger/OpenAPI bean
│   └── SecurityConfig.java           — Spring Security filter chain, stateless JWT
├── controller/
│   └── AuthController.java           — /auth/* REST endpoints
├── domain/
│   ├── entity/                       — JPA entities (User, Role, Permission, RefreshToken, VerificationToken)
│   └── enums/
│       └── TokenType.java
├── dto/
│   ├── request/                      — RegisterRequest, LoginRequest, RefreshRequest, …
│   └── response/                     — AuthResponse, ApiResponse
├── exception/
│   ├── AuthException.java            — base + AccountLocked / InvalidCredentials / UserAlreadyExists inner classes
│   ├── VerificationException.java
│   └── GlobalExceptionHandler.java   — @RestControllerAdvice
├── kafka/
│   ├── events/                       — EmailVerificationEvent, WelcomeEmailEvent, PhoneOtpEvent
│   └── NotificationProducer.java     — sends events to Kafka topics
├── repository/                       — Spring Data JPA repositories
├── security/
│   ├── JwtProperties.java            — @ConfigurationProperties(prefix="jwt")
│   ├── JwtService.java               — token generation & validation (JJWT 0.12.x)
│   ├── JwtAuthFilter.java            — OncePerRequestFilter, reads Bearer header
│   └── UserDetailsServiceImpl.java   — loads user + roles + permissions
├── service/
│   ├── AuthService.java              — register, login, logout, refresh
│   └── VerificationService.java      — email & phone OTP verification
└── util/
    └── HashUtil.java                 — SHA-256 hex helper
```

### Database schema

Schema: `account_service` (PostgreSQL, `train_booking` database)

Tables: `users`, `roles`, `permissions`, `role_permissions`, `user_roles`, `refresh_tokens`, `verification_tokens`

DDL is managed externally — `spring.jpa.hibernate.ddl-auto=none`.

### Key design decisions

- All tokens (refresh tokens, email verification, phone OTP) are stored as SHA-256 hashes; raw tokens are never persisted.
- JWT is stateless (access token); only refresh tokens are persisted.
- No refresh token rotation — a new access token is issued on `/auth/refresh` but the refresh token is unchanged.
- Account locking is automatic after N failed login attempts (configurable via `auth.max-failed-attempts`). Locks auto-expire after `auth.lock-duration-minutes`.
- `ip_address` columns are PostgreSQL `INET` type — mapped as `String` with `@Column(columnDefinition = "inet")`.
- `metadata` in `verification_tokens` is PostgreSQL `JSONB` — mapped as `String` with `@Column(columnDefinition = "jsonb")`.
