# Catalog Service

Restaurant and menu catalog API for the Food Delivery Platform. The service owns its catalog data and runs independently of User, Delivery, and Notification services.

## Technology

- Java 25, Spring Boot 4.0.7, Spring MVC
- Spring Data JPA, MySQL, Flyway
- Jakarta Bean Validation
- JUnit 5, Mockito, and Spring MVC slice tests

No Redis, Kafka, or inter-service database access is used. Restaurant and menu writes have no authentication or authorization. This service is for trusted local development only and must not be exposed publicly until authorization is implemented.

## Database prerequisites and configuration

Create and grant access to a dedicated MySQL schema named `catalog_db` outside the service. The service does not create database users, schemas, grants, or credentials. Flyway creates the tables in that existing schema on startup.

Provide these environment variables using a local secret mechanism. The application reads them through the Spring placeholders in `src/main/resources/application.properties`:

| Variable | Purpose |
|---|---|
| `DB_URL` | JDBC URL pointing to the dedicated catalog schema, for example `jdbc:mysql://localhost:3306/catalog_db` |
| `DB_USERNAME` | Database account with appropriate catalog-schema permissions |
| `DB_PASSWORD` | Password for that account; use a placeholder value in documentation only |

Example:

```powershell
$env:DB_URL='jdbc:mysql://localhost:3306/catalog_db'
$env:DB_USERNAME='catalog_app'
$env:DB_PASSWORD='<replace-with-catalog-db-password>'
```

Hibernate uses `ddl-auto=validate`; Flyway is the schema source of truth. The service is configured for port `8083`, following the existing User Service (`8081`) and Delivery Service (`8082`) configuration. Runtime availability of the port has not been verified.

`application.properties` requires the `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` environment placeholders; it does not provide documented literal credentials or fallback defaults. Supply the values through a local secret mechanism and do not commit them. Reviewing and establishing suitable secret management remains a follow-up before deployment.

## Run locally

From this directory, with JDK 25 and the database configured:

```powershell
.\mvnw.cmd spring-boot:run
```

This command is documented for local use but was not executed as part of implementation verification.

## API summary

All endpoints use JSON except responses with no body.

| Method | Path | Behavior |
|---|---|---|
| `POST` | `/api/v1/restaurants` | Create restaurant; returns `201` and `Location` |
| `GET` | `/api/v1/restaurants/{id}` | Retrieve restaurant, including activation state |
| `GET` | `/api/v1/restaurants?page=0&size=20&sortBy=name&direction=asc` | Paginated restaurants; size 1–100; inactive restaurants are included and expose `active` |
| `PUT` | `/api/v1/restaurants/{id}` | Update restaurant details |
| `PATCH` | `/api/v1/restaurants/{id}/activation` | Set `{"active":true}` or `{"active":false}` |
| `POST` | `/api/v1/restaurants/{restaurantId}/menu-items` | Add menu item; returns `201` and `Location` |
| `GET` | `/api/v1/restaurants/{restaurantId}/menu-items?page=0&size=20` | List available, non-deleted items only for an active restaurant; inactive restaurant returns an empty page |
| `GET` | `/api/v1/menu-items/{id}` | Retrieve a non-deleted item, including its availability state |
| `PUT` | `/api/v1/menu-items/{id}` | Update item name, description, and price without changing its restaurant |
| `PATCH` | `/api/v1/menu-items/{id}/availability` | Set `{"available":true}` or `{"available":false}` |
| `DELETE` | `/api/v1/menu-items/{id}` | Soft-delete item; repeated deletion returns `404` |

Restaurant activation is a catalog listing flag, not real-time order acceptance. Menu price is denominated in INR; currency conversion and multi-currency prices are not supported. Menu list results filter unavailable and soft-deleted items. A direct item lookup returns any non-deleted item, including an unavailable item; soft-deleted items are hidden from all normal reads.

### Example requests

Create a restaurant:

```json
{
  "name": "Example Kitchen",
  "description": "Fresh local meals",
  "address": "Main Street",
  "phoneNumber": "+911234567890"
}
```

Add a menu item:

```json
{
  "name": "Vegetable soup",
  "description": "Seasonal vegetables",
  "price": 125.00
}
```

## Tests

The database-independent unit and MVC test command from this directory is:

```powershell
.\mvnw.cmd "-Dtest=RestaurantServiceTest,MenuItemServiceTest,RestaurantControllerTest,MenuItemControllerTest" test
```

The focused tests mock repositories or service dependencies and do not require MySQL, Redis, or Kafka. The reported focused run completed with **40 tests passed, 0 failures, 0 errors, and 0 skipped**. This does not verify runtime startup, database migrations, HTTP behavior, or MySQL persistence.

Database integration tests for migrations, constraints, persistence, pagination, and soft-delete filtering are pending. No Testcontainers dependency or safe isolated catalog database is currently configured. Do not run database-backed tests until an isolated disposable database strategy has been established.

## Verification status

- Automated unit/MVC tests: 40 passed in the reported focused run.
- Runtime application startup and HTTP smoke testing: pending.
- Actual HTTP-to-MySQL persistence: pending.
- Persistence after application restart: pending.
- The database schema and Flyway history were inspected read-only; V1 and V2 were reported successfully applied, with no pending migrations. No smoke-test records were created.

## Known limitations

- There is no authentication or authorization; write endpoints must remain limited to trusted local use.
- Restaurant activation does not represent operating hours or accepting-orders status.
- No currency conversion, menu categories, inventory, image storage, ordering, cart, payment, Redis, Kafka, search service, or external integrations are implemented.
- Database integration behavior has not been verified.
