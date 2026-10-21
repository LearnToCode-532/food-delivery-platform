# Food Delivery Platform

A Java microservices learning project for exploring user management, delivery-partner operations, persistence, Redis, and Kafka-based events. The repository currently contains User, Delivery, and Notification services; broader platform components are not implemented here.

## Project status

- **Phase 4:** The initial user-registration event flow is implemented and was manually verified. Full database-backed regression testing has not been completed.
- **Phase 5:** Profile update, ownership checks, soft deletion, and transaction-aware cache invalidation are implemented. Focused unit and controller/security tests are in place; see [`ai/current-phase.md`](ai/current-phase.md) for current verification results and test-isolation limits.

See [`ai/current-phase.md`](ai/current-phase.md) for the detailed phase status and recorded verification history.

## Services and implemented functionality

### User Service

The User Service provides user registration and retrieval, profile update and deletion, and login. It uses Spring Data JPA with MySQL, validates requests, hashes passwords with BCrypt, and implements JWT authentication.

Profile changes use the authenticated principal's identity; clients cannot update or delete another account by supplying its ID. Email is trimmed and lowercased consistently at registration, profile update, and login. Deletion is soft and preserves unique email/phone ownership, so deleted accounts cannot re-register with those identifiers. Changing email invalidates tokens containing the previous email subject, so the user must log in again.

#### Profile API

Update the authenticated user's profile:

```http
PUT /api/v1/users/{userId}
Authorization: Bearer <JWT>
Content-Type: application/json
```

```json
{
  "email": "john@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "phoneNumber": "9876543210"
}
```

Returns `200 OK` with the user response DTO. Email is trimmed and lowercased; phone numbers retain the existing ten-digit format. Invalid input returns `400`, missing/invalid authentication returns `401`, a non-owner returns `403`, an absent/deleted user returns `404`, and duplicate email/phone returns `409`.

Delete the authenticated user's profile:

```http
DELETE /api/v1/users/{userId}
Authorization: Bearer <JWT>
```

Returns `204 No Content`. Deletion sets the account status to `DELETED` rather than removing the row; the account's email and phone remain reserved, and the account cannot log in or use its existing JWT on subsequent requests. No deletion event is published.
Because the old token is rejected after the first deletion, retrying the HTTP request with that token returns `401`; the service-level delete operation itself is idempotent.

Missing or invalid authentication returns `401`, a non-owner returns `403`, and an unknown user ID returns `404`.

The current security configuration makes registration and login public and requires authentication for user endpoints. Profile updates and deletion verify that the authenticated JWT principal owns the requested account. The authenticated read-by-ID endpoint remains ID-based and does not restrict reads to the caller's own account.

### Delivery Service

The Delivery Service provides these operations under `/api/v1/delivery-partners`:

- Create a delivery partner, retrieve one by ID or list all partners, and delete a partner.
- Change a partner's availability.
- Update a partner's location.
- Search for nearby partners by latitude, longitude, and radius.

Partner records and status are persisted in MySQL. Redis stores the available-partner set and location data using GEO operations. Nearby search uses Redis GEO and filters results by availability. Availability changes update MySQL and Redis; deletion removes the partner from Redis location and availability data before deleting the database record. The two stores are not one atomic transaction, so consistency and recovery remain areas for improvement.

Delivery assignment and order workflows are not implemented.

### Notification Service and registration events

After a user registration transaction commits, `UserRegisteredEventPublisher` creates a `UserRegistered` event containing an event ID (`UUID`), user ID, and occurrence time, and asynchronously sends it to the `user.events` Kafka topic using the event ID as the message key. The Notification Service consumes that event with the `notification-service` consumer group, validates the required fields, and currently logs the event; email or SMS delivery is not implemented.

Both services configure the topic as `user.events`. Kafka bootstrap servers can be set with `KAFKA_BOOTSTRAP_SERVERS`; the checked-in fallback address is environment-specific and may not be reachable in another setup. The Notification Service consumer group can be overridden with `KAFKA_CONSUMER_GROUP_ID`.

`KafkaConsumerConfiguration` uses Spring Kafka's `DefaultErrorHandler` with two retries at one-second intervals for retryable processing failures (three attempts total, including the initial delivery). After retries, it logs the record's topic, partition, and offset and recovers the record; no dead-letter topic is configured. Spring Kafka's default fatal deserialization exceptions are recovered without retries.

Publishing after the database commit does not make the database commit and Kafka send atomic. A process failure can therefore leave a committed user without a published event. Consumer idempotency and exactly-once processing are not implemented; a redelivered event may be processed more than once.

## Technology stack

The service POMs currently configure:

- Java 25 and Spring Boot 4.0.7, with an independent Maven build and wrapper for each service.
- MySQL with Spring Data JPA and Flyway migrations in the User and Delivery services.
- Redis for User Service caching and Delivery Service availability/location operations.
- Apache Kafka for user-registration events between the User and Notification services.
- Spring Security and JWT in the User Service (`jjwt` 0.12.5).
- Spring MVC and request validation for HTTP APIs, plus Spring Boot Actuator in the User Service.

