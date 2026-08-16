# Financial Messaging Platform - MT200 Message Service
## Epic Title
**Financial Messaging Platform - MT200 Financial Institution Transfer Message Service**
## Confidence Level
**88%** — EPIC-04 introduces open Technical Questions (see Open Questions)
## Problem Statement
Business systems need to create standardized inter-bank transfer messages (SWIFT MT200 series) without being coupled to specific message formats or network protocols. The International Team (تیم بین الملل) needs a unified service to create and validate financial transfer messages without understanding the underlying message format complexity.
The platform must abstract away SWIFT/SEPA message structures while ensuring compliance with financial messaging standards.
## Goals
1. **Abstract message complexity** - Business systems send business data, platform handles message format
2. **Support multiple networks** - SWIFT, SEPA with extensible design
3. **Centralize validation** - All message validation in one place with consistent error handling
4. **Generate audit trail** - All message creation attempts logged with correlation IDs
5. **Enable future extensibility** - Architecture supports adding new message types and networks
## Non-Goals
1. Business validation (account balance, transfer limits, authorization)
2. Actual message transmission to SWIFT/SEPA networks
3. Real-time message tracking after creation
4. Message versioning/history
5. Retry mechanisms or webhook notifications
6. Message types other than MT200 in this epic
---
## Epic Breakdown
### EPIC-01: Message Creation Service
Core API endpoint for creating MT200 financial institution transfer messages.
**Requirements:**
- FR-001: Accept message creation requests with 18 input fields via POST /api/v1/messages
- FR-002: Validate all mandatory fields are present
- FR-003: Validate amount > 0 and match currency decimal rules
- FR-004: Validate currency against ISO 4217 codes
- FR-005: Validate value date format (YYYY-MM-DD)
- FR-009: Generate unique Message ID
- FR-010: Store message with status "Draft" or "Validation Failed"
- FR-011: Return validation errors with Persian messages and error codes
- FR-012: Support SWIFT and SEPA networks
- FR-014: Audit all message creation attempts with timestamp and correlation ID
- NFR-001: API response time < 500ms for validation
- NFR-002: Support 100 concurrent message creation requests
- NFR-003: All errors logged as structured JSON with correlation ID
- NFR-004: Database transactions for data integrity
- NFR-005: OAuth 2.0 authentication via existing SSO
- NFR-006: OpenAPI 3.0 documentation auto-generated
**Acceptance Criteria:**
- AC-001: Create Valid Message
- AC-002: Validation Failure - Invalid Amount
- AC-003: Missing Required Field
- AC-004: Unsupported Network
- AC-007: Unauthorized Access
---
### EPIC-02: Message Definition Management
Configurable message templates and field mapping rules.
**Requirements:**
- FR-006: Select active Message Definition by message type and network
- FR-012: Support SWIFT and SEPA networks (definition management aspect)
**Acceptance Criteria:**
- AC-005: Missing Message Definition
---
### EPIC-03: Institution Management
Master data management for financial institutions.
**Requirements:**
- FR-007: Validate sender institution exists in institution master table
- FR-008: Validate receiver institution exists and is valid for selected network
- FR-013: Maintain institution master table with BIC, branch codes, active status
**Acceptance Criteria:**
- AC-006: Invalid Institution
---
### EPIC-04: Platform Standards Migration
Migrates the foundational stack and code conventions to the confirmed standards below (Java 25, Spring Boot 4.1.0, Redis, record-based DTOs), applied retroactively to EPIC-01/02/03 and as the baseline for all future epics. No new business functionality — conventions/infrastructure only.
**Requirements:**
- FR-015: Build target updated to Java 25
- FR-016: Framework upgraded to Spring Boot 4.1.0
- FR-017: Replace Caffeine cache with Redis for message-definition lookups, preserving existing TTL/eviction semantics unless explicitly changed
- FR-018: Convert all DTO classes from `final class` to `record`, preserving existing validation annotations and serialization behavior
- FR-019: Re-verify EPIC-01/02/03 implementations against the updated standards
- NFR-007: No regression in cache hit latency after the Redis migration [blocked by TQ-1]
- NFR-008: Build passes on Java 25 with no deprecated-API warnings introduced by the version bump
- NFR-009: Redis connection does not introduce a new unauthenticated network surface [blocked by TQ-2]
**Acceptance Criteria:**
- AC-008: Build Succeeds on Target Stack
- AC-009: Redis Cache Functional Equivalence
- AC-010: DTO Record Conversion Complete
- AC-011: Re-verified Epics Show No Standards Violations
---
## Requirements
### Functional Requirements
| ID | Requirement | Priority | Epic |
|----|-------------|----------|------|
| FR-001 | Accept message creation requests with 18 input fields via POST /api/v1/messages | Must | EPIC-01 |
| FR-002 | Validate all mandatory fields are present (messageType, network, requestReference, transactionReference, valueDate, currency, amount, senderInstitutionIdentifier, receiverInstitutionIdentifier) | Must | EPIC-01 |
| FR-003 | Validate amount > 0 and match currency decimal rules | Must | EPIC-01 |
| FR-004 | Validate currency against ISO 4217 codes | Must | EPIC-01 |
| FR-005 | Validate value date format (YYYY-MM-DD) | Must | EPIC-01 |
| FR-006 | Select active Message Definition by message type and network | Must | EPIC-02 |
| FR-007 | Validate sender institution exists in institution master table | Must | EPIC-03 |
| FR-008 | Validate receiver institution exists and is valid for selected network | Must | EPIC-03 |
| FR-009 | Generate unique Message ID | Must | EPIC-01 |
| FR-010 | Store message with status "Draft" (success) or "Validation Failed" (errors) | Must | EPIC-01 |
| FR-011 | Return validation errors with Persian messages and error codes | Must | EPIC-01 |
| FR-012 | Support SWIFT and SEPA networks | Must | EPIC-01, EPIC-02 |
| FR-013 | Maintain institution master table with BIC, branch codes, active status | Must | EPIC-03 |
| FR-014 | Audit all message creation attempts with timestamp and correlation ID | Must | EPIC-01 |
| FR-015 | Build target updated to Java 25 | Must | EPIC-04 |
| FR-016 | Framework upgraded to Spring Boot 4.1.0 | Must | EPIC-04 |
| FR-017 | Replace Caffeine cache with Redis for message-definition lookups | Must | EPIC-04 |
| FR-018 | Convert all DTO classes from final class to record | Must | EPIC-04 |
| FR-019 | Re-verify EPIC-01/02/03 against updated standards | Must | EPIC-04 |
### Non-Functional Requirements
| ID | Requirement | Priority |
|----|-------------|----------|
| NFR-001 | API response time < 500ms for validation | Must |
| NFR-002 | Support 100 concurrent message creation requests | Should |
| NFR-003 | All errors logged as structured JSON with correlation ID | Must |
| NFR-004 | Database transactions for data integrity | Must |
| NFR-005 | OAuth 2.0 authentication via existing SSO | Must |
| NFR-006 | OpenAPI 3.0 documentation auto-generated | Must |
| NFR-007 | No regression in cache hit latency after Redis migration | Must |
| NFR-008 | Build passes on Java 25 with no deprecated-API warnings | Must |
| NFR-009 | Redis connection secured — no unauthenticated network surface | Must |
---
## Acceptance Criteria
### AC-001: Create Valid Message
```gherkin
GIVEN valid input with all required fields
AND Message Definition exists and is active for MT200/SWIFT
AND sender/receiver institutions are valid
WHEN POST /api/v1/messages is called with valid OAuth token
THEN message is created with status "Draft"
AND unique messageId is returned
AND validationResult is "Success"
AND response status is 200
```
### AC-002: Validation Failure - Invalid Amount
```gherkin
GIVEN input with amount <= 0
WHEN POST /api/v1/messages is called
THEN message is NOT created
AND status is "Validation Failed"
AND validationErrors contains error code MSG-001
AND error message is "مبلغ باید بزرگتر از صفر باشد"
AND response status is 400
```
### AC-003: Missing Required Field
```gherkin
GIVEN input missing currency field
WHEN POST /api/v1/messages is called
THEN response status is 400
AND validationErrors includes code MSG-001
AND message indicates currency is required
```
### AC-004: Unsupported Network
```gherkin
GIVEN network value "INVALID_NETWORK"
WHEN POST /api/v1/messages is called
THEN response status is 400
AND validationErrors contains code MSG-003
AND message is "شبکه انتخاب‌شده پشتیبانی نمی‌شود"
```
### AC-005: Missing Message Definition
```gherkin
GIVEN no active Message Definition for requested message type and network
WHEN POST /api/v1/messages is called
THEN response status is 400
AND validationErrors contains code MSG-004 or MSG-005
```
### AC-006: Invalid Institution
```gherkin
GIVEN senderInstitutionIdentifier that does not exist in institution table
WHEN POST /api/v1/messages is called
THEN response status is 400
AND validationErrors indicates invalid sender institution
```
### AC-007: Unauthorized Access
```gherkin
GIVEN request without valid OAuth token
WHEN POST /api/v1/messages is called
THEN response status is 401
```
### AC-008: Build Succeeds on Target Stack
```gherkin
GIVEN the project is built with Java 25 and Spring Boot 4.1.0
WHEN `mvn clean package` is run
THEN the build succeeds
AND no deprecated-API warnings are introduced by the version bump
```
### AC-009: Redis Cache Functional Equivalence
```gherkin
GIVEN message-definition lookups previously served by Caffeine
WHEN the same lookups are served by Redis after migration
THEN cache hit/miss behavior is functionally equivalent
AND no regression in cache hit latency is observed
AND no Caffeine dependency remains in pom.xml
```
### AC-010: DTO Record Conversion Complete
```gherkin
GIVEN all DTO classes previously implemented as final class
WHEN the migration is complete
THEN all DTO classes are Java record types
AND existing validation annotations and serialization behavior are preserved
AND no final class DTOs remain
```
### AC-011: Re-verified Epics Show No Standards Violations
```gherkin
GIVEN EPIC-01, EPIC-02, and EPIC-03 are already implemented
WHEN verify-epic is re-run on each against the updated standards
THEN no standards violations related to framework version, cache library, or DTO pattern remain
```
---
## Agentic Decisions

