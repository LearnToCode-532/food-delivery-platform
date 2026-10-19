# Current Phase

## Phase

**Phase 5 — User Profile Management**

## Status

- Phase 1 — Architecture & User Service: **COMPLETE**
- Phase 2 — Authentication & Authorization: **COMPLETE**
- Phase 3 — Redis Caching, Availability & GEO: **IMPLEMENTATION COMPLETE; DOCUMENTATION REVIEWED; GIT CHECKPOINT DEFERRED**
- Phase 4 — Kafka Events: **INITIAL REGISTRATION FLOW IMPLEMENTED AND MANUALLY VERIFIED; FULL DATABASE REGRESSION DEFERRED**
- Phase 5 — User Profile Management: **IN PROGRESS; UPDATE/DELETE SERVICE TESTS ADDED**

The Phase 3 documentation review is complete. No Git checkpoint was created because committing is outside the current task.

## Completed

### Phase 1 — Architecture & User Service

- Architecture confirmed
- Repository initialized
- AI coding guidelines created
- Phase documentation created
- Spring Boot 4.0.7 User Service initialized
- Java 25 configured
- Maven build configured
- MySQL configured
- `user_db` database configured
- Spring Boot to MySQL connectivity verified
- Flyway database migrations configured
- User entity implemented
- User repository implemented
- User DTOs implemented
- Request validation implemented
- User service implemented
- BCrypt password hashing implemented
- Duplicate email and phone validation implemented
- Global exception handling implemented
- User registration API implemented
- User retrieval API implemented
- User API manually verified
- User persistence verified
- Tests implemented and verified
- Maven compile, test, and install verified

### Phase 2 — Authentication & Authorization

- Spring Security integrated
- Authentication architecture implemented
- `AuthenticationManager` configured
- `UserDetailsService` implemented
- BCrypt password authentication implemented
- Login API implemented
- JWT generation implemented
- JWT validation implemented
- JWT authentication filter implemented
- Stateless security configuration implemented
- Protected user APIs implemented
- Public registration endpoint configured
- Public login endpoint configured
- Authentication failure handling implemented
- JWT expiration handling implemented
- Authentication flow manually verified
- Protected endpoint access manually verified
- Invalid authentication scenarios tested
- Full test suite verified

### Phase 2 Verification

Latest recorded full test result:

```text
Tests run: 60
Failures: 0
Errors: 0
Skipped: 0

BUILD SUCCESS
```

Phase 2 is complete.

### Phase 3 — Redis Caching, Availability & GEO

#### A. User Service — Redis Caching

- Redis installed and running locally through WSL2
- Spring Data Redis configured
- Redis connection timeouts configured
- Spring Cache abstraction enabled
- Redis cache manager configured
- JSON serialization configured
- Cache TTL configured to 10 minutes
- User response caching implemented
- Cache invalidation implemented for user updates and deletion
- DTO caching used instead of caching JPA entities
- Redis cache error handling implemented
- Database fallback behavior implemented for cache failures
- Cache hit/miss and invalidation tests implemented
- Redis failure behavior tested

#### B. Delivery Service — Partner Availability

- Delivery Service created on port `8082`
- Separate `delivery_db` configured
- Flyway migrations configured
- Delivery partner entity, repository, service, and controller implemented
- Delivery partner availability states implemented:
  - `OFFLINE`
  - `AVAILABLE`
  - `BUSY`
  - `SUSPENDED`
- Redis availability tracking implemented
- Redis availability key: `delivery:partners:available`
- MySQL retained as the durable source of truth
- Redis availability synchronization implemented
- Availability transition validation implemented
- Redis failure handling implemented
- Unit and integration tests implemented

#### C. Delivery Service — Redis GEO Location Tracking

- Location update request DTO implemented
- Latitude and longitude validation implemented
- Nearby partner response DTO implemented
- Redis GEO location service implemented
- Redis GEO key: `delivery:partners:geo`
- Partner location update API implemented
- Nearby partner search API implemented
- Radius-based geographic search implemented
- Only currently available partners returned by nearby search
- Results ordered by distance ascending
- Suspended partner location updates restricted
- GEO location removal implemented during partner deletion
- Redis GEO failure handling implemented
- Unit and integration tests implemented

#### Delivery Service API

Update a partner's location:

```http
PUT /api/v1/delivery-partners/{id}/location
```

Find nearby available partners:

```http
GET /api/v1/delivery-partners/nearby?latitude=25.3176&longitude=82.9739&radiusKm=5
```

Update availability:

```http
PATCH /api/v1/delivery-partners/{id}/availability
```

