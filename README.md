# Financial Messaging Platform (FMP5)

A Spring Boot backend service for creating and validating inter-bank financial transfer messages (SWIFT MT200 series). The platform abstracts message format complexity from business systems while ensuring compliance with financial messaging standards.

## Features

- **Message Creation**: Create standardized financial transfer messages (MT200 series)
- **Multi-Network Support**: SWIFT and SEPA networks with extensible design
- **Comprehensive Validation**: All business validation rules in one place
- **Persian Error Messages**: Localized error codes and messages (MSG-000 to MSG-007)
- **Audit Trail**: All message creation attempts logged with correlation IDs
- **OpenAPI Documentation**: Interactive Swagger UI for API exploration

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
| Lombok | - | Boilerplate reduction |

## Prerequisites

- Java 21+
- Maven 3.9+
- Docker & Docker Compose (recommended for local development)

## Quick Start (Docker - Recommended)

### 1. Clone the repository

```bash
git clone https://github.com/mhdi-esli/FMP5.git
cd FMP5
```

### 2. Start services with Docker Compose

```bash
docker-compose up -d
```

This will:
- Start PostgreSQL database on `localhost:5432`
- Build and start the Spring Boot application on `localhost:8080`

### 3. Verify the application

```bash
# Check container status
docker-compose ps

# View logs
docker-compose logs -f app
```

The application starts on port `8080` by default.

### 4. Access API Documentation

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/api-docs

## Quick Start (Local Development)

If you prefer to run locally without Docker:

### 1. Configure database

Create a PostgreSQL database:

```sql
CREATE DATABASE messaging;
CREATE USER messaging_user WITH PASSWORD 'messaging_password';
GRANT USAGE, CREATE ON SCHEMA public TO messaging_user;
GRANT ALL ON SCHEMA public TO messaging_user;
```

### 2. Run the application

```bash
./mvnw spring-boot:run
```

### 4. Access API Documentation

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/api-docs

## API Endpoints

### Create Message

```http
POST /api/v1/messages
Content-Type: application/json
Authorization: Bearer <token>
```

**Request Body:**

```json
{
  "messageType": "MT202",
  "network": "SWIFT",
  "senderInstitutionId": "BANK_IR_001",
  "receiverInstitutionId": "BANK_IR_002",
  "amount": 1000000.00,
  "currency": "EUR",
  "valueDate": "2026-07-26",
  "reference": "TXN-001"
}
```

**Success Response (201):**

```json
{
  "messageId": "msg_abc123",
  "messageType": "MT202",
  "network": "SWIFT",
  "status": "DRAFT",
  "creationDateTime": "2026-07-26T12:00:00Z",
  "validationResult": "SUCCESS",
  "validationErrors": []
}
```

**Validation Error Response (400):**

```json
{
  "status": "VALIDATION_FAILED",
  "validationResult": "FAILED",
  "validationErrors": [
    {
      "code": "MSG-001",
      "field": "amount",
      "message": "مبلغ باید بزرگتر از صفر باشد"
    }
  ]
}
```

## Error Codes

| Code | Message (Persian) | HTTP Status |
|------|-------------------|-------------|
| MSG-000 | پیام با موفقیت ایجاد شد | 200 |
| MSG-001 | اطلاعات ورودی معتبر نیست | 400 |
| MSG-002 | نوع پیام معتبر نیست | 400 |
| MSG-003 | شبکه انتخاب‌شده پشتیبانی نمی‌شود | 400 |
| MSG-004 | Message Definition یافت نشد | 400 |
| MSG-005 | Message Definition غیرفعال است | 400 |
| MSG-006 | اعتبارسنجی پیام ناموفق بود | 400 |
| MSG-007 | ایجاد پیام با خطا مواجه شد | 500 |

## Project Structure

```
src/
├── main/
│   ├── java/com/bank/messaging/
│   │   ├── config/           # Security, OpenAPI configuration
│   │   ├── controller/       # REST controllers
│   │   ├── dto/              # Request/Response DTOs
│   │   ├── entity/           # JPA entities
│   │   ├── enums/            # MessageStatus, Network, ErrorCode
│   │   ├── exception/        # Global exception handling
│   │   ├── repository/       # Spring Data repositories
│   │   └── service/          # Business logic
│   └── resources/
│       ├── application.yml   # Application configuration
│       └── db/migration/     # Flyway migrations
└── test/
    └── java/com/bank/messaging/
        ├── controller/       # Controller tests
        ├── repository/       # Integration tests
        └── service/          # Unit tests
```

## Running Tests

```bash
# Run all tests
./mvnw test

# Run with integration tests (uses Testcontainers)
./mvnw verify
```

## Database Migrations

Migrations are located in `src/main/resources/db/migration/` and run automatically on startup via Flyway.

To create a new migration:

```sql
-- V2__Add_new_table.sql
CREATE TABLE example (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);
```

## Configuration

Key configuration properties in `application.yml`:

| Property | Description | Default |
|----------|-------------|---------|
| `server.port` | Application port | 8080 |
| `spring.datasource.url` | Database URL | jdbc:postgresql://localhost:5432/messaging |
| `spring.flyway.enabled` | Enable migrations | true |
| `spring.cache.type` | Cache provider | caffeine |

## Docker Configuration

The application uses the following default Docker configuration:

| Service | Port | Environment Variables |
|---------|------|----------------------|
| PostgreSQL | 5432 | `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` |
| Application | 8080 | `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` |

### Environment Variables for Docker

When running with Docker Compose, the following environment variables are configured:

- `SPRING_DATASOURCE_URL`: `jdbc:postgresql://postgres:5432/messaging`
- `SPRING_DATASOURCE_USERNAME`: `messaging_user`
- `SPRING_DATASOURCE_PASSWORD`: `messaging_password`
- `SPRING_JPA_HIBERNATE_DDL_AUTO`: `validate`
- `SPRING_FLYWAY_ENABLED`: `true`

### Building Docker Image Manually

```bash
# Build the image
docker build -t fmp5:latest .

# Run the container
docker run -p 8080:8080 fmp5:latest
```

## Architecture Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Monolith first | Spring Boot monolith | Faster development, can split later |
| REST API | JSON over HTTP | Standard, widely supported |
| Test strategy | Unit + Integration + Contract | Full coverage with Testcontainers |
| Database migrations | Flyway | Version-controlled, Spring integration |
| API documentation | OpenAPI 3.0 / Swagger | Auto-generated, interactive UI |
| Caching | Caffeine (in-memory) | Simple, can migrate to Redis |

## Roadmap

| Epic | Description | Status |
|------|-------------|--------|
| EPIC-01 | Message Creation Service | ✅ Completed |
| EPIC-02 | Message Definition Service | Planned |
| EPIC-03 | Institution Management | Planned |
| EPIC-04 | Message Transmission | Planned |

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is proprietary and confidential.

