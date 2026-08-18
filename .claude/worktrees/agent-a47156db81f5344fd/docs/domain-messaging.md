# Domain: messaging

**Generated:** 2026-07-26
**Coverage:** 100%
**Version Hash:** `a1b2c3d4e5` (22 main source files + 4 test files)

---

## Domain Model

### Entities

| Entity | Description | Key Fields | File |
|--------|-------------|------------|------|
| `Message` | Core entity for a financial institution transfer message | `messageId`, `messageType`, `network`, `status`, `amount`, `currency`, `senderInstitutionId`, `receiverInstitutionId`, `valueDate`, `validationErrors` (JSONB) | `entity/Message.java` |
| `Institution` | Financial institution master data (stub for EPIC-03) | `institutionId`, `bic`, `name`, `branchId`, `isActive`, `supportedNetworks` | `entity/Institution.java` |
| `MessageDefinitionMapping` | Message definition configuration (stub for EPIC-02) | `messageType`, `network`, `version`, `isActive`, `fieldMappings` (JSONB) | `entity/MessageDefinitionMapping.java` |

### Value Objects (Records)

| Value Object | Description | Fields | File |
|-------------|-------------|--------|------|
| `MessageRequest` | Input DTO for message creation | 18 fields — see API Endpoints section | `dto/MessageRequest.java` |
| `MessageResponse` | Output DTO after message creation | `messageId`, `messageType`, `network`, `status`, `creationDateTime`, `validationResult`, `validationErrors` | `dto/MessageResponse.java` |
| `ValidationError` | A single validation error | `code`, `field`, `message` | `dto/ValidationError.java` |
| `ValidationResult` | Validation result container | `result` (SUCCESS/FAILED), `errors` (list) | `service/ValidationResult.java` |

### Enums

| Enum | Values | File |
|------|--------|------|
| `MessageType` | `MT200("200")` | `enums/MessageType.java` |
| `Network` | `SWIFT`, `SEPA` | `enums/Network.java` |
| `MessageStatus` | `DRAFT`, `VALIDATION_FAILED` | `enums/MessageStatus.java` |
| `ValidationResultEnum` | `SUCCESS`, `FAILED` | `enums/ValidationResultEnum.java` |
| `ErrorCode` | `MSG-000` to `MSG-007` (Persian messages) | `enums/ErrorCode.java` |

---

## API Endpoints

### POST /api/v1/messages

| Method | Path | Authentication | Content-Type |
|--------|------|---------------|--------------|
| POST | `/api/v1/messages` | OAuth 2.0 (JWT) | `application/json` |

**Controller:** `MessageController.createMessage()` — line 41

**Request (`MessageRequest`):**

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `messageType` | String | ✅ | `@NotBlank` |
| `network` | String | ✅ | `@NotBlank`, validated against `Network` enum |
| `requestReference` | String | ✅ | `@NotBlank`, alphanumeric + `-_` |
| `transactionReference` | String | ✅ | `@NotBlank`, alphanumeric + `-_` |
| `relatedReference` | String | ❌ | Alphanumeric + `-_` (if provided) |
| `valueDate` | String | ✅ | `@NotBlank`, format `yyyy-MM-dd` |
| `currency` | String | ✅ | `@NotBlank`, `@Pattern("^[A-Z]{3}$")`, validated against supported list |
| `amount` | BigDecimal | ✅ | `@NotNull`, `@Positive`, must be > 0 |
| `senderInstitutionIdentifier` | String | ✅ | `@NotBlank` |
| `senderBIC` | String | ❌ | — |
| `senderBranchIdentifier` | String | ❌ | — |
| `receiverInstitutionIdentifier` | String | ✅ | `@NotBlank` |
| `receiverBIC` | String | ❌ | — |
| `receiverBranchIdentifier` | String | ❌ | — |
| `chargeType` | String | ❌ | — |
| `instructionCode` | String | ❌ | — |
| `narrative` | String | ❌ | — |
| `additionalInformation` | String | ❌ | — |

