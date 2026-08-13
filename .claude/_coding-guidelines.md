# Coding Guidelines

**Last Updated:** 2026-08-12
**Project:** Financial Messaging Platform

This document is the authoritative source for coding standards and conventions. All advisors and skills must consult this before making code quality recommendations.

---

## Coding Standards

| Aspect | Decision |
|--------|----------|
| Language | Java 21+ |
| Framework | Spring Boot 4.x |
| Build Tool | Maven |
| Encoding | UTF-8 everywhere |

### SWA_101 Compliance (Mandatory)
- **URLs:** `kebab-case` paths, no `/api/` prefix, `/v{major}/` only
- **JSON:** `camelCase` fields, ISO 8601 UTC dates
- **Monetary values:** `BigDecimal` only — never `float`/`double`
- **String enums:** Never numeric
- **Empty arrays:** `[]`, never `null`
- **Error response:** `errorList` always present (empty `[]` on success)
- **HTTP status:** 400 for validation, never 422 for general validation

### Request/Response Envelope
```java
public record ResponseEnvelope<T>(
    T resultData,
    String message,
    List<ErrorItem> errorList
)
```

---

## Naming Conventions

| Entity | Convention | Example |
|--------|------------|---------|
| Classes | UpperCamelCase | `MessageController`, `ValidationService` |
| Methods | lowerCamelCase | `findActiveDefinition`, `validateMessage` |
| Variables | lowerCamelCase | `messageType`, `validationErrors` |
| Constants | UPPER_SNAKE_CASE | `MAX_MESSAGE_LENGTH` |
| Packages | lower.case | `com.bank.messaging` |
| Files | kebab-case for resources, PascalCase for classes | `message-controller.yaml`, `ValidationError.java` |

### Error Codes
- Format: `MSG-XXX` (MSG-000 to MSG-012)
- Mapped to SWA_101 codes 201-212 with issuer `MGS`

---

## Testing Practices

| Aspect | Target |
|--------|--------|
| Line Coverage | 80% (JaCoCo — build fails below) |
| Unit Tests | JUnit 5, mock external dependencies |
| Integration Tests | Testcontainers for PostgreSQL |
| Contract Tests | Pact for inter-service contracts |
| Architecture Tests | ArchUnit — violations fail `mvn test` |

### TDD Approach
1. Write failing test first
2. Implement minimal code to pass
3. Refactor
4. Never weaken test to force pass

### Test Fixture
Shared test fixture asserts envelope shape (`resultData`/`message`/`errorList`) on every endpoint.

---

## Design Patterns

| Pattern | Usage |
|---------|-------|
| Layered Architecture | Controller → Service → Repository |
| DTO Pattern | Never expose entities directly |
| Global Exception Handler | `@ControllerAdvice` for all exceptions |
| Validation | Bean Validation (JSR-380) with custom validators |
| Idempotency | Key from header, replay detection |

---

## Static Analysis

| Tool | Configuration |
|------|---------------|
| Spotless | Auto-formats, fails build on unformatted code |
| Checkstyle | Rules enforced by build |
| Dependency Check | Vulnerability scanning in `mvn verify` |

**Hard Rules:**
1. No magic numbers — use constants or enums
2. No raw strings — use constants for error codes/messages
3. No swallowed exceptions — log and rethrow or handle
4. No `System.out` — use SLF4J
5. No mutable state in services — services should be stateless
6. No business logic in controllers — controllers only delegate
7. No direct entity exposure — always use DTOs in API

---

## Documentation Conventions

| Artifact | Tool | Location |
|----------|------|----------|
| API Spec | SpringDoc → exported YAML | `/documents/openapi/openapi.yaml` |
| Architecture Decisions | ADR (MADR format) | `/documents/adr/` |
| Context Map | Mermaid | In ADRs or `/documents/` |
| C4 Diagrams | PlantUML | `/documents/c4/` |

---

## Open Issues

None. All required questions answered.

---

## Iteration History

- **2026-08-12** — Updated to reflect questionnaire answers. Added SWA_101 compliance section, fixed error code mapping, updated testing targets to 80% coverage.
