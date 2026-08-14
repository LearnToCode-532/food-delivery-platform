# Coding Guidelines

## Java

- Java 25
- Prefer clean, readable code
- Follow SOLID where appropriate
- Avoid unnecessary abstractions
- Prefer constructor injection

## Spring

- Spring Boot 4.0.7
- Use @RestController for REST APIs
- Use DTOs for API contracts
- Do not expose JPA entities directly
- Use @ControllerAdvice for global exception handling
- Use Bean Validation

## Database

- JPA/Hibernate
- Avoid N+1 queries
- Use transactions carefully
- Do not access another service's database

## API

- RESTful endpoints
- Proper HTTP status codes
- Request validation
- Consistent error responses

## Logging

- Use SLF4J
- Do not use System.out.println
- Do not log passwords/tokens/payment secrets

## Configuration

- No hardcoded credentials
- Use application.yml
- Environment-specific configuration
- Secrets must not be committed

## Testing

- JUnit 5
- Mockito
- Spring Boot Test
- Testcontainers for integration tests

## Kafka

- Events must have unique event IDs
- Consumers must be idempotent
- Do not assume exactly-once processing

## Redis

- Define TTL where appropriate
- Redis is not the primary persistent store
- Handle Redis failures gracefully