**Success Response (200):**

```json
{
  "messageId": "MSG-20260726-000001",
  "messageType": "200",
  "network": "SWIFT",
  "status": "DRAFT",
  "creationDateTime": "2026-07-26T10:35:12",
  "validationResult": "SUCCESS",
  "validationErrors": []
}
```

**Error Response (400) — Validation Failed:**

```json
{
  "messageId": null,
  "messageType": "200",
  "network": "SWIFT",
  "status": "VALIDATION_FAILED",
  "creationDateTime": "2026-07-26T10:35:12",
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

**Error Response (401) — Unauthorized:**

OAuth 2.0 — Spring Security returns 401 when no valid JWT token is present.

---

## Error Codes

| Code | Persian Message | HTTP Status | Evidence |
|------|----------------|-------------|----------|
| MSG-000 | پیام با موفقیت ایجاد شد | 200 | `ErrorCode.java` line 11 |
| MSG-001 | اطلاعات ورودی معتبر نیست | 400 | `ErrorCode.java` line 12, `GlobalExceptionHandler.java` line 36 |
| MSG-002 | نوع پیام معتبر نیست | 400 | `ErrorCode.java` line 13 (defined but not yet enforced in code) |
| MSG-003 | شبکه انتخاب‌شده پشتیبانی نمی‌شود | 400 | `ErrorCode.java` line 14, `MessageValidationService.java` line 79 |
| MSG-004 | Message Definition یافت نشد | 400 | `ErrorCode.java` line 15, `MessageCreationService.java` line 101 |
| MSG-005 | Message Definition غیرفعال است | 400 | `ErrorCode.java` line 16, `MessageCreationService.java` line 93 |
| MSG-006 | اعتبارسنجی پیام ناموفق بود | 400 | `ErrorCode.java` line 17 (defined but not yet enforced in code) |
| MSG-007 | ایجاد پیام با خطا مواجه شد | 500 | `ErrorCode.java` line 18, `GlobalExceptionHandler.java` line 103 |

---

## Key Behaviors / Business Rules

### BR-1: Amount Must Be Greater Than Zero

**Evidence:** `MessageValidationService.validateAmount()` — line 91

**Rule:** When `amount` is <= 0, validation returns `MSG-001` with Persian message.

**Tests:** `MessageValidationServiceTest.validate_amountZero_returnsFailed()`

---

### BR-2: Currency Must Be Valid ISO 4217

**Evidence:** `MessageValidationService.validateCurrency()` — line 105

**Rule:** Currency must be one of 30 supported codes (USD, EUR, GBP, CHF, JPY, etc.). Invalid codes produce `MSG-001`.

**Tests:** `MessageValidationServiceTest.validate_invalidCurrencyCode_returnsFailed()`

---

### BR-3: Value Date Must Be Valid Format

**Evidence:** `MessageValidationService.validateValueDate()` — line 119

**Rule:** Value date must match `yyyy-MM-dd` format. Invalid format produces `MSG-001`.

**Tests:** `MessageValidationServiceTest.validate_invalidValueDateFormat_returnsFailed()`

---

### BR-4: Network Must Be Supported

**Evidence:** `MessageValidationService.validateNetwork()` — line 76, `MessageCreationService.createMessage()` — line 56

**Rule:** Only `SWIFT` and `SEPA` networks are accepted. Invalid network produces `MSG-003`.

**Tests:** `MessageValidationServiceTest.validate_invalidNetwork_returnsFailed()`, `MessageControllerTest.postMessages_invalidNetwork_returns400()`

---

### BR-5: References Must Not Have Invalid Characters

**Evidence:** `MessageValidationService.validateReference()` — line 137

**Rule:** References may only contain alphanumeric characters, hyphens, and underscores. Other characters produce `MSG-001`.

**Tests:** Not explicitly tested (only happy-path reference tested via other ACs).

---

### BR-6: Message Definition Must Exist and Be Active

**Evidence:** `MessageCreationService.validateMessageDefinition()` — lines 85-104

**Rule:** Before creating a message, the service checks for an active message definition. If none exists → `MSG-004`. If it exists but is inactive → `MSG-005`.

**Tests:** `MessageCreationServiceTest.createMessage_messageDefinitionNotFound_returnsError()`, `MessageCreationServiceTest.createMessage_messageDefinitionInactive_returnsError()`

---

### BR-7: All Validation Errors Collected Together

**Evidence:** `MessageValidationService.validate()` — line 41

**Rule:** All validation errors are collected in a single pass and returned together. Multiple errors in one request are all reported.

**Tests:** `MessageValidationServiceTest.validate_multipleErrors_returnsAllErrors()`

---

### BR-8: Message ID Generated as MSG-YYYYMMDD-NNNNNN

**Evidence:** `MessageCreationService.generateMessageId()` — lines 111-114

**Rule:** Message IDs follow the format `MSG-{date}-{6-digit-sequence}`. Sequence is per-instance `AtomicInteger`.

**Tests:** `MessageCreationServiceTest.createMessage_messageIdFormat_correct()`

---

### BR-9: Failed Messages Persisted to Database

**Evidence:** `MessageCreationService.createFailedResponse()` — lines 145-169

**Rule:** Even failed validation attempts save the message entity with `VALIDATION_FAILED` status and JSONB serialized errors.

**Tests:** Not explicitly tested (covered by AC-002/AC-004 end-to-end).

---

### BR-10: OAuth 2.0 Required for All Protected Endpoints

**Evidence:** `SecurityConfig.securityFilterChain()` — lines 20-29

**Rule:** Swagger UI and health endpoints are public. All other requests require a valid OAuth 2.0 JWT token.

**Tests:** `MessageControllerTest.postMessages_noToken_returns401()`

---

## Key Components

| Component | Type | Responsibility | File |
|-----------|------|----------------|------|
| `MessageController` | Controller | REST endpoint for message creation | `controller/MessageController.java` |
| `MessageCreationService` | Service | Orchestrates validation, definition lookup, ID generation, persistence | `service/MessageCreationService.java` |
| `MessageValidationService` | Service | Validates all input fields (required, format, range) | `service/MessageValidationService.java` |
| `ValidationResult` | Record | Validation result container | `service/ValidationResult.java` |
| `MessageRepository` | Repository | Spring Data JPA for Message entity | `repository/MessageRepository.java` |
| `MessageDefinitionMappingRepository` | Repository | Stub for EPIC-02 | `repository/MessageDefinitionMappingRepository.java` |
| `InstitutionRepository` | Repository | Stub for EPIC-03 | `repository/InstitutionRepository.java` |
| `GlobalExceptionHandler` | Component | `@RestControllerAdvice` for validation, type mismatch, and generic errors | `exception/GlobalExceptionHandler.java` |
| `SecurityConfig` | Configuration | OAuth 2.0 resource server configuration | `config/SecurityConfig.java` |
| `OpenApiConfig` | Configuration | OpenAPI/Swagger documentation config | `config/OpenApiConfig.java` |

---

## Features

| Feature | Status | Evidence |
|---------|--------|----------|
| Create MT200 Message | ✅ Implemented | `MessageController.createMessage()` → `MessageCreationService.createMessage()` |
| Amount Must Be Positive | ✅ Implemented | `MessageValidationService.validateAmount()` exceeds zero check |
| Currency Validation | ✅ Implemented | `MessageValidationService.validateCurrency()` |
| Date Validation | ✅ Implemented | `MessageValidationService.validateValueDate()` |
| Network Validation | ✅ Implemented | `MessageValidationService.validateNetwork()` |
| Message Definition Check | ✅ Implemented | `MessageCreationService.validateMessageDefinition()` |
| Message ID Generation | ✅ Implemented | `MessageCreationService.generateMessageId()` in MSG-YYYYMMDD-NNNNNN format |
| Validation Error Collection | ✅ Implemented | `MessageValidationService.validate()` collects all errors |
| OAuth 2.0 Authentication | ✅ Implemented | `SecurityConfig` with `oauth2ResourceServer` |
| Error Codes with Persian Messages | ✅ Implemented | `ErrorCode` enum with Persian text |
| OpenAPI Documentation | ✅ Implemented | `OpenApiConfig` with springdoc |
| Institution Validation | ⚠️ Stub | `InstitutionRepository` stub for EPIC-03 — not yet wired into message creation |
| Message Definition Management | ⚠️ Stub | `MessageDefinitionMappingRepository` stub for EPIC-02 |
| Schema Migrations | ✅ Implemented | `V1__Initial_schema.sql` with seed data |

---

## Acceptance Criteria

| AC ID | Description | Status | Evidence |
|-------|-------------|--------|----------|
| AC-001 | Create Valid Message | ✅ Verified | `MessageCreationServiceTest.createMessage_validRequest_returnsDraftStatus()` |
| AC-002 | Validation Failure — Invalid Amount | ✅ Verified | `MessageValidationServiceTest.validate_amountZero_returnsFailed()` |
| AC-003 | Missing Required Field | ✅ Verified | `MessageValidationServiceTest.validate_missingCurrency_returnsFailed()` |
| AC-004 | Unsupported Network | ✅ Verified | `MessageValidationServiceTest.validate_invalidNetwork_returnsFailed()` |
| AC-007 | Unauthorized Access | ✅ Verified | `MessageControllerTest.postMessages_noToken_returns401()` |

Note: AC-005 (Missing Message Definition) is covered by `MessageCreationServiceTest.createMessage_messageDefinitionNotFound_returnsError()` but maps to EPIC-02 scope in the PRD. AC-006 (Invalid Institution) maps to EPIC-03 scope.

---

## Data Flow

### Message Creation (Happy Path)

```
Client → MessageController → MessageCreationService → MessageValidationService (validate)
                                                     → MessageDefinitionMappingRepository (check active)
                                                     → MessageRepository (persist)
                                                     → MessageResponse
