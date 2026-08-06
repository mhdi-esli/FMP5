# Financial Messaging Platform (FMP5)

A Spring Boot backend service for creating and validating inter-bank financial transfer messages (SWIFT MT200 series). The platform abstracts message format complexity from business systems while ensuring compliance with financial messaging standards.

## Quick Start

```bash
# Build the project
mvn clean package

# Run the application
mvn spring-boot:run

# Run tests
mvn test

# Build without tests
mvn clean package -DskipTests
```

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/messages` | Create a new MT200 financial institution transfer message |
| GET | `/api-docs` | OpenAPI JSON documentation |
| GET | `/swagger-ui.html` | Interactive Swagger UI |

## Project Structure

```
src/main/java/com/bank/messaging/
├── config/           # Spring configuration (Security, Cache, OpenAPI)
├── controller/       # REST controllers (Message, MessageDefinition, Institution)
├── dto/              # Data Transfer Objects (Request/Response models)
├── entity/           # JPA entities (Message, MessageDefinitionMapping, Institution)
├── enums/            # Domain enums (MessageType, Network, Status, ErrorCodes)
├── exception/        # Custom exceptions and handlers
├── repository/       # JPA repositories
├── service/          # Business logic services
└── FinancialMessagingPlatformApplication.java
```

## Technology Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 21 | Runtime environment |
| Spring Boot | 3.3.2 | Application framework |
| PostgreSQL | - | Primary database |
| Flyway | - | Database migrations |
| Caffeine | - | In-memory caching |
| SpringDoc OpenAPI | 2.6.0 | API documentation |
| Testcontainers | 1.20.1 | Integration testing |
| Lombok | - | Code generation |

## Key Concepts

### Message Creation Flow
1. **Controller** receives message creation request
2. **Validation Service** validates required fields (amount, currency, network)
3. **Message Definition Service** checks if message type/network is defined and active
4. **Institution Service** validates sender institution and network compatibility
5. **Message Repository** persists message with status DRAFT or VALIDATION_FAILED

### Error Handling
Messages with validation failures are stored with `VALIDATION_FAILED` status and include detailed Persian error messages:
- `MSG-001`: Missing or invalid required fields
- `MSG-003`: Invalid network selection
- `MSG-004`: Message definition not found
- `MSG-005`: Message definition inactive
- `MSG-010/011/012`: Institution validation errors

## Configuration

### Database
Uses PostgreSQL with Flyway migrations at `src/main/resources/db/migration/`.

### Security
OAuth2 resource server configuration pointing to TPS SSO:
- JWK Set URI: `https://sso.tps.ir/.well-known/openid-configuration/jwks`

### Caching
Caffeine cache configured with:
- Maximum size: 1000 entries
- TTL: 300 seconds

## OpenAPI Documentation
- API Docs: `http://localhost:8080/api-docs`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

## Environment Variables

| Variable | Description |
|----------|-------------|
| `SPRING_DATASOURCE_URL` | PostgreSQL connection string |
| `SPRING_DATASOURCE_USERNAME` | Database username |
| `SPRING_DATASOURCE_PASSWORD` | Database password |

## Current Epic Status

| Epic | Description | Status |
|------|-------------|--------|
| EPIC-01 | Message Creation Service | Complete |
| EPIC-02 | Message Definition Management | Complete |
| EPIC-03 | Institution Management | Complete |

## Development Notes

- All validation errors return Persian messages in the response
- Message IDs follow format: `MSG-YYYYMMDD-NNNNNN` (daily sequential)
- Testcontainers are used for integration tests requiring database
- Enable `spring.jpa.show-sql=true` in dev for SQL logging
