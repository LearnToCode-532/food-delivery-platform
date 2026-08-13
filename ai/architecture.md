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