```

### Message Creation (Validation Fails)

```
Client → MessageController → MessageCreationService → MessageValidationService (validate fails)
                                                     → MessageRepository (persist failed attempt)
                                                     → MessageResponse (status: VALIDATION_FAILED)
```

---

## Database Schema

### `messages` Table

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | BIGSERIAL | PRIMARY KEY |
| `message_id` | VARCHAR(50) | UNIQUE, NOT NULL |
| `message_type` | VARCHAR(10) | NOT NULL |
| `network` | VARCHAR(20) | NOT NULL |
| `status` | VARCHAR(20) | NOT NULL |
| `validation_errors` | JSONB | nullable |
| `created_at` | TIMESTAMP | NOT NULL |

Full schema in `V1__Initial_schema.sql` — 23 columns total.

---

## Unverified / Needs Review

| Item | Issue | Location |
|------|-------|----------|
| MSG-002 enforcement | ErrorCode defined but not referenced in business logic | `ErrorCode.java` line 13 |
| MSG-006 enforcement | ErrorCode defined but not referenced in business logic | `ErrorCode.java` line 17 |
| Institution validation | `InstitutionRepository` exists but is not wired into `MessageCreationService` | Stub pending EPIC-03 |
| Reference character validation test | `validateReference()` has no dedicated test for invalid characters | `MessageValidationService.java` line 131 |
| Decimal places per currency | Spec hints at "decimal places per currency rules" but not implemented | As designed — currency decimal validation not in scope for V1 per spec decision |

---

## Open Issues

- No files were skipped during analysis.

---

## Iteration History

| Date | Changes |
|------|---------|
| 2026-07-26 | Initial documentation generated from codebase (22 source files, 4 test files, 1 migration) |
