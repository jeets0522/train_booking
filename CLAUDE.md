# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Java-based train booking application — multi-module monorepo. Each service lives in its own subdirectory with its own `pom.xml` and Maven wrapper.

## Modules

| Directory | Artifact | Description |
|---|---|---|
| `accounts/` | `com.trainbooking:accounts` | Authentication & account management microservice |
| `notification/` | `com.trainbooking:notification` | Multi-channel notification dispatcher (Email v1; SMS / WhatsApp / Push scaffolded) |

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
│   ├── events/domain/                — sealed DomainEvent + UserRegistered, EmailVerificationRequested, PhoneOtpRequested (eventType discriminator in JSON)
│   └── DomainEventPublisher.java     — publishes domain facts to the account.events topic
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

---

## notification — Build & Run

**Stack:** Spring Boot 4.0.6, Java 21, PostgreSQL, Spring Kafka (consumer), AWS SDK v2 (SES), sendgrid-java, Resilience4j 2.x (circuit breaker), springdoc-openapi 3.x

### Build

```bash
cd notification
./mvnw clean install          # full build + tests
./mvnw compile                # compile only
./mvnw package -DskipTests
```

### Run

```bash
cd notification
./mvnw spring-boot:run
```

The service starts on `http://localhost:8081`.

Provider credentials are read from env vars:

| Var | Used by | Notes |
|---|---|---|
| `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` | SES | Standard AWS SDK credential chain; `~/.aws/credentials` and instance roles also work |
| `AWS_REGION` | SES | Defaults to `us-east-1` |
| `SENDGRID_API_KEY` | SendGrid | If unset, the `SendGrid` bean is not created and email falls back to SES only |

### Tests

```bash
./mvnw test
```

### API docs (Swagger UI)

`http://localhost:8081/swagger-ui.html`

OpenAPI JSON: `http://localhost:8081/api-docs`

---

## notification — Architecture

```
com.trainbooking.notification
├── NotificationApplication.java        — entry point, @EnableScheduling
├── config/
│   ├── ProviderConfig.java             — SesClient + SendGrid beans (SendGrid conditional on api-key)
│   ├── ResilienceConfig.java           — CircuitBreakerRegistry (per-provider breakers)
│   └── OpenApiConfig.java
├── controller/
│   ├── NotificationController.java     — GET /notifications/{id}
│   ├── WebhookController.java          — POST /webhooks/ses, /webhooks/sendgrid
│   └── SuppressionController.java      — admin CRUD on /suppressions
├── domain/
│   ├── entity/                         — Notification, SuppressionEntry, DeliveryEvent
│   └── enums/                          — Channel, NotificationStatus, ProviderName, SuppressionReason, SmsMode
├── dto/
│   ├── request/                        — AddSuppressionRequest
│   └── response/                       — NotificationResponse, ApiResponse
├── exception/
│   ├── NotificationException.java
│   ├── TemplateException.java
│   ├── ProviderException.java
│   └── GlobalExceptionHandler.java
├── kafka/
│   ├── events/domain/                  — Mirrored sealed DomainEvent + subtypes from accounts (byte-compatible)
│   ├── NotificationCommand.java        — record: resolved delivery instruction (channel, priority, templateKey, variables)
│   ├── NotificationCommandPublisher.java — routes a command to its (channel, priority) delivery topic
│   ├── DomainEventConsumer.java        — @KafkaListener on account.events → NotificationPolicy → publish commands
│   └── DeliveryConsumer.java           — @KafkaListener per delivery topic → NotificationRequest → NotificationService.send()
├── policy/
│   └── NotificationPolicy.java         — maps a DomainEvent → 0..n NotificationCommands (the only event→template mapping)
├── repository/                         — Spring Data JPA repos
├── template/
│   ├── TemplateCatalog.java            — loads template-catalog.yml at startup
│   ├── TemplateDescriptor.java         — record (key, channel, mode, providers, variables, body, titleKey, bodyKey)
│   ├── ProviderTemplateRef.java        — sealed: SesTemplateRef | SendGridTemplateRef | TwilioContentRef | SnsApprovedBody | MetaTemplateRef
│   └── VariableValidator.java
├── provider/
│   ├── ProviderResult.java             — sealed: Success | RetryableFailure | PermanentFailure
│   └── email/
│       ├── EmailProvider.java          — interface
│       ├── TemplatedEmail.java         — record (to, from, templateRef, variables)
│       ├── EmailRecipient.java
│       ├── SesEmailProvider.java       — AWS SDK v2 sendTemplatedEmail
│       └── SendGridEmailProvider.java  — sendgrid-java dynamic template
├── channel/
│   ├── NotificationChannel.java        — interface (channel() + dispatch())
│   ├── NotificationRequest.java        — record (input to dispatch)
│   ├── DispatchResult.java             — record (output of dispatch)
│   ├── ChannelDispatcher.java          — Map<Channel, NotificationChannel> routing
│   └── email/
│       └── EmailChannel.java           — provider failover via Resilience4j circuit breakers
└── service/
    ├── NotificationService.java        — orchestrator: persist → suppress → render → dispatch → update
    ├── SuppressionService.java
    ├── DeliveryEventService.java       — webhook processor; updates Notification status, adds suppression on bounce/complaint
    └── RetryWorker.java                — @Scheduled, picks up stuck PENDING/FAILED notifications
```

