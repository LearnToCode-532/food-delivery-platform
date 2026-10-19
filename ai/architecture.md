# Food Delivery Platform Architecture

## Architecture Style

Microservices

## Communication

Synchronous:
- REST

Asynchronous:
- Kafka

## Services

- API Gateway
- User Service
- Restaurant Service
- Menu Service
- Cart Service
- Order Service
- Payment Service
- Delivery Service
- Notification Service

## Data Ownership

Each service owns its database.

No service directly accesses another service's database.

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

JWT-based authentication.

## Observability

- Spring Boot Actuator
- Prometheus
- Grafana
- OpenTelemetry
- Jaeger/Tempo

## Deployment

- Docker
- Kubernetes