These endpoints form the foundation for the future delivery assignment workflow.

### Phase 3 Verification

- Redis caching, availability tracking, and GEO implementation are complete.
- Historical Surefire reports recorded 67 User Service tests and 45 Delivery Service tests with no reported failures, but the Delivery report used `delivery_db`. Because Delivery integration tests delete rows, that run is not safe full-regression evidence.
- No Phase 3 database-backed regression suite was run during the current continuation. The Phase 3 implementation status is retained, but this documentation does not treat the historical Delivery run as safe regression verification.
- No new Phase 3 manual verification was performed as part of this continuation.

### Phase 4 — Kafka User Registration Event

- Spring Boot 4.0.7-managed Kafka starter added to the User Service and Notification Service
- Spring Boot JSON starter added to the Notification Service for Jackson 3 Kafka JSON serialization
- Both services accept `KAFKA_BOOTSTRAP_SERVERS` as an override; the current local properties use the WSL2-advertised broker address as the fallback
- `POST /api/v1/users` schedules a `UserRegistered` event after the registration transaction commits
- Event fields: `eventId` (`UUID`), `userId` (`Long`), and `occurredAt` (`Instant`)
- Producer sends asynchronously to `user.events` with the event ID as the Kafka key
- Producer metadata/buffer waits are bounded to five seconds; the request does not wait for the send future
- Notification Service is a separate Maven project and consumes the event using group `notification-service`
- Consumer contract is an independent record with the same JSON fields; it does not depend on User Service classes or its database
- JSON type headers are disabled; consumer deserialization uses an explicit target type and trusts only its event package
- Consumer uses Spring Kafka's default exception classification. Retryable listener failures are attempted three times total (initial delivery plus two retries at one-second intervals), then topic/partition/offset are logged and the record is recovered; no dead-letter topic is configured
- Spring Kafka's default fatal exceptions, including `DeserializationException`, are recovered without retries. The default error handler acknowledges handled records, allowing their offsets to advance
- Consumer processing is not idempotency-tracked. A crash before an offset is committed can cause a record to be delivered and processed again
- No email/SMS provider, transactional outbox, or exactly-once guarantee is implemented

#### Phase 4 Verification — Manual

- **Verified manually on October 19, 2026:** a user was registered through the User Service, the User Service published a `UserRegistered` event, and the Notification Service consumed it.
- Confirmed consumer log: `Received UserRegistered event 8ccdc007-3771-4423-a5cc-82e95112a988 for user 110`
- Kafka 4.0.2 was running locally in WSL2 in KRaft mode.
- **Local networking workaround:** Windows `localhost:9092` resolved to IPv6 while the Notification Service attempted IPv4. The working local setup used the WSL2 IP for Kafka's advertised PLAINTEXT listener and as the Spring Kafka bootstrap server. Do not rely on `localhost` across this Windows/WSL2 boundary in this setup.
- The WSL2 IP can change after a WSL restart. Re-discover the current WSL2 IP and update the local broker advertised listener and `KAFKA_BOOTSTRAP_SERVERS` for both services; do not copy a current IP into shared documentation.

#### Phase 4 Verification — Automated

- User Service focused registration and producer tests: **15 passed, 0 failures, 0 errors, 0 skipped**. Command, from `services/user-service`: `.\mvnw.cmd clean '-Dtest=UserServiceImplTest,UserRegisteredEventPublisherTest' test`
- Notification Service suite: **7 passed**. Command, from `services/notification-service`: `..\user-service\mvnw.cmd test`
- The focused tests cover scheduling only after successful persistence, after-commit publication, rollback suppression, the three-field event contract, synchronous and asynchronous producer send failures, valid/invalid consumer events, JSON serialization/deserialization, malformed JSON capture, retry count, and recovery logging.
- The focused consumer retry test verifies three total attempts for an ordinary retryable processing exception (initial delivery plus two retries), with two one-second backoffs.
- The focused deserialization recovery test verifies that a `DeserializationException` is treated by Spring Kafka's default fatal classification: recovery occurs without retries and the handled record is eligible for offset commit.
- On October 19, 2026, the first focused User Service invocation without `clean` failed with six class-format/verifier errors in pre-existing `target` bytecode. The clean rebuild above then passed all 15 tests. The failed attempt did not start Spring contexts or access external infrastructure.

#### Test Isolation Guardrails and Current Verification

