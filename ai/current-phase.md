# Current Project Status

## 1. Project Status

The repository contains implementations for the User, Delivery, Notification, and Catalog services. The historical phase numbering is inconsistent across older project documents; the Phase 6 and Phase 7 labels below describe the security and reliability work reviewed here, not a claim that every historical phase is complete.

Phase 6 security and Phase 7 reliability work are implemented in part. Focused tests have been reported as passing, but database-, Redis-, and Kafka-backed end-to-end verification remains incomplete. Neither phase is considered fully verified.

The Catalog Service implementation phase is complete. Its focused unit and MVC test run reported 40 tests passed with 0 failures, 0 errors, and 0 skipped. Runtime HTTP smoke testing and end-to-end database persistence verification remain pending; this status does not reconstruct historical phase numbering or claim full end-to-end verification.

## 2. Implemented Services and Capabilities

### User Service

- User registration, retrieval, profile update, and soft deletion.
- MySQL persistence through Spring Data JPA and Flyway.
- Request validation, BCrypt password hashing, duplicate-identifier checks, and account status handling.
- Public login and registration endpoints; stateless JWT authentication for protected user endpoints.
- Redis-backed user response caching with a 10-minute TTL and transaction-aware cache operations.
- Registration schedules a `UserRegistered` Kafka event after the database transaction commits.

### Delivery Service

- Delivery-partner creation, retrieval, deletion, availability changes, and nearby-partner lookup.
- MySQL persistence for partner records and availability status.
- Redis set and GEO operations for availability and location projections.
- Availability and deletion update the database within the transaction, then update Redis in an after-commit callback. A Redis failure after commit can leave the Redis projection inconsistent with MySQL.
- Location updates are written to Redis GEO; the implementation does not persist partner location in MySQL.
- Delivery assignment and order workflows are not implemented.

### Notification Service

- Consumes and validates `UserRegistered` events from Kafka and logs valid events.
- Retry/recovery handling is configured for consumer failures.
- Email and SMS delivery are not implemented.

### Catalog Service

- An independent service under `services/catalog-service` manages restaurant creation, retrieval, paginated/sorted listing, updating, and activation, plus menu-item creation, retrieval, paginated listing, updating, availability, and soft deletion.
- DTO validation and consistent API error handling are implemented.
- It uses MySQL, JPA, and Flyway with an owned `catalog_db` schema, `ddl-auto=validate`, and externalized `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`.
- Automated service unit and MVC-slice tests cover the catalog behavior; these tests do not exercise MySQL persistence.
- Menu item prices use INR with decimal precision and non-negative validation. Restaurant activation means enabled in the catalog only, not accepting orders.
- Restaurant list APIs include activation state; restaurant menu listing returns available, non-deleted items only and returns an empty page for an inactive restaurant. Direct menu item lookup returns non-deleted items, including unavailable items.
- The service has no authentication or authorization. Write endpoints are for trusted local development only and must not be publicly exposed.

### Technology

**Implemented in service code:** Java 25, Spring Boot 4, Maven wrappers, Spring MVC, Spring Data JPA, MySQL, Flyway, Redis, Apache Kafka, Spring Security, JWT, and Spring Boot Actuator in the User Service.

**Planned, not implemented as integrated platform capabilities:** Cart, Order, Payment, API Gateway, notification-provider delivery, Docker/Kubernetes deployment, CI/CD, AWS deployment, and expanded observability such as Prometheus, Grafana, and OpenTelemetry.

## 3. Phase 6 — Security Hardening

**Status: Implemented in part; focused tests reported passing; full integration verification deferred.**

- The public login flow and stateless JWT filter-chain configuration are present.
- Invalid login credentials are handled through Spring MVC exception resolution; missing or invalid JWTs are rejected by the configured Security entry point. Both paths return JSON 401 responses.
- The Security filter chain has a JSON 403 access-denied handler. The focused MVC security test invokes the global exception handler directly for 403; it does not prove the filter-chain 403 handler works end-to-end.
- JWT unit tests include malformed/invalid tokens and a deterministic expired-token case. `CustomUserDetailsServiceTest` uses mocked repository dependencies to test rejection of inactive, suspended, and deleted users at the user-details-service level.
- `JwtAuthenticationIntegrationTest` contains fuller database-backed authentication and token scenarios, including account states and rejection of an existing token after soft deletion. It has not been run because database and Redis resource isolation could not be independently verified.
- Do not treat mocked account-state tests as proof of the database-backed authentication path.

## 4. Phase 7 — Reliability Hardening

**Status: Narrow changes implemented; focused mock-based tests reported passing; integration behavior remains unverified.**

- User Service Redis cache configuration uses a 10-minute TTL and a transaction-aware cache manager. The cache error handler logs and suppresses cache errors. Its tests verify logging/suppression, not that a cached service actually falls back to a database query.
- Delivery Service availability and deletion Redis projections are scheduled after database commit; rollback should therefore avoid applying those callbacks. Tests use mocks and manually invoke transaction callbacks; they do not verify real MySQL transaction and Redis behavior.
- A Redis projection operation that fails after commit can leave Redis out of sync with MySQL. No reconciliation mechanism is implemented.
- User registration event publication is scheduled after database commit. The database commit and Kafka send are not atomic; there is no transactional outbox, so a committed registration can lack its event.
- Notification consumer retry configuration makes two retries after the initial attempt for retryable processing failures. Recovery logs record metadata and acknowledge the recovered record; fatal deserialization failures use Spring Kafka's fatal classification and are recovered without retries. There is no durable dead-letter queue (DLQ), replay mechanism, or consumer idempotency tracking.
- Consumer validation/logging is not email or SMS delivery. Consumer tests use mocks/direct error-handler calls rather than a real Kafka broker.

