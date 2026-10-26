# Food Delivery Platform Architecture

## Architecture Style

Microservices

## Communication

Synchronous:
- REST

Asynchronous:
- Kafka

## Services

Currently implemented:

- User Service
- Catalog Service (restaurant and menu-item catalog data; one deployable and one owned database)
- Delivery Service
- Notification Service

Planned:

- API Gateway
- Cart Service
- Order Service
- Payment Service

## Data Ownership

Each service owns its database.

No service directly accesses another service's database.

Current implementation note: the Catalog Service is implemented as a single deployable for the Restaurant and Menu domains. The future Cart and Order services should consume catalog APIs rather than directly accessing the catalog database.

## Catalog Service

The Catalog Service is an independent Spring Boot application at `services/catalog-service`, configured for local port `8083`. It owns restaurant and menu-item catalog data in its MySQL `catalog_db` schema. Flyway migrations manage the schema, and Hibernate validates it.

The service is independent of the existing User, Delivery, and Notification services. No inter-service integrations are implemented for the Catalog Service; Redis and Kafka are not part of its current implementation. Authentication and authorization are also not implemented. It is intended for trusted local development only and must not be exposed publicly until appropriate access control is in place.

## Redis

Redis is used for:
- Caching
- GEO location
- Temporary state

## Kafka

Kafka is used for:
- Order events
- Payment events
- Restaurant events
- Delivery events
- Notifications

Initial event flow:
- The User Service publishes a `UserRegistered` event to `user.events` after the user database transaction commits.
- The Notification Service consumes `user.events` independently and does not access the User Service database.
- The event contract contains only `eventId`, `userId`, and `occurredAt`.
- The initial producer is asynchronous and does not use a transactional outbox; database commit and Kafka publication are not atomic.

## Security

JWT-based authentication is implemented for the User Service. Authentication and authorization are not implemented for the Catalog Service, which is limited to trusted local development.

## Observability

- Spring Boot Actuator
- Prometheus
- Grafana
- OpenTelemetry
- Jaeger/Tempo

## Deployment

- Docker
- Kubernetes