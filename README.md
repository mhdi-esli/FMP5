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
- PostgreSQL 15+

## Quick Start

### 1. Clone the repository

```bash
git clone https://github.com/mhdi-esli/FMP5.git
cd FMP5
```

### 2. Configure database

Create a PostgreSQL database:

```sql
CREATE DATABASE messaging;
CREATE USER messaging_user WITH PASSWORD 'messaging_password';
GRANT ALL PRIVILEGES ON DATABASE messaging TO messaging_user;
```

### 3. Run the application

```bash
./mvnw spring-boot:run
```

The application starts on port `8080` by default.

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

## Contact

For questions or support, contact the International Team (تیم بین الملل).