## 5. Verification Status

The following focused results were reported for the recent work. They are unit, MVC-slice, or mock-based results as applicable; they do not establish that all service test suites pass or that external integrations work end-to-end.

| Service and focus | Reported result | Scope and limits |
|---|---:|---|
| User Service JWT/security | 17 passed | Focused JWT/security tests; no database, Redis, or Kafka integration. Includes MVC tests and JWT unit tests. |
| User Service cache/event publisher | 8 passed | Focused configuration, cache error-handler, and event-publisher tests; mocks/configuration checks, not live Redis/Kafka. |
| Delivery Service transaction callbacks | 4 passed | Mocked repository/Redis tests with manually invoked callbacks; not a real transaction or Redis integration test. |
| Notification Service consumer/config/JSON | 7 passed | Unit/mock-based consumer, serialization, and error-handler tests; no broker-backed verification. |
| Catalog Service unit/MVC | 40 passed | Focused service unit and MVC tests; no runtime HTTP or MySQL integration. |

Catalog Service focused test result: **40 tests passed, 0 failures, 0 errors, 0 skipped**. The reported run used automated unit and MVC tests and did not use MySQL, Redis, or Kafka. The other listed counts are focused results reported from earlier work.

Catalog runtime verification remains pending:

- Runtime application startup has not been successfully confirmed.
- HTTP CRUD smoke tests have not been executed.
- Persistence through actual HTTP requests has not been verified.
- Persistence after an application restart has not been verified.
- The database schema and Flyway history were inspected read-only; versions V1 and V2 were reported as successfully applied, with no pending migrations.
- No smoke-test records were created.

These checks are follow-up verification for the completed implementation phase; they do not establish that the service is fully end-to-end verified or production-ready.

`JwtAuthenticationIntegrationTest` is database-backed and remains unrun because the intended test database and Redis DB have not been independently confirmed as dedicated and isolated. Its test setup deletes all user rows in its selected schema. The Maven test properties naming a test schema and Redis DB 15 do not alone establish resource ownership or safe isolation.

## 6. Known Limitations and Deferred Risks

1. **Deferred:** Database-backed authentication and deleted-user integration tests have not been verified in an approved isolated environment.
2. **Unverified:** The configured Security filter-chain 403 handler lacks direct end-to-end test coverage.
3. **Unresolved:** Redis projection failures after commit can leave Delivery Service state inconsistent; no reconciliation mechanism exists.
4. **Unresolved:** Registration database commits and Kafka publication are not atomic; there is no transactional outbox.
5. **Unresolved:** Kafka recovery has no durable dead-letter queue or replay mechanism.
6. **Unverified:** Cache error-handler tests do not demonstrate real database fallback behavior.
7. **Unresolved:** Cache eviction failures can leave stale cached user data until expiration or another successful eviction.
8. **Unresolved:** Notification event processing has no idempotency tracking, so duplicate delivery may repeat processing.
9. **Unimplemented:** Delivery assignment, Cart, Order, Payment, API Gateway, and email/SMS provider workflows.
10. **Pending verification:** Catalog runtime startup, HTTP CRUD behavior, HTTP-to-MySQL persistence, and restart persistence have not been verified. The schema and Flyway history were inspected read-only; V1 and V2 were reported applied with no pending migrations.
11. **Security limitation:** Catalog write endpoints have no authentication or authorization and must not be publicly exposed.
12. **Unverified:** Local runtime startup and end-to-end behavior across all services are not established by the focused test results.

## 7. Next Feature and Planned Roadmap

The Catalog Service implementation phase is complete and its focused unit/MVC test run reported 40 passing tests. Runtime smoke testing, actual database persistence verification, and authorization remain outstanding. Runtime smoke testing is a follow-up verification task, not a newly numbered phase. The following is a **proposed future sequence**, not a claim about historical phase numbering:

1. **Local runtime and CI foundation, as needed** — Establish repeatable local development and safe automated checks before they are prerequisites for feature work.
2. **Catalog verification and authorization** — Establish isolated database verification; implement an approved access-control boundary before any public deployment.
3. **Cart service** — Build cart workflows on catalog data.
4. **Order lifecycle** — Define and implement order creation and state transitions using catalog/cart capabilities.
5. **Payment service** — Add payment workflows with appropriate security and failure handling.
6. **Delivery assignment and reliability improvements** — Assign partners to orders and address Redis reconciliation and event reliability where required.
7. **API Gateway and notification delivery integration** — Add gateway routing and connect notifications to delivery providers after event handling is designed.
8. **Production delivery and operations** — Add Docker/Kubernetes, observability, CI/CD, and AWS deployment.

The immediate next step is to establish a safe catalog database integration strategy and agree on an authorization design before public exposure. This does not imply the deferred Phase 6/7 risks are resolved or that any service is production-ready.

## 8. Rules for Future Work

- Work incrementally, one feature at a time.
- Inspect existing code and documentation before proposing changes.
- Do not overwrite existing work or make unrelated refactors.
- Do not run database-backed tests until test database and Redis isolation are verified.
- Do not print secrets or expose credentials.
- Do not commit, push, or stage changes without explicit approval.
- Keep known limitations visible until they are deliberately addressed.
- Distinguish implementation, focused unit-test evidence, and integration verification; do not claim end-to-end reliability based on unit tests alone.
