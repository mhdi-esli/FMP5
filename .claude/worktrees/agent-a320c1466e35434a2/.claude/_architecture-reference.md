# Architecture Reference

**Last Updated:** 2026-07-26
**Confidence Level:** 92%

This document is the authoritative source for architectural decisions in the Financial Messaging Platform. All advisors and skills must consult this before making recommendations.

---

## Technology Stack

| Component | Decision | Rationale |
|-----------|----------|-----------|
| Language | Java 21+ | Latest LTS with modern features |
| Framework | Spring Boot 4.x | Enterprise standard, team expertise |
| Database | PostgreSQL | Robust, JSON support, team familiarity |
| Migrations | Flyway | Version-controlled, Spring Boot integration |
| Caching | Caffeine (in-memory) | Simple, no external dependency, path to Redis |
| Authentication | OAuth 2.0 via existing SSO | Integrates with current infrastructure |
| Logging | Structured JSON + ELK | Matches existing observability stack |
| Testing | JUnit 5 + Testcontainers + Pact | Full coverage: unit, integration, contract |
| Documentation | OpenAPI 3.0 / Swagger | Auto-generated, interactive UI |
| Deployment | Docker on standalone servers | Current infrastructure, path to Kubernetes |

---

## Architecture Style

**Decision:** Standard Spring Boot Layered Architecture

```
com.bank.messaging/
├── controller/       # REST endpoints
├── service/          # Business logic
├── repository/       # Data access (Spring Data JPA)
├── entity/           # JPA entities
├── dto/              # Request/Response DTOs
├── enums/            # MessageType, Network, Status, ErrorCodes
├── records/          # MessageDefinition, ValidationRule
├── config/           # Spring configuration
├── exception/        # Global exception handling
├── validator/        # Custom validation logic
└── util/             # Helpers (correlation ID, etc.)
```

**Rationale:** Pragmatic, fastest to develop, team familiarity. Not Clean Architecture for this project.

---

## System Boundaries

### In Scope (This Platform)

| Boundary | Description |
|----------|-------------|
| Message Creation API | `POST /api/v1/messages` |
| Message Validation | Field-level, business rules |
| Message Persistence | PostgreSQL storage |
| Message Definition Management | Configurable templates |
| Institution Master Data | BIC, branch codes, active status |

### Out of Scope (External Systems)

| Boundary | Owner |
|----------|-------|
| Business validation (balance, limits) | Calling systems |
| Actual SWIFT/SEPA transmission | Future epic |
| User authentication | Existing SSO system |
| Real-time tracking | Future epic |

---

## Cross-Epic Dependencies

```
EPIC-01: Message Creation Service
├── depends on → EPIC-02 (Message Definition Repository)
│   └── Solution: Stub/fake implementation, swap when EPIC-02 ready
└── depends on → EPIC-03 (Institution Repository)
    └── Solution: Stub/fake implementation, swap when EPIC-03 ready

EPIC-02: Message Definition Management
└── no external dependencies

EPIC-03: Institution Management
└── no external dependencies
```

**Stub/Fake Pattern:**
- Define interface in dependent epic
- Implement stub for testing
- Swap via `@Profile` or configuration when real implementation ready
- No rework needed in dependent epic

---

## Data Model

### Core Entities

```
┌─────────────────┐     ┌─────────────────┐
│     Message     │     │  Institution    │
├─────────────────┤     ├─────────────────┤
│ messageId       │     │ institutionId   │
│ messageType     │     │ bic             │
│ network         │     │ name            │
│ status          │     │ branchId        │
│ amount          │     │ isActive        │
│ currency        │     │ supportedNetworks│
│ senderInstId    │────▶│                 │
│ receiverInstId  │────▶│                 │
│ validationErrors│     │                 │
│ createdAt       │     │                 │
└─────────────────┘     └─────────────────┘

┌─────────────────────────┐
│ MessageDefinitionMapping│
├─────────────────────────┤
│ messageType             │
│ network                 │
│ version                 │
│ isActive                │
│ fieldMappings (JSONB)   │
│ validationRules (JSONB) │
└─────────────────────────┘
```

### Message ID Format

**Decision:** `MSG-YYYYMMDD-NNNNNN` (sequential daily)

Example: `MSG-20260725-000001`

**Rationale:** Human-readable, matches PRD example, daily sequence.

---

## Validation Strategy

**Decision:** Collect all validation errors, return together

| Aspect | Approach |
|--------|----------|
| Error collection | All errors in single response |
| Error format | `{ code, field, message }` |
| Message language | Persian |
| Message storage | Java Enum with code and message fields |

### Error Codes

| Code | Message (Persian) |
|------|-------------------|
| MSG-000 | پیام با موفقیت ایجاد شد |
| MSG-001 | اطلاعات ورودی معتبر نیست |
| MSG-002 | نوع پیام معتبر نیست |
| MSG-003 | شبکه انتخاب‌شده پشتیبانی نمی‌شود |
| MSG-004 | Message Definition یافت نشد |
| MSG-005 | Message Definition غیرفعال است |
| MSG-006 | اعتبارسنجی پیام ناموفق بود |
| MSG-007 | ایجاد پیام با خطا مواجه شد |

---

## Performance Requirements

| Metric | Target |
|--------|--------|
| API response time | < 500ms for validation |
| Concurrent requests | 100 simultaneous |
| Database | PostgreSQL with JSONB for validation errors |

---

## Security Requirements

| Aspect | Approach |
|--------|----------|
| Authentication | OAuth 2.0 via existing SSO |
| Authorization | Token-based, user info from token |
| Input validation | All fields validated before processing |
| SQL injection | Parameterized queries only (JPA) |
| Audit logging | All requests logged with correlation ID |

---

## Scalability Considerations

### Current Deployment

- Docker on standalone servers
- Single instance per service
- PostgreSQL single instance

### Future Path

- Kubernetes deployment
- Horizontal pod autoscaling
- Redis cache (replace Caffeine)
- Read replicas for PostgreSQL

---

## Integration Points

### Inbound

| Consumer | Protocol | Auth |
|----------|----------|------|
| International Team | REST/JSON | OAuth 2.0 |

### Outbound

| Provider | Protocol | Purpose |
|----------|----------|---------|
| SSO System | OAuth 2.0 | Token validation |
| PostgreSQL | JDBC | Data persistence |
| ELK Stack | TCP/HTTP | Structured logging |

---

## Standards Update Process

When a decision has cross-epic implications:

1. Document the proposed change
2. Run `/set-standards` (if skill exists) or manual update
3. Update `_architecture-reference.md`
4. Notify affected epics

**This document is the single source of truth. Advisors must read it fresh on each invocation.**

---

## References

- PRD: `docs/PRD.md`
- EPIC-01 Spec: `specs/epic-01-message-creation-service/spec.md`
- Original Requirements: `docs/MT200series-messages-document.md`
