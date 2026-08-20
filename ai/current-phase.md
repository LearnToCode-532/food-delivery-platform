# Current Phase

## Phase

Phase 3 — Redis Caching

## Status

Phase 2 — Authentication & Authorization is **COMPLETE**.

Phase 3 is the **current phase**.

## Completed

### Phase 1 — Architecture & User Service

* Architecture confirmed
* Repository initialized
* AI coding guidelines created
* Phase documentation created
* Spring Boot 4.0.7 User Service initialized
* Java 25 configured
* Maven build configured
* MySQL configured
* `user_db` database configured
* Spring Boot → MySQL connectivity verified
* Flyway database migration configured
* User entity implemented
* User repository implemented
* User DTOs implemented
* Request validation implemented
* User service implemented
* BCrypt password hashing implemented
* Duplicate email/phone validation implemented
* Global exception handling implemented
* User registration API implemented
* User retrieval API implemented
* User API manually verified
* User persistence verified
* Tests implemented and verified
* Maven compile/test/install verified

### Phase 2 — Authentication & Authorization

* Spring Security integrated
* Authentication architecture implemented
* `AuthenticationManager` configured
* `UserDetailsService` implemented
* BCrypt password authentication implemented
* Login API implemented
* JWT generation implemented
* JWT validation implemented
* JWT authentication filter implemented
* Stateless security configuration implemented
* Protected user APIs implemented
* Public registration endpoint configured
* Public login endpoint configured
* Authentication failure handling implemented
* JWT expiration handling implemented
* Authentication flow manually verified
* Protected endpoint access manually verified
* Invalid authentication scenarios tested
* Full test suite verified

### Phase 2 Verification

Latest full Maven test result:

```text
Tests run: 60
Failures: 0
Errors: 0
Skipped: 0

BUILD SUCCESS
```

Phase 2 is considered complete.

## Currently Working On

### Phase 3 — Redis Caching

Current objective:

> Introduce Redis caching into the User Service while understanding the architecture, cache behavior, consistency implications, and failure scenarios.

Current focus:

* Introduce Redis
* Configure Redis locally
* Understand Spring Data Redis
* Understand cache-aside pattern
* Identify appropriate user data to cache
* Implement cache reads
* Implement cache writes
* Implement cache invalidation
* Configure TTL
* Configure Redis serialization
* Test cache hit/miss behavior
* Test cache invalidation
* Test Redis failure scenarios
* Understand Redis vs MySQL responsibilities

## Next

After the Redis implementation is understood and verified:

* Complete Phase 3 testing
* Verify cache behavior
* Document Redis decisions and trade-offs
* Create a clean Git checkpoint
* Begin Phase 4 — Kafka Events

## Known Improvements — Later

The following improvements are intentionally deferred and are not blockers for the completed authentication phase:

* Replace broad JWT exception handling with more specific exceptions
* Improve distinction between expired and invalid JWTs
* Move authentication exception handling into the global exception handling strategy
* Review actuator endpoint exposure
* Consider caching `UserDetails` where appropriate
* Move JWT configuration toward strongly typed configuration
* Introduce production-grade secret management
* Implement refresh tokens
* Introduce RBAC / authorities

These should not distract from the current Redis phase unless they become necessary for the current implementation.

## Security Configuration

JWT secrets must not be committed to Git.

Example configuration:

```properties
jwt.secret-key=${JWT_SECRET_KEY}
jwt.expiration-ms=3600000
```

Production secret management will be addressed in a later phase.

## Do Not Implement Yet

The following technologies and patterns are intentionally deferred:

* Kafka
* Kubernetes
* Payment workflow
* Delivery assignment
* Distributed tracing
* Prometheus
* Grafana
* OpenTelemetry
* Advanced microservice patterns
* Distributed transactions
* Saga
* Transactional Outbox

They should only be introduced in their respective phases.

## Phase Order

```text
Phase 1
Architecture & User Service
        ↓
✅ COMPLETE

Phase 2
Authentication & Authorization
        ↓
✅ COMPLETE

Phase 3
Redis Caching
        ↓
🔄 CURRENT

Phase 4
Kafka Events
        ↓
⏳ PLANNED

Phase 5
User Profile Management
        ↓
⏳ PLANNED

Phase 6
Advanced Features
        ↓
⏳ PLANNED
```

## Development Principles

Continue prioritizing:

* Understanding over code volume
* Architecture over CRUD
* Debugging over copy-pasting
* Trade-offs over definitions
* Production problems over tutorial examples
* Interview readiness over feature count

The project should advance phase-by-phase. Do not prematurely introduce technologies from future phases.