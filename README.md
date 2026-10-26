# Food Delivery Platform

A Java microservices learning project for building food-delivery capabilities incrementally. The repository currently implements User, Delivery, Notification, and a Restaurant/Menu Catalog service. Cart, Order, Payment, and API Gateway capabilities are not implemented.

## Architecture and current status

- **User Service** manages user registration, login, profiles, and JWT-protected user operations. It persists users in MySQL, uses Redis for caching, and publishes registration events to Kafka after the database transaction commits.
- **Delivery Service** manages delivery-partner records and availability in MySQL, and uses Redis sets and GEO operations for availability and location projections.
- **Notification Service** consumes and validates user-registration events from Kafka and logs them. It does not deliver email or SMS.
- **Catalog Service** manages restaurant details and activation plus menu items, availability, pagination, and soft deletion in its own MySQL schema.

The services are separate Spring Boot applications with independent Maven wrappers. See [`ai/architecture.md`](ai/architecture.md) for architecture notes and [`ai/current-phase.md`](ai/current-phase.md) for the authoritative current status, verification evidence, deferred risks, and proposed next feature.

## Implemented capabilities

### User Service

- User registration, retrieval, profile update, and soft deletion.
- Request validation, BCrypt password hashing, duplicate identifier checks, and account status handling.
- Public registration and login endpoints, with stateless JWT authentication for protected user endpoints.
- Profile update and deletion ownership checks based on the authenticated principal. The authenticated read-by-ID endpoint remains ID-based and does not restrict reads to the caller's own account.
- Redis-backed user response caching configured with a 10-minute TTL and transaction-aware cache operations.
- A `UserRegistered` event is scheduled after a successful registration transaction commits.

Email changes invalidate tokens containing the prior email subject, requiring the user to log in again. Deletion sets the account status to `DELETED`; it does not remove the row or publish a deletion event.

### Delivery Service

The Delivery Service exposes operations under `/api/v1/delivery-partners` to create, retrieve, list, and delete partners; change availability; update location; and search nearby available partners. Partner records and availability status are persisted in MySQL. Redis stores availability and location projections; location updates are stored in Redis GEO and are not persisted in MySQL.

Availability changes save the database state first and schedule the Redis projection update after commit. Deletion removes the database record and schedules Redis projection removal after commit. This avoids applying these Redis changes for a rolled-back database transaction, but a Redis failure after commit can still leave the projection inconsistent. The stores are not one atomic transaction, and no reconciliation mechanism is implemented.

Delivery assignment and order workflows are not implemented.

### Catalog Service

The Catalog Service is an independent Spring Boot application at `services/catalog-service`, configured for local port `8083` and its own MySQL schema, `catalog_db`. It manages restaurant creation, retrieval, paginated/sorted listing, updating, and activation, as well as menu-item creation, retrieval, listing, updating, availability, and soft deletion. DTO validation and consistent API errors are implemented. Flyway manages the schema and Hibernate validates it.

The focused automated unit/MVC test run reported **40 tests passed, 0 failures, 0 errors, and 0 skipped**. Runtime application startup, HTTP smoke tests, HTTP-to-MySQL persistence, and persistence after restart remain pending; the service is not fully end-to-end verified or production-ready. It has no authentication or authorization and is intended only for trusted local development. Restaurant activation is a catalog flag and does not represent real-time order acceptance. Menu prices are currently denominated in INR.

### Notification Service and Kafka events

The User Service publishes `UserRegistered` events to `user.events`, keyed by event ID, after the registration transaction commits. The Notification Service consumes the event, validates its ID, user ID, and occurrence time, then logs it. Email/SMS delivery and consumer idempotency are not implemented.

The Notification Service uses two retries at one-second intervals after the initial delivery for retryable processing failures (three attempts total). Recovered records are logged by topic, partition, and offset, then acknowledged. Fatal deserialization failures are recovered without retries under Spring Kafka's default classification. There is no dead-letter queue (DLQ), durable replay mechanism, or consumer idempotency tracking.

Database commit and Kafka publication are not atomic. A process failure after commit can leave a user registration without a corresponding event; there is no transactional outbox.

## Technology stack

**Present in service code/configuration:** Java 25, Spring Boot 4, Maven, Spring MVC, Spring Data JPA, MySQL, Flyway, Redis, Apache Kafka, Spring Security, JWT, and Spring Boot Actuator in the User Service.

**Planned, not integrated platform capabilities:** Cart, Order, Payment, API Gateway, email/SMS delivery, Docker/Kubernetes deployment, CI/CD, AWS deployment, Prometheus, Grafana, and OpenTelemetry.

## Local service instructions

The commands below are already documented for this repository. The service configuration uses the listed environment-variable names; successful startup and infrastructure connectivity were not verified during this documentation update. Check each service's `src/main/resources/application.properties` and arrange the required infrastructure and credentials using a local secret mechanism. Do not commit secrets.

