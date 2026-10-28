# Cart Service

Independent cart API for the Food Delivery Platform. The service owns cart data, retrieves authoritative menu-item details from the Catalog Service over HTTP, and does not access another service's database.

## Technology and boundaries

- Java 25, Spring Boot 4.0.7, Spring MVC, Spring Data JPA, MySQL, and Flyway.
- DTO-based API with Jakarta Bean Validation and a consistent error response.
- Spring `RestClient` calls the Catalog Service; the Catalog Service base URL is required configuration.
- No Redis, Kafka, checkout, orders, payments, discounts, delivery charges, or inventory reservation.
- There is no authentication or authorization. A user ID in the route is only a local-development ownership boundary and does not prove the caller owns that user account. Cart-ID operations filter by both cart ID and user ID and return `404` for a different owner. Do not expose this service publicly without an appropriate access-control layer.

## Database and configuration

Create a dedicated MySQL schema named `cart_db` and grant a local service account access before starting the application. Cart Service does not create users, schemas, or grants. Flyway applies the Cart Service migrations to that schema; Hibernate uses `ddl-auto=validate`. It is configured for local port `8084`.

Set the following variables using a local secret/configuration mechanism. The service reads them through the Spring placeholders defined in `src/main/resources/application.properties`:

| Variable | Purpose |
|---|---|
| `DB_URL` | JDBC URL for the dedicated Cart schema, such as `jdbc:mysql://localhost:3306/cart_db` |
| `DB_USERNAME` | Account with access to `cart_db` |
| `DB_PASSWORD` | Password for that account; use a placeholder value in documentation only |
| `CATALOG_SERVICE_BASE_URL` | Base URL of Catalog Service, such as `http://localhost:8083` |

Example:

```powershell
$env:DB_URL='jdbc:mysql://localhost:3306/cart_db'
$env:DB_USERNAME='cart_app'
$env:DB_PASSWORD='<replace-with-cart-db-password>'
$env:CATALOG_SERVICE_BASE_URL='http://localhost:8083'
```

The service has no datasource or Catalog URL fallback; all four values are required. Do not commit credentials.

## Run locally

Prerequisites: JDK 25, reachable MySQL with the pre-created `cart_db` schema, and a reachable Catalog Service. From this directory, after setting the variables:

```powershell
.\mvnw.cmd spring-boot:run
```

Startup and database migration application have not been verified as part of the focused test run.

## API

All paths are versioned under `/api/v1`. The route's `userId` scopes the cart; routes containing `cartId` also verify that the cart belongs to that user. No user ID or price is treated as authenticated or trusted input.

| Method | Path | Behavior |
|---|---|---|
| `PUT` | `/api/v1/users/{userId}/cart` | Create the user's cart if absent, otherwise return the existing cart; returns `200` with the cart and subtotal |
| `GET` | `/api/v1/users/{userId}/cart` | Retrieve the user's existing cart and subtotal; `404` if absent |
| `GET` | `/api/v1/users/{userId}/cart/{cartId}` | Retrieve a cart only if it belongs to `userId` |
| `POST` | `/api/v1/users/{userId}/cart/{cartId}/items/{menuItemId}` | Add quantity; a repeated item increments the existing line; returns the updated cart |
| `PUT` | `/api/v1/users/{userId}/cart/{cartId}/items/{menuItemId}` | Set the line quantity; returns the updated cart |
| `DELETE` | `/api/v1/users/{userId}/cart/{cartId}/items/{menuItemId}` | Remove one line; returns `204` |
| `DELETE` | `/api/v1/users/{userId}/cart/{cartId}/items` | Clear all lines; returns `204` |

Add and quantity-update requests require a JSON positive integer quantity:

```json
{
  "quantity": 2
}
```

Clients cannot submit item names, prices, or totals. For each add, Cart Service calls Catalog Service `GET /api/v1/menu-items/{id}` and validates its item ID, name, non-negative INR price, and availability. A missing Catalog item returns `404`; an unavailable item returns `409`; Catalog connectivity or server errors return `503`; an invalid Catalog response returns `502`. Invalid request bodies or identifiers return `400`.

The name and unit price from the Catalog response are stored as a cart-line snapshot. Re-adding the same item increases its quantity and refreshes its snapshot; changing quantity alone keeps the existing snapshot. The subtotal is recalculated from persisted snapshot prices and quantities using `BigDecimal`. It can differ from the current Catalog price until that item is added again.

## Data model

Flyway migration `V1__create_carts.sql` creates:

- `carts`: one row per user ID, with a unique user constraint and created/updated timestamps.
- `cart_items`: menu-item ID and name, positive quantity, non-negative `DECIMAL(10,2)` unit-price snapshot, INR currency, and timestamps.
- A foreign key from cart items to carts, a unique `(cart_id, menu_item_id)` constraint, and quantity/price checks.

`user_id` and `menu_item_id` refer to other services' data and intentionally have no cross-service foreign keys. Cart Service does not own User or Catalog data.

## Tests and verification

Run the focused test set from this directory:

```powershell
.\mvnw.cmd "-Dtest=CartPersistenceServiceTest,CartServiceImplTest,CartControllerTest,CatalogMenuItemClientTest" test
```

The reported focused run completed with **24 tests, 0 failures, 0 errors, and 0 skipped**. Repository/service tests use Mockito, controller tests use Spring MVC with a mocked service, and Catalog client tests use `MockRestServiceServer`; no real MySQL or Catalog Service is used.

Runtime application startup, Flyway execution against MySQL, HTTP-to-MySQL persistence, restart persistence, and live Catalog connectivity remain unverified. Do not run database-backed tests against a development schema; first establish an isolated disposable test database.