Kubernetes, CI/CD, cloud deployment, Prometheus, Grafana, and OpenTelemetry are not represented as integrated components in the current service code and are future learning work.

## Getting started

Install JDK 25. Start and configure only the infrastructure required by the service you intend to run:

- **User Service:** MySQL, Redis at the configured local address, and Kafka for registration-event publishing. Configure `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET_KEY`, and, when needed, `KAFKA_BOOTSTRAP_SERVERS`.
- **Delivery Service:** MySQL and Redis at the configured local address. Configure `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`.
- **Notification Service:** a reachable Kafka broker. Configure `KAFKA_BOOTSTRAP_SERVERS` when the configured fallback is not reachable; optionally set `KAFKA_CONSUMER_GROUP_ID`.

Use locally managed environment variables or another appropriate local secret mechanism for credentials and JWT secrets. Do not commit secrets. Check each service's `src/main/resources/application.properties` and the current phase notes for its effective configuration. The User and Delivery services listen on ports 8081 and 8082, respectively.

Run each service from its own directory in PowerShell:

```powershell
Set-Location D:\food-delivery-platform\services\user-service
.\mvnw.cmd spring-boot:run
```

For the Delivery Service:

```powershell
Set-Location D:\food-delivery-platform\services\delivery-service
.\mvnw.cmd spring-boot:run
```

For the Notification Service:

```powershell
Set-Location D:\food-delivery-platform\services\notification-service
.\mvnw.cmd spring-boot:run
```

## Testing and verification

The following results are recorded in [`ai/current-phase.md`](ai/current-phase.md); they are scoped results, not evidence that every service regression suite passes:

- **User Service isolated regression tests:** the current profile, account-status/login, auth-service, and registration-event tests passed 41 tests with no failures or errors. The controller/security tests compiled but were not executed because test database and Redis isolation could not be verified. A separate test selection including `JwtServiceTest` failed during Spring context startup because test MySQL credentials were unavailable; details are in [`ai/current-phase.md`](ai/current-phase.md).
- **Notification Service tests:** the focused service test run is recorded as 7 tests passed. This covers consumer, event JSON, and Kafka error-handler behavior; it is not a broker-backed integration run.
- **Delivery Service test compilation:** `.\mvnw.cmd -DskipTests test-compile` compiled the five test source files successfully. Tests were not executed by this command.
- **Full database-backed User and Delivery regression:** deferred and not verified. A previous Delivery report used `delivery_db`, so it is not accepted as safe regression evidence.

The Maven Surefire configurations set test datasource URLs to `food_delivery_user_test` and `food_delivery_delivery_test`, expect credentials in `FDP_TEST_DB_USERNAME` and `FDP_TEST_DB_PASSWORD`, and configure Redis DB 15. These settings do not guarantee that the actual database or Redis instance is isolated, reserved, or safe to modify. Before running any database- or Redis-backed tests, verify the selected resources are dedicated to testing and reserved for that purpose. Some integration tests delete database rows or clear Redis keys. **Never run cleanup-capable tests against development databases or shared Redis data.**

Focused test commands, run from the indicated service directory:

```powershell
# User Service unit test
.\mvnw.cmd -Dtest=UserServiceImplTest test

# Focused User Service unit and event-publisher tests
.\mvnw.cmd clean '-Dtest=UserServiceImplTest,UserRegisteredEventPublisherTest' test

# Notification Service tests
.\mvnw.cmd test

# Compile Delivery Service test sources without executing tests
.\mvnw.cmd -DskipTests test-compile
```

The User and Notification focused test commands do not replace full database-backed integration testing. Confirm infrastructure and test-resource isolation before running broader suites.

## Known limitations and future work

- No transactional outbox: a committed user registration can be missing its Kafka event if publishing fails.
- No idempotent event processing, dead-letter topic, or exactly-once guarantee.
- Automated broker-backed integration tests and full safe database-backed User/Delivery regression remain deferred.
- Authenticated read-by-ID remains unrestricted by ownership; profile updates and deletion enforce ownership.
- Cache eviction is transaction-aware, but the existing Redis error handler suppresses eviction failures; a Redis outage can leave stale profile data until the cache TTL expires.
- Delivery assignment, order, payment, and notification-provider workflows are not implemented.
- Redis/MySQL consistency and recovery need further work.
- Kubernetes, CI/CD, production deployment, and expanded observability are planned rather than completed.

## Project documentation

- [`ai/architecture.md`](ai/architecture.md) — architecture and service responsibilities.
- [`ai/current-phase.md`](ai/current-phase.md) — phase status, verification history, and deferred work.
- [`ai/coding-guidelines.md`](ai/coding-guidelines.md) — project coding guidelines.
