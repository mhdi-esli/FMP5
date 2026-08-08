# FMP5 — Financial Messaging Platform
Spring Boot service creating/validating SWIFT MT200 inter-bank transfer messages.

## Commands
- Build: `mvn clean package`
- Run: `mvn spring-boot:run`
- Test: `mvn test`
- Build (no tests): `mvn clean package -DskipTests`

## Stack
Java 21 · Spring Boot 3.3.2 · PostgreSQL + Flyway · Caffeine cache · SpringDoc OpenAPI 2.6.0 · Testcontainers 1.20.1 · Lombok

## Structure
```
src/main/java/com/bank/messaging/
├── config/       Security, Cache, OpenAPI
├── controller/   Message, MessageDefinition, Institution
├── dto/
├── entity/       Message, MessageDefinitionMapping, Institution
├── enums/        MessageType, Network, Status, ErrorCodes
├── exception/
├── repository/
└── service/
```

## API
- `POST /api/v1/messages` — create MT200 transfer message
- `/api-docs`, `/swagger-ui.html`

## Message flow
Controller → Validation Service (required fields) → MessageDefinition Service (type/network active) → Institution Service (sender + network compat) → Repository (status: DRAFT | VALIDATION_FAILED)

## Error codes (responses are in Persian)
MSG-001 missing/invalid fields · MSG-003 invalid network · MSG-004 definition not found · MSG-005 definition inactive · MSG-010/011/012 institution validation

## DB
Flyway migrations: `src/main/resources/db/migration/`. Config via `SPRING_DATASOURCE_URL/_USERNAME/_PASSWORD`. Schema introspection available via postgres-mcp — always confirm live structure via MCP before writing migrations or Entity/Repository code (see .claude/rules/database.md).

## Security
OAuth2 resource server. JWK: `https://sso.tps.ir/.well-known/openid-configuration/jwks`

## Cache
Caffeine — max 1000 entries, TTL 300s

## Conventions
- Message ID: `MSG-YYYYMMDD-NNNNNN` (daily sequential)
- Validation errors always in Persian
- Integration tests use Testcontainers (needs DB)
- Dev: `spring.jpa.show-sql=true` for SQL logging
- Large files (logs, build output): Grep/bounded Read, not full Read (see .claude/rules/large-files.md)

## Epics
EPIC-01 Message Creation ✓ · EPIC-02 Message Definition Mgmt ✓ · EPIC-03 Institution Mgmt ✓

## SDD
Spec-driven workflow via `.claude/skills/`: brainstorm → write-spec → architecture → implementation → verify. Each phase reads from written files (`brainstorm/00_Project_Brief.md`, spec docs), not conversation history — each is safe to run in a separate Claude Code session/container.