- **User Service:** requires its configured MySQL datasource and Redis for cache operations; Kafka is needed for registration-event publication. Configuration keys include `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET_KEY`, and the optional Kafka override `KAFKA_BOOTSTRAP_SERVERS`. The application is configured for port `8081`.
- **Delivery Service:** requires its configured MySQL datasource and Redis. Configuration keys include `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`. The application is configured for port `8082`.
- **Catalog Service:** requires a dedicated MySQL schema named `catalog_db`. Configure `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`; Flyway manages the service tables and Hibernate validates them. The service is configured for port `8083`. Its Maven command is documented in [the Catalog Service README](services/catalog-service/README.md); runtime startup and port availability have not been verified.
- **Notification Service:** requires a reachable Kafka broker. Configuration keys include `KAFKA_BOOTSTRAP_SERVERS` and the optional `KAFKA_CONSUMER_GROUP_ID`. No HTTP port is documented here.

Run the services from their respective directories in PowerShell:

```powershell
Set-Location D:\food-delivery-platform\services\user-service
.\mvnw.cmd spring-boot:run
```

```powershell
Set-Location D:\food-delivery-platform\services\delivery-service
.\mvnw.cmd spring-boot:run
```

```powershell
Set-Location D:\food-delivery-platform\services\notification-service
.\mvnw.cmd spring-boot:run
```

The checked-in local Kafka fallback is environment-specific. Use the configured override when that fallback is not reachable; do not assume a particular local broker address will work in another environment.

## Testing and verification

The following focused results are recorded for recent work:

| Service and focus | Reported result | What it establishes |
|---|---:|---|
| User Service JWT/security | 17 passed | Focused JWT/security unit and MVC tests; not external integration verification. |
| User Service cache/event publisher | 8 passed | Focused cache configuration/error-handler and event-publisher tests; not live Redis or Kafka verification. |
| Delivery Service transaction callbacks | 4 passed | Mock-based tests that manually invoke transaction callbacks; not real MySQL/Redis transaction behavior. |
| Notification Service consumer/config/JSON | 7 passed | Unit/mock-based consumer, serialization, and error-handler behavior; not broker-backed processing. |
| Catalog Service unit/MVC | 40 passed | Focused service and MVC tests; no runtime HTTP or MySQL integration. |

Catalog result: **40 tests passed, 0 failures, 0 errors, 0 skipped** in the reported focused unit/MVC run. This focused run does not show that every service's full test suite passes or that external integrations work end-to-end. Runtime HTTP smoke testing and end-to-end database persistence verification are still pending.

`JwtAuthenticationIntegrationTest` is database-backed and has not been run because the test database and Redis isolation have not been independently verified. Its setup deletes user rows in the selected schema. Test-runner properties naming `food_delivery_user_test` and Redis logical DB 15 do not prove those resources are dedicated or safe to modify. Do not run cleanup-capable database/Redis tests until resource ownership and isolation are confirmed.

Additional verification gaps:

- The security MVC test invokes the global 403 exception handler directly; the configured Spring Security filter-chain 403 path is not directly verified.
- Cache error-handler tests verify logging and suppression, not that a cached service successfully falls back to a database query.
- Delivery callback tests use mocks and manually invoked callbacks, not live MySQL and Redis.
- Notification tests do not use a Kafka broker.
- Catalog tests do not run Flyway against MySQL or verify database constraints and persistence.

## Known limitations and next steps

- Redis projection failures after commit can leave Delivery Service data inconsistent; there is no reconciliation mechanism.
- Cache eviction failures can leave stale cached user data until expiration or a later successful eviction.
- Registration database commits and Kafka publication are not atomic; there is no transactional outbox.
- Kafka recovery has no durable DLQ or replay mechanism.
- Database-backed authentication/deleted-user tests remain deferred pending verified test-resource isolation.
- Delivery assignment, Cart, Order, Payment, API Gateway, and notification-provider workflows are not implemented.
- Catalog runtime smoke testing, actual HTTP-to-MySQL persistence, and restart persistence remain pending. The Catalog Service currently has no authorization and is not suitable for public exposure.
- Docker/Kubernetes, CI/CD, AWS deployment, and expanded observability are planned rather than integrated.

**Current Status and Next Steps:** The Catalog Service implementation phase is complete, with 40 focused unit/MVC tests reported passing. Runtime startup, HTTP smoke testing, actual database persistence, and authorization remain unverified. Phase 6 security and Phase 7 reliability work remain only partially verified. See [`ai/current-phase.md`](ai/current-phase.md) for current status and deferred risks.

## Project documentation

- [`ai/architecture.md`](ai/architecture.md) — architecture and service responsibilities.
- [`ai/current-phase.md`](ai/current-phase.md) — authoritative current status, test evidence, known limitations, and proposed roadmap.
- [`ai/coding-guidelines.md`](ai/coding-guidelines.md) — project coding guidelines.