**Implementation Stack**: Java 25, Spring Boot 4.1.0, PostgreSQL + Flyway, Redis (caching), OAuth 2.0 (existing SSO), OpenAPI 3.0/Swagger, Docker on standalone servers, structured JSON logging to ELK
**Testing Strategy**: JUnit 5 + Testcontainers + Pact — unit, integration, and contract test coverage
**CI/CD Approach**: [TBD — carry over actual current pipeline or confirm in set-standards]
**ADR Practice**: [TBD — confirm in set-standards; SAW_102's MADR/Persian ADR format applies once confirmed]
**Branching Strategy**: [TBD — confirm in set-standards]

*Superseded by EPIC-04*: Java 21+ → Java 25; Caffeine → Redis. See Decision Log entries 14–15.
---
## Architecture
**Standard Spring Boot layered structure:**
```
com.bank.messaging/
├── controller/       # REST endpoints
├── service/          # Business logic
├── repository/       # Data access (Spring Data JPA)
├── entity/           # JPA entities
├── dto/              # Request/Response DTOs — implemented as Java records (see EPIC-04, FR-018)
├── enums/            # MessageType, Network, Status, ErrorCodes
├── config/           # Spring configuration
├── exception/        # Global exception handling
├── validator/        # Custom validation logic
└── util/             # Helpers (correlation ID, etc.)
```
*Note*: the original structure listed a separate `records/` package alongside `dto/` for `MessageDefinition`/`ValidationRule`. Per EPIC-04's DTO-to-record conversion, `dto/` itself now holds record types — the two packages are consolidated under `dto/` rather than kept as a parallel split, to avoid an arbitrary distinction between "DTOs" and "records" once DTOs *are* records.
---
## Database Schema (Initial)
```sql
-- Messages table
CREATE TABLE messages (
    id BIGSERIAL PRIMARY KEY,
    message_id VARCHAR(50) UNIQUE NOT NULL,
    message_type VARCHAR(10) NOT NULL,
    network VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    request_reference VARCHAR(100),
    transaction_reference VARCHAR(100),
    related_reference VARCHAR(100),
    value_date DATE,
    currency VARCHAR(3),
    amount DECIMAL(20, 4),
    sender_institution_id VARCHAR(50),
    sender_bic VARCHAR(11),
    sender_branch_id VARCHAR(50),
    receiver_institution_id VARCHAR(50),
    receiver_bic VARCHAR(11),
    receiver_branch_id VARCHAR(50),
    charge_type VARCHAR(3),
    instruction_code VARCHAR(10),
    narrative TEXT,
    additional_information TEXT,
    validation_result VARCHAR(20),
    validation_errors JSONB,
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100)
);
-- Institutions table
CREATE TABLE institutions (
    id BIGSERIAL PRIMARY KEY,
    institution_id VARCHAR(50) UNIQUE NOT NULL,
    bic VARCHAR(11),
    name VARCHAR(200),
    branch_id VARCHAR(50),
    is_active BOOLEAN DEFAULT true,
    supported_networks VARCHAR[], -- SWIFT, SEPA
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);
-- Message Definition mappings (runtime configurable)
CREATE TABLE message_definition_mappings (
    id BIGSERIAL PRIMARY KEY,
    message_type VARCHAR(10) NOT NULL,
    network VARCHAR(20) NOT NULL,
    version INTEGER NOT NULL,
    is_active BOOLEAN DEFAULT true,
    field_mappings JSONB,
    validation_rules JSONB,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    UNIQUE(message_type, network, version)
);
```
---
## API Contract
### POST /api/v1/messages
**Request:**
```json
{
  "messageType": "200",
  "network": "SWIFT",
  "requestReference": "REQ-10001",
  "transactionReference": "TRX-458796",
  "valueDate": "2026-07-25",
  "currency": "USD",
  "amount": 25000.00,
  "senderInstitutionIdentifier": "BANK01",
  "receiverInstitutionIdentifier": "BANK02",
  "narrative": "Transfer for settlement"
}
```
**Success Response (200):**
```json
{
  "messageId": "MSG-20260725-000001",
  "messageType": "200",
  "network": "SWIFT",
  "status": "Draft",
  "creationDateTime": "2026-07-25T10:35:12",
  "validationResult": "Success",
  "validationErrors": []
}
```
**Error Response (400):**
```json
{
  "messageId": null,
  "messageType": "200",
  "network": "SWIFT",
  "status": "Validation Failed",
  "validationResult": "Failed",
  "validationErrors": [
    {
      "code": "MSG-001",
      "field": "Amount",
      "message": "مبلغ باید بزرگتر از صفر باشد"
    }
  ]
}
```
---
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
---
## Decision Log
| # | Decision | Rationale | Date |
|---|----------|-----------|------|
| 1 | ~~Use Java 21+ / Spring Boot 4.x~~ Superseded — see #14 | Latest LTS with modern features (at the time) | 2026-07-26 |
| 2 | PostgreSQL for message persistence | Robust, open-source, JSON support | 2026-07-26 |
| 3 | Hybrid approach for Message Definitions | Enums/Records for fixed structure + Database for runtime mappings | 2026-07-26 |
| 4 | Internal institution master table | Full control, includes BIC, branch codes, active status | 2026-07-26 |
| 5 | OAuth 2.0 via existing SSO system | Integrates with current infrastructure | 2026-07-26 |
| 6 | Structured JSON logging with correlation ID to ELK | Matches existing observability stack | 2026-07-26 |
| 7 | Unit + Integration + Contract tests (Pact) | Full coverage, API contract verification | 2026-07-26 |
| 8 | Docker on standalone servers | Current infrastructure, path to Kubernetes later | 2026-07-26 |
| 9 | Flyway for database migrations | Version-controlled, Spring Boot integration | 2026-07-26 |
| 10 | OpenAPI 3.0 / Swagger | Standard, auto-generated, interactive UI | 2026-07-26 |
| 11 | ~~In-memory cache (Caffeine)~~ Superseded — see #15 | Simple, can migrate to Redis later | 2026-07-26 |
| 12 | Standard Spring Boot structure | Pragmatic, fastest to develop | 2026-07-26 |
| 13 | No additional features in V1 | Not required by specification document | 2026-07-26 |
| 14 | Java 25 / Spring Boot 4.1.0 (supersedes #1) | Confirmed current target versions; #1 was an unconfirmed default from before set-standards existed | 2026-08-16 |
| 15 | Redis replaces Caffeine (supersedes #11) | Multi-instance deployment support — confirm exact driver in set-standards (TQ-1) | 2026-08-16 |
| 16 | DTOs implemented as `record`, not `final class` | Reduces boilerplate; matches confirmed coding-guidelines preference | 2026-08-16 |
---
## Open Questions
- TQ-1 (high-impact, EPIC-04): What's the actual driver for Redis over Caffeine — multi-instance cache consistency, or something else? Changes whether Redis Cluster/Sentinel is in scope.
- TQ-2 (high-impact, EPIC-04): Redis auth/TLS approach — password-only, ACL-based, TLS-in-transit?
- TQ-3 (high-impact, EPIC-04): Confirm minimum JDK for Spring Boot 4.1.0 is compatible with Java 25 (check Spring's supported-versions matrix before starting `write-spec epic-04`).
---
## Iteration History
| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-07-26 | Initial PRD created from technical document |
| 1.1 | 2026-07-26 | Added technology decisions through Q&A |
| 1.2 | 2026-07-26 | Finalized architecture, project scope, and epic breakdown |
| 1.3 | 2026-08-16 | Migrated to brainstorm/Epic_PRD.md (correct path for write-spec/plan-tasks/set-standards). Replaced Technical Decisions with Agentic Decisions (5-field format for set-standards carryover). Added EPIC-04: Platform Standards Migration (Java 25, Spring Boot 4.1.0, Redis, record DTOs) with FR-015–019, NFR-007–009, AC-008–011. Reopened TQ-1/2/3 under Open Questions. Consolidated dto/records/ package split in Architecture. |
