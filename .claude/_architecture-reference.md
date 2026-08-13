# Architecture Reference

**Last Updated:** 2026-08-12
**Confidence Level:** 100%

This document is the authoritative source for architectural decisions in the Financial Messaging Platform. All advisors and skills must consult this before making recommendations.

---

## System Architecture

| Component | Decision | Rationale |
|-----------|----------|-----------|
| Architecture Style | Layered Monolith | Simple, maintainable, team expertise |
| Deployment | Docker now, Kubernetes within the year | Current infrastructure, path to K8s |
| Service Communication | REST/HTTP (SWA_101 compliant) | Inter-service standards |

### Major Components
- `controller/` — HTTP endpoint layer
- `service/` — Business logic layer
- `repository/` — Data access layer
- `dto/` — Data transfer objects (SWA_101 envelope: `resultData`/`message`/`errorList`)
- `exception/` — Global exception handling with `@ControllerAdvice`
- `config/` — Spring configuration

---

## Major Design Decisions

| Decision | Details |
|----------|---------|
| URL Structure | `/v{major}/{resource}` (no `/api/` prefix per SWA_101 §1) |
| Error Format | SWA_101 format: `issuer` (2-6 uppercase) + `code` (≥201) + `description` |
| Validation | Bean Validation (JSR-380) + custom validators |
| Testing Pyramid | 80% line coverage, JUnit 5 + Testcontainers + Pact |
| Logging | Structured JSON to ELK stack |

### Error Codes Mapping
| Old Code | New SWA_101 Code | Issuer |
|----------|------------------|--------|
| MSG-001 to MSG-007 | 201-207 | `MGS` (Message service) |

**Decision:** Collect all validation errors, return together in single response per SWA_101 §3.

### Error Response Format
```json
{
  "resultData": null,
  "message": "درخواست نامعتبر است",
  "errorList": [
    {
      "issuer": "MGS",
      "code": 201,
      "description": "Invalid ISO 4217 currency code"
    }
  ]
}
```

---

## Constraints

| Constraint | Details |
|------------|---------|
| Language | Java 21+ |
| Framework | Spring Boot 4.x |
| Database | PostgreSQL with JSONB for validation errors |
| Migrations | Flyway |
| Caching | Caffeine (in-memory), path to Redis |

---

## Key Dependencies

| Dependency | Purpose |
|------------|---------|
| Spring Boot 4.x | Enterprise framework |
| PostgreSQL | Data persistence |
| Flyway | Database migrations |
| Caffeine | In-memory caching |
| JUnit 5 + Testcontainers | Unit and integration testing |
| Pact | Contract testing |
| SLF4J + Logback | Structured JSON logging |
| SpringDoc | OpenAPI 3.0 generation |

---

## Development Patterns

| Pattern | When to Use |
|---------|-------------|
| DTO Envelope | All API responses (SWA_101 `resultData`/`message`/`errorList`) |
| Global Exception Handler | `@ControllerAdvice` for all exceptions |
| Bean Validation | Request parameter validation |
| Service Layer | Business logic separation |
| Repository Pattern | Data access abstraction |

---

## Open Issues

None. All required questions answered.

---

## Iteration History

- **2026-08-12** — Updated to reflect questionnaire answers. Fixed URL structure (removed `/api/` prefix), error format (SWA_101 compliant), and added completeness score.