- User and Delivery Service Maven Surefire configurations pin test datasource URLs to `food_delivery_user_test` and `food_delivery_delivery_test`, respectively. Both receive credentials only from `FDP_TEST_DB_USERNAME` and `FDP_TEST_DB_PASSWORD`; no credential values are stored in the repository.
- User Service tests are pinned to Redis DB 15 and a loopback Kafka address on port 1, so test-triggered registration events cannot reach the configured shared broker. Delivery Service tests are pinned to Redis DB 15.
- These are test-runner properties only. The runtime datasource defaults and service configuration have not been changed for test isolation.
- User's Flyway migration and Hibernate `ddl-auto=update` can modify the selected User schema at context startup. Delivery's Flyway migration can modify its selected schema; Hibernate uses `ddl-auto=validate`. The Surefire datasource pins ensure the schemas selected by ordinary Maven test runs are the dedicated test schemas, not `user_db` or `delivery_db`.
- Delivery integration tests delete repository rows and both services' Redis tests clear keys. Both services' integration suites therefore still require Redis DB 15 to be confirmed reserved for testing immediately before execution. An empty Redis DB alone does not establish exclusive isolation.
- Safe checks run on October 19, 2026:
  - User Service focused tests: **15 tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS** (command above).
  - Notification Service: **7 tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS** (`..\user-service\mvnw.cmd test`, from `services/notification-service`).
  - Delivery Service: `.\mvnw.cmd -DskipTests test-compile` completed with **BUILD SUCCESS** and compiled all five test source files; no tests were executed.
- In the October 19, 2026 continuation, the User focused tests were rerun after a clean build: **15 tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS**. Command, from `services/user-service`: `.\mvnw.cmd clean '-Dtest=UserServiceImplTest,UserRegisteredEventPublisherTest' test`.
- In the same continuation, Notification tests were rerun: **7 tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS**. Command, from `services/notification-service`: `..\user-service\mvnw.cmd test`.
- Delivery test compilation was rerun with `.\mvnw.cmd -DskipTests test-compile` from `services/delivery-service`: **BUILD SUCCESS**; all five test sources were already up to date. No Delivery tests were executed.
- By user decision, no full database-backed User or Delivery suite was run in this continuation. Their full regression status remains unverified; the Delivery report shown below used `delivery_db` and is not safe regression evidence.

#### Latest Full-Service Surefire Reports — October 19, 2026

- User Service: **67 tests, 0 failures, 0 errors, 0 skipped**. Surefire reports show the integration-test datasource was `food_delivery_user_test`.
- Delivery Service: **45 tests, 0 failures, 0 errors, 0 skipped**. Surefire reports show the integration-test datasource was `delivery_db`, not an isolated test schema. Since integration tests call `repository.deleteAll()`, this result is not accepted as a safe regression verification and may have affected development data.
- Notification Service: **7 tests, 0 failures, 0 errors, 0 skipped**.
- The Surefire report files verify the counts and database URL logged during the runs, but do not preserve the exact Maven command lines. Do not infer or record an invocation as exact without its console transcript.
- These reports predate this documentation checkpoint. No full suites were run as part of this checkpoint.
- No broker-backed automated integration test was run; the end-to-end Kafka path above was verified manually.

#### Full Database Regression — Deferred

- User-requested scope for this continuation excludes further database/Redis isolation setup and full database-backed regression runs. These tests remain unverified, not failed or passed.
- If a future regression run is requested, use the Surefire test-only datasource pins and configure the restricted test account via `FDP_TEST_DB_USERNAME` and `FDP_TEST_DB_PASSWORD`. Confirm Redis DB 15 is reserved exclusively for the tests before execution; do not flush it to make it empty.
- No database-backed tests were run in this continuation, so `user_db` and `delivery_db` were not selected by a test process.

## Currently Working On

### Phase 5 — User Profile Management

- [x] Existing profile update and delete operations retained: `PUT /api/v1/users/{userId}` and `DELETE /api/v1/users/{userId}`
- [x] Add focused service unit tests for successful profile update, preservation of account fields, and missing-user behavior for update and delete
- [ ] Define any additional Phase 5 acceptance criteria before expanding beyond the documented update/delete scope

The implementation's current delete operation physically removes the user row. The roadmap does not specify soft deletion, profile ownership rules, or duplicate-phone behavior during updates; these semantics should be agreed before changing them.

#### Phase 5 Verification — Automated

- User Service profile service tests, command from `services/user-service`: `.\mvnw.cmd '-Dtest=UserServiceImplTest' test`
- Result on October 19, 2026: **14 tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS**. These are Mockito unit tests and do not connect to MySQL, Redis, or Kafka.
- Full User and Delivery Service database-backed suites remain intentionally deferred. This focused result is not full-suite regression evidence.

## Phase 4 Follow-up — Deferred