### Template catalog

`src/main/resources/template-catalog.yml` is the integration contract between code and the providers. Each entry maps an internal template key to provider-specific template IDs:

```yaml
templates:
  EMAIL_VERIFICATION:
    channel: EMAIL
    providers:
      ses:      { template_name: notification_email_verification_v1 }
      sendgrid: { template_id: d-PLACEHOLDER-EMAIL-VERIFICATION }
    variables: [otp, firstName, expiresAt]
```

Template **content** (HTML body, subject) lives in the providers — SES Console / SendGrid Dynamic Templates. This service never renders HTML for email; it passes `(template_id, variables)` and lets the provider merge. Placeholders in the shipped catalog (`d-PLACEHOLDER-*`) must be replaced with real IDs before sending.

### Database schema

Schema: `notification_service` (same `vectordbpg` database; declared in `accounts/db/sql/db.sql`)

Tables: `notifications`, `suppression_list`, `delivery_events`

DDL: `notification/db/sql/db.sql` (run after `accounts/db/sql/db.sql`). `spring.jpa.hibernate.ddl-auto=none`.

### Kafka topics & flow

Two-stage delivery. Topic names must match `application.properties` on both sides; all topics are auto-created via `NewTopic` beans on startup.

**Stage 1 — inbound domain events.** `accounts.kafka.DomainEventPublisher` emits *facts* (it knows nothing about templates/channels) to a single topic:

| Topic | Event types (sealed `DomainEvent`, `eventType` discriminator) | v1 status |
|---|---|---|
| `account.events` | `UserRegistered`, `EmailVerificationRequested`, `PhoneOtpRequested` | ✓ consumed by `DomainEventConsumer` |

`DomainEventConsumer` runs `NotificationPolicy.apply(event)` to decide which notifications result, then publishes a `NotificationCommand` per notification.

**Stage 2 — channel × priority delivery topics.** `NotificationCommandPublisher` routes by `(channel, priority)`; `DeliveryConsumer` has one `@KafkaListener` per topic → `NotificationService.send()`:

| Topic | Routed when | v1 status |
|---|---|---|
| `notification.email.transactional` | EMAIL + TRANSACTIONAL (e.g. EMAIL_VERIFICATION) | ✓ active |
| `notification.email.bulk` | EMAIL + BULK (e.g. WELCOME_EMAIL) | ✓ active |
| `notification.sms.transactional` | SMS + TRANSACTIONAL (PhoneOtpRequested) | listener wired; template reserved (SMS channel deferred) |

Adding a new notification = a `templateKey` + a `template-catalog.yml` entry + one line in `NotificationPolicy`. No new topic/listener/event type.

Idempotency: each domain event carries a unique `eventId`; the command id is `nameUUIDFromBytes(eventId + ":" + templateKey)`, so redelivery maps to the same notification row and `NotificationService.send()` short-circuits.

Cross-service event POJOs are duplicated (not in a shared module). Field structure must stay byte-compatible.

### Key design decisions

- **Provider-hosted templates.** Email templates live in SES *and* SendGrid; the service references them by ID via `template-catalog.yml`. Same pattern planned for WhatsApp (Meta-approved) and India SMS (DLT-registered). Tradeoff: every new template must be created in all providers + catalog entry; upside: non-engineers can edit copy through provider UI, and the service carries no rendering engine.
- **Sealed `ProviderResult`** lets each provider classify outcomes as `Success`, `RetryableFailure` (channel will fail over), or `PermanentFailure` (channel aborts — failover wouldn't help for bad input). Permanent failures do NOT trip the circuit breaker; retryable ones do.
- **Two-stage delivery (domain events → commands).** Producers emit domain *facts* on `account.events`; `NotificationPolicy` (in the notification service) decides which notifications they imply and emits a `NotificationCommand` per notification onto channel × priority delivery topics. Topology is governed by transport concerns (channel, priority), not message variety — new email kinds are data (a `templateKey` + catalog + policy line), never new topics.
- **Idempotent consumer.** Each domain event carries a unique `eventId`; the notification PK is `nameUUIDFromBytes(eventId + ":" + templateKey)` (so one event fanning out to N notifications yields N stable ids). Redelivery short-circuits at `NotificationService.send()` if the notification is already SENT/DELIVERED.
- **Per-provider circuit breakers** named `email-{provider}` (e.g. `email-ses`). Configured globally via `notification.circuit-breaker.*` properties; metrics exposed via actuator.
- **Suppression list** is checked before every send. Bounce / complaint events from provider webhooks auto-populate it.
- **Webhook signature verification is NOT yet implemented** — SES (via SNS) and SendGrid endpoints currently accept any payload. Add signature checks before exposing publicly.
- **Retry worker** runs every `notification.retry.scan-interval-ms` (default 30s), picks up PENDING/FAILED rows older than `stuck-after-seconds` with `attempts < max-attempts`, re-dispatches via the same code path. Single-instance only for v1; needs distributed locking or per-row pessimistic lock if scaled out.
- **`template_variables` is PostgreSQL `JSONB`** — mapped as `String` with `@Column(columnDefinition = "jsonb")`, serialized via Jackson in `NotificationService`.
