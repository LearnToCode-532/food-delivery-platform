# Current Phase

## Phase

**Phase 3 — Redis Caching, Availability & GEO Location Tracking**

## Status

- Phase 1 — Architecture & User Service: **COMPLETE**
- Phase 2 — Authentication & Authorization: **COMPLETE**
- Phase 3 — Redis Caching, Availability & GEO: **IMPLEMENTATION COMPLETE**
- Phase 4 — Kafka Events: **NEXT**

Both User Service and Delivery Service test suites are passing.

Before starting Phase 4, finish documenting Phase 3 decisions and create a clean Git checkpoint.

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

Latest reported status:

- User Service test suite: **PASS**
- Delivery Service test suite: **PASS**
- Redis caching implementation: complete
- Redis availability tracking implementation: complete
- Redis GEO implementation: complete

Both services' test suites have passed after resolving the compilation and GEO-related test issues.

## Currently Working On

### Phase 3 — Finalization

Before beginning Phase 4:

- [ ] Review Redis cache, availability, and GEO design decisions
- [ ] Verify the APIs manually against a running Redis instance
- [ ] Document consistency limitations and failure scenarios
- [ ] Review the complete test results
- [ ] Create a clean Git checkpoint

Do not repeat completed implementation work unless verification reveals a defect.

## Next

### Phase 4 — Kafka Events

Introduce asynchronous communication between services and understand event-driven architecture.

Planned objectives:

- Understand synchronous versus asynchronous communication
- Understand Kafka topics, partitions, offsets, and consumer groups
- Configure Kafka locally
- Introduce event producers and consumers
- Define event naming and payload conventions
- Implement an initial event-driven workflow
- Handle consumer failures and retries
- Understand duplicate delivery and idempotent processing
- Add integration tests for event-driven behavior
- Document delivery guarantees and consistency trade-offs

The first Kafka workflow should be selected based on the current service architecture rather than introducing events without a clear use case.

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

Kafka is planned for Phase 4 and should not be introduced retroactively into the completed Redis implementation.

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
Documentation, verification & Git checkpoint

Phase 4
Kafka Events
        ↓
NEXT

Phase 5
User Profile Management
        ↓
PLANNED

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