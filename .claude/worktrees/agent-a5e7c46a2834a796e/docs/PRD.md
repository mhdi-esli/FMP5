# Financial Messaging Platform - MT200 Message Service

## Epic Title
**Financial Messaging Platform - MT200 Financial Institution Transfer Message Service**

## Confidence Level
**92%**

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

### Non-Functional Requirements

| ID | Requirement | Priority |
|----|-------------|----------|
| NFR-001 | API response time < 500ms for validation | Must |
| NFR-002 | Support 100 concurrent message creation requests | Should |
| NFR-003 | All errors logged as structured JSON with correlation ID | Must |
| NFR-004 | Database transactions for data integrity | Must |
| NFR-005 | OAuth 2.0 authentication via existing SSO | Must |
| NFR-006 | OpenAPI 3.0 documentation auto-generated | Must |

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

---

## Technical Decisions

| Component | Technology | Rationale |
|-----------|------------|-----------|
| Language | Java 21+ | Latest LTS with modern features |
| Framework | Spring Boot 4.x | Enterprise standard, team expertise |
| Database | PostgreSQL | Robust, JSON support, team familiarity |
| Migrations | Flyway | Version-controlled, Spring Boot integration |
| Caching | Caffeine | Simple in-memory, no external dependency |
| Authentication | OAuth 2.0 (existing SSO) | Integrates with current infrastructure |
| Logging | Structured JSON + ELK | Matches existing observability stack |
| Testing | JUnit 5 + Testcontainers + Pact | Full coverage: unit, integration, contract |
| Documentation | OpenAPI 3.0 / Swagger | Auto-generated, interactive UI |
| Deployment | Docker on standalone servers | Current infrastructure |

---

## Architecture

**Standard Spring Boot layered structure:**

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
| 1 | Use Java 21+ / Spring Boot 4.x | Latest LTS with modern features | 2026-07-26 |
| 2 | PostgreSQL for message persistence | Robust, open-source, JSON support | 2026-07-26 |
| 3 | Hybrid approach for Message Definitions | Enums/Records for fixed structure + Database for runtime mappings | 2026-07-26 |
| 4 | Internal institution master table | Full control, includes BIC, branch codes, active status | 2026-07-26 |
| 5 | OAuth 2.0 via existing SSO system | Integrates with current infrastructure | 2026-07-26 |
| 6 | Structured JSON logging with correlation ID to ELK | Matches existing observability stack | 2026-07-26 |
| 7 | Unit + Integration + Contract tests (Pact) | Full coverage, API contract verification | 2026-07-26 |
| 8 | Docker on standalone servers | Current infrastructure, path to Kubernetes later | 2026-07-26 |
| 9 | Flyway for database migrations | Version-controlled, Spring Boot integration | 2026-07-26 |
| 10 | OpenAPI 3.0 / Swagger | Standard, auto-generated, interactive UI | 2026-07-26 |
| 11 | In-memory cache (Caffeine) | Simple, can migrate to Redis later | 2026-07-26 |
| 12 | Standard Spring Boot structure | Pragmatic, fastest to develop | 2026-07-26 |
| 13 | No additional features in V1 | Not required by specification document | 2026-07-26 |

---

## Open Questions

None - all questions resolved.

---

## Iteration History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-07-26 | Initial PRD created from technical document |
| 1.1 | 2026-07-26 | Added technology decisions through Q&A |
| 1.2 | 2026-07-26 | Finalized architecture, project scope, and epic breakdown |
