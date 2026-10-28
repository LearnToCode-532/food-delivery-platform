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
- Cart Service (user cart data and price snapshots; independent deployable and owned database)
- Delivery Service
- Notification Service

Planned:

- API Gateway
- Order Service
- Payment Service

## Data Ownership

Each service owns its database.

No service directly accesses another service's database.

Current implementation note: the Catalog Service is implemented as a single deployable for the Restaurant and Menu domains. The Cart Service consumes catalog APIs over HTTP; future services should use service APIs rather than directly accessing another service's database.

## Catalog Service

The Catalog Service is an independent Spring Boot application at `services/catalog-service`, configured for local port `8083`. It owns restaurant and menu-item catalog data in its MySQL `catalog_db` schema. Flyway migrations manage the schema, and Hibernate validates it.

The service is independent of the User, Delivery, and Notification services and has no outbound integrations. The Cart Service consumes its menu-item API over HTTP. Redis and Kafka are not part of the Catalog Service implementation. Authentication and authorization are not implemented; it is intended for trusted local development only and must not be exposed publicly until appropriate access control is in place.

## Cart Service

The Cart Service is an independent Spring Boot application at `services/cart-service`, configured for local port `8084`. It owns the `cart_db` MySQL schema and manages one cart per user, cart lines, and persisted unit-price snapshots. Flyway manages its schema and Hibernate validates it.

The API is scoped under `/api/v1/users/{userId}` and each cart-ID operation verifies that the cart belongs to that user ID. User IDs are references to data managed by the User Service; there is no cross-service database foreign key, user lookup, or authentication integration. This is a trusted local-development boundary, not production authorization.

When a menu item is added, Cart Service calls the Catalog Service over HTTP using `CATALOG_SERVICE_BASE_URL` and `GET /api/v1/menu-items/{id}`. It stores the returned item name, current INR price, and quantity in its own database; repeated adds increment quantity and refresh the snapshot. Subtotals are calculated from persisted line snapshots. Cart Service does not access Catalog tables or use Redis/Kafka, and it does not implement checkout, orders, payments, discounts, delivery charges, or inventory reservation.

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

JWT-based authentication is implemented for the User Service. Authentication and authorization are not implemented for the Catalog or Cart Services; both are limited to trusted local development.

## Observability

- Spring Boot Actuator
- Prometheus
- Grafana
- OpenTelemetry
- Jaeger/Tempo

## Deployment

- Docker
- Kubernetes