The initial registration workflow is implemented and manually verified. Full User and Delivery database-backed regression was explicitly deferred; no full suite is claimed as passed. Optional reliability follow-ups are:

- Add broker-backed integration tests when a safe broker test environment is available
- Study partitions, offsets, consumer groups, duplicate delivery, idempotent processing, and consumer restart behavior
- Consider a transactional outbox and operational dead-letter handling in a later reliability milestone

These follow-ups are not prerequisites to the documented Phase 5 update/delete scope.

## Phase 3 Documentation and Git Checkpoint

- [x] Review and reconcile Phase 3 implementation and verification notes with the available test evidence
- [ ] Create a Git checkpoint — deferred; no commit was created

## Future Delivery Assignment Workflow

Delivery assignment is planned for a later milestone, after the required foundations are understood.

Expected behavior:

1. Receive a request to find a delivery partner for an order.
2. Find nearby available partners using Redis GEO.
3. Process candidates in ascending distance order.
4. Offer the delivery to the nearest eligible partner.
5. Wait up to 10 seconds for acceptance.
6. If accepted, assign the partner exactly once.
7. If rejected or timed out, try the next eligible partner.
8. Handle concurrent assignment attempts, retries, and service failures.

Before implementation, design the assignment state machine and define ownership of assignment state.

Potential future concerns include Kafka events, idempotency, atomic state transitions, transactional outbox, recovery after failures, and reconciliation between MySQL and Redis.

Do not implement the complete assignment workflow during Phase 3.

## Known Improvements — Later

### Authentication and Security

- Replace broad JWT exception handling with more specific exceptions
- Improve distinction between expired and invalid JWTs
- Move authentication exception handling into the global exception handling strategy
- Review Actuator endpoint exposure
- Consider caching `UserDetails` where appropriate
- Move JWT configuration toward strongly typed configuration
- Introduce production-grade secret management
- Implement refresh tokens
- Introduce RBAC and authorities

### Redis and Distributed Consistency

- Review stale availability entries and recovery procedures
- Add reconciliation between Redis operational data and MySQL
- Consider atomic server-side availability filtering if search volume requires it
- Improve resilience of Redis/MySQL synchronization
- Evaluate transactional outbox and event-driven synchronization
- Define location expiration and stale-location policies

### Kafka and Event Reliability

- Add a transactional outbox so committed user registrations can be relayed to Kafka after process restarts
- Define idempotent consumer behavior before adding notification side effects
- Evaluate bounded retry and dead-letter topic policies for operational recovery
- Add broker-backed integration tests and document duplicate/redelivery handling

These improvements should be addressed when relevant to the current phase, not prematurely.

## Security Configuration

JWT secrets must never be committed to Git.

Example configuration:

```properties
jwt.secret-key=${JWT_SECRET_KEY}
jwt.expiration-ms=3600000
```

Production secret management will be addressed in a later phase.

Database credentials must also be managed through environment variables or an appropriate local secret mechanism.

## Do Not Implement Yet

Unless explicitly introduced by the current phase, defer:

- Complete delivery assignment workflow
- Payment workflow
- Kubernetes deployment
- Distributed tracing
- Prometheus and Grafana
- OpenTelemetry
- Advanced saga orchestration
- Transactional outbox implementation
- Production-scale distributed transactions

The initial Kafka registration event flow is implemented in Phase 4. Reliability follow-ups remain deferred and should be handled in an appropriate later milestone.

## Phase Order

```text
Phase 1
Architecture & User Service
        ↓
COMPLETE

Phase 2
Authentication & Authorization
        ↓
COMPLETE

Phase 3
Redis Caching, Availability & GEO
        ↓
IMPLEMENTATION COMPLETE
        ↓
Documentation reviewed
Git checkpoint deferred

Phase 4
Kafka Events
        ↓
Initial flow implemented
Full DB regression deferred

Phase 5
User Profile Management
        ↓
IN PROGRESS

Phase 6
Advanced Features
        ↓
PLANNED

Later milestone
Delivery Assignment Workflow
        ↓
State machine, partner selection,
acceptance timeout and reliable assignment
```

The roadmap can be refined as the implementation develops, but completed phases should remain documented accurately.

## Development Principles

Continue prioritizing:

- Understanding over code volume
- Architecture over CRUD
- Debugging over copy-pasting
- Trade-offs over definitions
- Production problems over tutorial examples
- Interview readiness over feature count
- Incremental implementation over premature complexity
- Reliable tests and verification over unverified completion claims

The project should advance phase-by-phase. Reuse existing code and architecture, avoid unnecessary rewrites, and do not claim that a feature is verified unless the relevant tests or manual checks have actually passed.