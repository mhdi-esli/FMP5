# Technical Specification: EPIC-01 Message Creation Service

## Confidence Level: 95%

**PRD Confidence Level:** 92%

---

## Architecture

The Message Creation Service follows a standard Spring Boot layered architecture, handling the core API endpoint for MT200 financial institution transfer message creation.

```
┌─────────────────────────────────────────────────────────────────┐
│                        Presentation Layer                        │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  MessageController                                        │    │
│  │  - POST /api/v1/messages                                  │    │
│  │  - Request validation, response mapping                   │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                         Service Layer                            │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  MessageCreationService                                   │    │
│  │  - Orchestrate validation flow                            │    │
│  │  - Generate message ID                                     │    │
│  │  - Coordinate institution/message definition lookups       │    │
│  └─────────────────────────────────────────────────────────┘    │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  MessageValidationService                                 │    │
│  │  - Field validation (required, format, range)             │    │
│  │  - Business rule validation                               │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        Repository Layer                          │
│  ┌─────────────────────┐  ┌─────────────────────┐               │
│  │  MessageRepository   │  │  InstitutionRepo    │               │
│  │  (Spring Data JPA)   │  │  (from EPIC-03)     │               │
│  └─────────────────────┘  └─────────────────────┘               │
│  ┌─────────────────────┐                                        │
│  │  MessageDefinition   │                                        │
│  │  Repository (EPIC-02)│                                        │
│  └─────────────────────┘                                        │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        Database (PostgreSQL)                     │
│  - messages table                                                │
│  - institutions table (EPIC-03)                                  │
│  - message_definition_mappings table (EPIC-02)                   │
└─────────────────────────────────────────────────────────────────┘
```

### Request Flow

1. **Controller** receives HTTP POST, extracts OAuth user info
2. **Validation** runs field-level checks (required, format, range)
3. **Institution lookup** validates sender/receiver (depends on EPIC-03)
4. **Message Definition lookup** validates network support (depends on EPIC-02)
5. **Message ID generation** creates unique identifier
6. **Persistence** saves Message entity with Draft/Validation Failed status
7. **Response** returns result with validation errors if any

---

## Components

### 1. MessageController

| Aspect | Details |
|--------|---------|
| **Name** | `MessageController` |
| **Package** | `com.bank.messaging.controller` |
| **Responsibility** | REST endpoint for message creation, request/response mapping |
| **Exposes** | `POST /api/v1/messages` |
| **Consumes** | `MessageCreationService`, `MessageRequestMapper`, `MessageResponseMapper` |

### 2. MessageCreationService

| Aspect | Details |
|--------|---------|
| **Name** | `MessageCreationService` |
| **Package** | `com.bank.messaging.service` |
| **Responsibility** | Orchestrates message creation flow, coordinates validation, generates IDs |
| **Exposes** | `createMessage(MessageRequest): MessageResponse` |
| **Consumes** | `MessageValidationService`, `MessageRepository`, `InstitutionRepository`, `MessageDefinitionRepository` |

### 3. MessageValidationService

| Aspect | Details |
|--------|---------|
| **Name** | `MessageValidationService` |
| **Package** | `com.bank.messaging.service` |
| **Responsibility** | Validates all input fields per business rules |
| **Exposes** | `validate(MessageRequest): ValidationResult` |
| **Consumes** | `CurrencyValidator`, `DateValidator`, `ReferenceValidator` |

### 4. MessageRequest (DTO)

| Aspect | Details |
|--------|---------|
| **Name** | `MessageRequest` |
| **Package** | `com.bank.messaging.dto` |
| **Responsibility** | Input DTO for message creation request |
| **Fields** | All 18 input fields from API contract |

### 5. MessageResponse (DTO)

| Aspect | Details |
|--------|---------|
| **Name** | `MessageResponse` |
| **Package** | `com.bank.messaging.dto` |
| **Responsibility** | Output DTO for message creation response |
| **Fields** | messageId, messageType, network, status, creationDateTime, validationResult, validationErrors |

### 6. Message (Entity)

| Aspect | Details |
|--------|---------|
| **Name** | `Message` |
| **Package** | `com.bank.messaging.entity` |
| **Responsibility** | JPA entity for messages table |
| **Fields** | All fields from database schema |

### 7. ValidationError (DTO)

| Aspect | Details |
|--------|---------|
| **Name** | `ValidationError` |
| **Package** | `com.bank.messaging.dto` |
| **Responsibility** | Represents a single validation error |
| **Fields** | code, field, message |

### 8. Enums

| Enum | Package | Values |
|------|---------|--------|
| `MessageType` | `com.bank.messaging.enums` | MT200("200") |
| `Network` | `com.bank.messaging.enums` | SWIFT, SEPA |
| `MessageStatus` | `com.bank.messaging.enums` | DRAFT, VALIDATION_FAILED |
| `ValidationResult` | `com.bank.messaging.enums` | SUCCESS, FAILED |
| `ErrorCode` | `com.bank.messaging.enums` | MSG-000 to MSG-007 |

---

## Dependencies

### Internal Dependencies

| Dependency | Epic | Status | Description |
|------------|------|--------|-------------|
| `InstitutionRepository` | EPIC-03 | Not implemented | Repository for institution lookup |
| `MessageDefinitionRepository` | EPIC-02 | Not implemented | Repository for message definition lookup |

### External Dependencies

| Dependency | Version | Purpose |
|------------|---------|---------|
| Java | 21+ | Runtime |
| Spring Boot | 4.x | Framework |
| Spring Web | (via Boot) | REST API |
| Spring Data JPA | (via Boot) | Repository layer |
| PostgreSQL Driver | 42.7.x | Database connectivity |
| Flyway | (via Boot) | Database migrations |
| Caffeine | 3.1.x | In-memory caching |
| Springdoc OpenAPI | 2.x | API documentation |
| OAuth2 Resource Server | (via Boot) | Authentication |
| Jackson | (via Boot) | JSON serialization |
| Lombok | 1.18.x | Boilerplate reduction |
| JUnit 5 | (via Boot) | Unit testing |
| Testcontainers | 1.19.x | Integration testing |
| Pact | 4.6.x | Contract testing |

---

## Boundaries

### In Scope (EPIC-01)

- REST API endpoint `POST /api/v1/messages`
- Request/response DTOs
- Field-level validation (required, format, range)
- Message ID generation
- Message persistence
- Error response with Persian messages
- Structured logging with correlation ID
- OAuth 2.0 authentication integration

### Out of Scope (Other Epics)

| Concern | Handled By |
|---------|------------|
| Institution master data CRUD | EPIC-03 |
| Institution existence validation | EPIC-03 |
| Message Definition management | EPIC-02 |
| Message Definition lookup | EPIC-02 |
| Actual SWIFT/SEPA transmission | Future epic |

### System Context

```
                    ┌─────────────────┐
                    │  International  │
                    │      Team       │
                    │   (Consumer)    │
                    └────────┬────────┘
                             │
                             │ OAuth 2.0 Token
                             │ POST /api/v1/messages
                             ▼
┌────────────────────────────────────────────────────────┐
│                                                        │
│               Message Creation Service                 │
│                      (EPIC-01)                         │
│                                                        │
│  - Validates request                                   │
│  - Looks up institutions (calls EPIC-03)              │
│  - Looks up message definitions (calls EPIC-02)       │
│  - Persists message                                    │
│  - Returns result                                      │
│                                                        │
└────────────────────────────────────────────────────────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
              ▼              ▼              ▼
        ┌──────────┐  ┌──────────┐  ┌──────────┐
        │  EPIC-02 │  │  EPIC-03 │  │ PostgreSQL│
        │ Message  │  │Institution│  │ Database │
        │Definition│  │ Master   │  │          │
        └──────────┘  └──────────┘  └──────────┘
```

---

## Data Flow

### Happy Path (Valid Message)

```
Client Request
     │
     ▼
┌─────────────────┐
│ MessageController│
│ - Extract token  │
│ - Parse request  │
└────────┬────────┘
         │
         ▼
┌─────────────────────┐
│ MessageValidationService │
│ - Validate required fields │
│ - Validate formats    │
│ - Validate ranges     │
└────────┬────────────┘
         │ ValidationResult = SUCCESS
         ▼
┌─────────────────────┐
│ MessageCreationService │
│ - Lookup sender institution (EPIC-03) [blocked by TQ-1]
│ - Lookup receiver institution (EPIC-03) [blocked by TQ-1]
│ - Lookup Message Definition (EPIC-02) [blocked by TQ-2]
│ - Generate Message ID │
└────────┬────────────┘
         │
         ▼
┌─────────────────────┐
│ MessageRepository   │
│ - Save Message entity │
│ - Status = DRAFT     │
└────────┬────────────┘
         │
         ▼
┌─────────────────────┐
│ Response (200 OK)   │
│ - messageId         │
│ - status = Draft    │
│ - validationResult  │
└─────────────────────┘
```

### Error Path (Validation Failed)

```
Client Request
     │
     ▼
┌─────────────────────┐
│ MessageValidationService │
│ - Field validation fails │
└────────┬────────────┘
         │ ValidationResult = FAILED
         │ validationErrors = [...]
         ▼
┌─────────────────────┐
│ MessageCreationService │
│ - Generate Message ID │
│ - Build error response │
└────────┬────────────┘
         │
         ▼
┌─────────────────────┐
│ MessageRepository   │
│ - Save Message entity │
│ - Status = VALIDATION_FAILED │
│ - validationErrors = JSONB │
└────────┬────────────┘
         │
         ▼
┌─────────────────────┐
│ Response (400 Bad Request) │
│ - messageId = null   │
│ - status = Validation Failed │
│ - validationErrors  │
└─────────────────────┘
```

### Error Path (Unauthorized)

```
Client Request (no/invalid token)
     │
     ▼
┌─────────────────────┐
│ Spring Security     │
│ - Reject request    │
└────────┬────────────┘
         │
         ▼
┌─────────────────────┐
│ Response (401 Unauthorized) │
└─────────────────────┘
```

---

## Testing Strategy

| Layer | Responsibility | Target Coverage |
|-------|----------------|-----------------|
| **Unit Tests** | Business logic, validation rules, mappers | 80% |
| **Integration Tests** | Repository layer, service integration, DB operations | 70% |
| **Contract Tests** | API contract verification with Pact | All endpoints |
| **E2E Tests** | Full flow from request to response | Critical paths |

---

## Unit Tests

### MessageValidationServiceTest

| Test | Description | Expected Result |
|------|-------------|-----------------|
| `validate_allFieldsPresent_returnsSuccess` | All required fields provided | ValidationResult.SUCCESS |
| `validate_missingCurrency_returnsFailed` | Currency field null | ValidationResult.FAILED, MSG-001 |
| `validate_missingMessageType_returnsFailed` | MessageType null | ValidationResult.FAILED, MSG-001 |
| `validate_invalidAmountZero_returnsFailed` | Amount = 0 | ValidationResult.FAILED, MSG-001 |
| `validate_invalidAmountNegative_returnsFailed` | Amount = -100 | ValidationResult.FAILED, MSG-001 |
| `validate_invalidCurrencyCode_returnsFailed` | Currency = "INVALID" | ValidationResult.FAILED, MSG-001 |
| `validate_invalidValueDateFormat_returnsFailed` | ValueDate = "2026/07/25" | ValidationResult.FAILED, MSG-001 |
| `validate_invalidNetwork_returnsFailed` | Network = "INVALID" | ValidationResult.FAILED, MSG-003 |
| `validate_validDateFormat_returnsSuccess` | ValueDate = "2026-07-25" | ValidationResult.SUCCESS |

### MessageCreationServiceTest

| Test | Description | Expected Result |
|------|-------------|-----------------|
| `createMessage_validRequest_returnsDraftStatus` | Valid input, all deps pass | MessageResponse.status = DRAFT |
| `createMessage_validationFailed_returnsValidationFailedStatus` | Validation fails | MessageResponse.status = VALIDATION_FAILED |
| `createMessage_generatesUniqueMessageId` | Multiple requests | Each gets unique messageId |
| `createMessage_messageIdFormat_correct` | Any request | Format: MSG-YYYYMMDD-NNNNNN |

### MessageControllerTest

| Test | Description | Expected Result |
|------|-------------|-----------------|
| `postMessages_validRequest_returns200` | Valid JSON request | HTTP 200 |
| `postMessages_invalidJson_returns400` | Malformed JSON | HTTP 400 |
| `postMessages_missingContentType_returns415` | No Content-Type header | HTTP 415 |

---

## Integration Tests

### MessageRepositoryIntegrationTest

| Test | Description | Expected Result |
|------|-------------|-----------------|
| `save_validMessage_persistsToDatabase` | Save Message entity | Entity persisted with generated ID |
| `save_validationErrors_storedAsJsonb` | Save with JSONB errors | Errors retrievable as JSON |
| `findByMessageId_existingId_returnsMessage` | Query by messageId | Message entity returned |

### MessageCreationIntegrationTest

| Test | Description | Expected Result |
|------|-------------|-----------------|
| `createMessage_fullFlow_persistsToDatabase` | POST /api/v1/messages | Message in DB with DRAFT status |
| `createMessage_validationFailed_persistsErrors` | Invalid amount | Message in DB with errors |
| `createMessage_withOAuth_acceptsValidToken` | Valid OAuth token | Request processed |
| `createMessage_withoutOAuth_returns401` | No token | HTTP 401 |

---

## End-to-End Tests

### MessageCreationE2ETest

| Test | Description | Expected Result |
|------|-------------|-----------------|
| `createMessage_validRequest_allStepsPass` | Full HTTP request | 200 OK, message created |
| `createMessage_invalidAmount_errorResponse` | Amount = 0 | 400 Bad Request, Persian error |
| `createMessage_unsupportedNetwork_errorResponse` | Network = INVALID | 400 Bad Request, MSG-003 |

---

## Acceptance Criteria Mapping

### AC-001: Create Valid Message

**PRD Reference:** AC-001

#### Test Strategy

```java
@Test
void createMessage_validRequest_returnsDraftStatus() {
    // GIVEN: valid input with all required fields
    // AND: Message Definition exists and is active for MT200/SWIFT
    // AND: sender/receiver institutions are valid
    var request = MessageRequest.builder()
        .messageType("200")
        .network("SWIFT")
        .requestReference("REQ-10001")
        .transactionReference("TRX-458796")
        .valueDate("2026-07-25")
        .currency("USD")
        .amount(BigDecimal.valueOf(25000))
        .senderInstitutionIdentifier("BANK01")
        .receiverInstitutionIdentifier("BANK02")
        .build();

    // WHEN: POST /api/v1/messages is called with valid OAuth token
    var response = mockMvc.perform(post("/api/v1/messages")
        .header("Authorization", "Bearer " + validToken)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());

    // THEN: message is created with status "Draft"
    // AND: unique messageId is returned
    // AND: validationResult is "Success"
    assertThat(response.getStatus()).isEqualTo("Draft");
    assertThat(response.getValidationResult()).isEqualTo("Success");
    assertThat(response.getMessageId()).isNotNull();
}
```

#### Design Approach

1. **Controller layer**: `MessageController` receives request, delegates to service
2. **Service layer**: `MessageCreationService` orchestrates validation and persistence
3. **Validation**: `MessageValidationService` performs field validation
4. **External lookups**: Call EPIC-02 and EPIC-03 repositories (mocked/stubbed)
5. **Persistence**: `MessageRepository` saves entity

#### Implementation Approach

1. **Red**: Write failing test expecting Draft status
2. **Green**: Implement controller → service → repository flow
3. **Refactor**: Extract validation logic, clean up mappers

---

### AC-002: Validation Failure - Invalid Amount

**PRD Reference:** AC-002

#### Test Strategy

```java
@Test
void createMessage_amountZero_returnsValidationError() {
    // GIVEN: input with amount <= 0
    var request = validRequestBuilder()
        .amount(BigDecimal.ZERO)
        .build();

    // WHEN: POST /api/v1/messages is called
    var response = mockMvc.perform(post("/api/v1/messages")
        .content(objectMapper.writeValueAsString(request)))
        .andReturn();

    // THEN: message is NOT created
    // AND: status is "Validation Failed"
    // AND: validationErrors contains error code MSG-001
    // AND: error message is Persian
    assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    var body = response.getBody();
    assertThat(body.getStatus()).isEqualTo("Validation Failed");
    assertThat(body.getValidationErrors()).hasSize(1);
    assertThat(body.getValidationErrors().get(0).getCode()).isEqualTo("MSG-001");
    assertThat(body.getValidationErrors().get(0).getMessage())
        .isEqualTo("مبلغ باید بزرگتر از صفر باشد");
}
```

#### Design Approach

1. **Validation layer**: `MessageValidationService` checks amount > 0
2. **Error mapping**: Map to `ValidationError` with MSG-001 code
3. **Response building**: Include Persian message from `ErrorMessageProvider`

#### Implementation Approach

1. **Red**: Write failing test expecting MSG-001 error
2. **Green**: Add amount validation in `MessageValidationService`
3. **Refactor**: Extract amount validation to dedicated validator class

---

### AC-003: Missing Required Field

**PRD Reference:** AC-003

#### Test Strategy

```java
@Test
void createMessage_missingCurrency_returnsValidationError() {
    // GIVEN: input missing currency field
    var request = validRequestBuilder()
        .currency(null)
        .build();

    // WHEN: POST /api/v1/messages is called
    var response = mockMvc.perform(post("/api/v1/messages")
        .content(objectMapper.writeValueAsString(request)));

    // THEN: response status is 400
    // AND: validationErrors includes code MSG-001
    assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().getValidationErrors())
        .anyMatch(e -> e.getCode().equals("MSG-001"));
}
```

#### Design Approach

1. **Bean Validation**: Use `@NotNull` annotations on required fields
2. **Custom validator**: `MessageRequestValidator` for complex validation
3. **Error collection**: Collect all missing fields in single response

#### Implementation Approach

1. **Red**: Write failing test for missing currency
2. **Green**: Add `@NotNull` annotation and validation handler
3. **Refactor**: Consolidate required field validation

---

### AC-004: Unsupported Network

**PRD Reference:** AC-004

#### Test Strategy

```java
@Test
void createMessage_invalidNetwork_returnsValidationError() {
    // GIVEN: network value "INVALID_NETWORK"
    var request = validRequestBuilder()
        .network("INVALID_NETWORK")
        .build();

    // WHEN: POST /api/v1/messages is called
    var response = mockMvc.perform(post("/api/v1/messages")
        .content(objectMapper.writeValueAsString(request)));

    // THEN: response status is 400
    // AND: validationErrors contains code MSG-003
    assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().getValidationErrors().get(0).getCode())
        .isEqualTo("MSG-003");
    assertThat(response.getBody().getValidationErrors().get(0).getMessage())
        .isEqualTo("شبکه انتخاب‌شده پشتیبانی نمی‌شود");
}
```

#### Design Approach

1. **Enum validation**: Network enum only accepts SWIFT, SEPA
2. **Exception handling**: `MethodArgumentTypeMismatchException` handler
3. **Error mapping**: Map to MSG-003 with Persian message

#### Implementation Approach

1. **Red**: Write failing test for invalid network
2. **Green**: Add enum validation and exception handler
3. **Refactor**: Move to global validation error handler

---

### AC-007: Unauthorized Access

**PRD Reference:** AC-007

#### Test Strategy

```java
@Test
void createMessage_noToken_returns401() {
    // GIVEN: request without valid OAuth token
    var request = validRequest();

    // WHEN: POST /api/v1/messages is called
    var response = mockMvc.perform(post("/api/v1/messages")
        .content(objectMapper.writeValueAsString(request)));

    // THEN: response status is 401
    assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
}
```

#### Design Approach

1. **Spring Security**: Configure OAuth2 resource server
2. **Token validation**: Validate JWT against SSO public key
3. **Error response**: Standard 401 response

#### Implementation Approach

1. **Red**: Write failing test expecting 401
2. **Green**: Configure Spring Security with OAuth2
3. **Refactor**: Extract security configuration

---

## Implementation Plan

### Phase 1: Project Setup & Domain Models

| Step | Description | Test First |
|------|-------------|------------|
| 1.1 | Create Spring Boot project structure | - |
| 1.2 | Add dependencies (JPA, Validation, Security, OpenAPI) | - |
| 1.3 | Create enums (MessageType, Network, MessageStatus, ErrorCode) | Unit tests for enum values |
| 1.4 | Create Message entity | Unit test for entity mapping |
| 1.5 | Create DTOs (MessageRequest, MessageResponse, ValidationError) | Unit tests for DTO mapping |
| 1.6 | Create Flyway migration for messages table | Integration test for schema |

### Phase 2: Validation Layer (AC-002, AC-003, AC-004)

| Step | Description | Test First |
|------|-------------|------------|
| 2.1 | Write failing tests for field validation | ✅ AC-002, AC-003 tests |
| 2.2 | Implement `MessageValidationService` | Make tests pass |
| 2.3 | Add amount validation (MSG-001) | AC-002 test |
| 2.4 | Add required field validation (MSG-001) | AC-003 test |
| 2.5 | Add network validation (MSG-003) | AC-004 test |
| 2.6 | Implement Persian error messages | Verify messages |
| 2.7 | Refactor to dedicated validators | Maintain test coverage |

### Phase 3: Repository Layer

| Step | Description | Test First |
|------|-------------|------------|
| 3.1 | Write failing repository tests | Integration tests |
| 3.2 | Create `MessageRepository` interface | Make tests pass |
| 3.3 | Implement custom query for messageId lookup | Integration test |
| 3.4 | Test JSONB serialization of validationErrors | Integration test |

### Phase 4: Service Layer (AC-001)

| Step | Description | Test First |
|------|-------------|------------|
| 4.1 | Write failing service tests | ✅ AC-001 test |
| 4.2 | Implement `MessageCreationService.createMessage()` | Make tests pass |
| 4.3 | Implement Message ID generator | Unit test for format |
| 4.4 | Add logging with correlation ID | Verify log output |
| 4.5 | Create stubs for EPIC-02, EPIC-03 repositories | Mock-based tests |

### Phase 5: Controller Layer (AC-001, AC-007)

| Step | Description | Test First |
|------|-------------|------------|
| 5.1 | Write failing controller tests | ✅ AC-001, AC-007 tests |
| 5.2 | Implement `MessageController` | Make tests pass |
| 5.3 | Configure Spring Security OAuth2 | AC-007 test |
| 5.4 | Add global exception handler | Unit test |
| 5.5 | Configure OpenAPI documentation | Manual verification |

### Phase 6: Integration & E2E

| Step | Description | Test First |
|------|-------------|------------|
| 6.1 | Write integration tests with Testcontainers | Integration tests |
| 6.2 | Write E2E tests for critical paths | E2E tests |
| 6.3 | Write Pact contract tests | Contract tests |
| 6.4 | Performance test for 100 concurrent requests | Load test |

---

## Technical Questions

### Technical Questions — All Resolved ✅

---

## Risks

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| EPIC-02/EPIC-03 not ready | High | High | Use stub implementations |
| Currency decimal validation complex | Medium | Medium | Use established library (Moneta) |
| OAuth integration issues | Medium | High | Early integration testing with SSO team |
| Performance under 100 concurrent requests | Low | Medium | Load testing in Phase 6 |
| Persian character encoding | Low | Low | UTF-8 throughout |

---

## Decision Log

| # | Decision | Rationale | Date |
|---|----------|-----------|------|
| 1 | Use standard Spring Boot layered architecture | Matches PRD decision, team familiarity | 2026-07-26 |
| 2 | Generate Message ID in service layer | Centralized logic, easier testing | 2026-07-26 |
| TQ-1 | Use stub/fake for InstitutionRepository, swap when EPIC-03 ready | Allows parallel development, no EPIC-01 rework needed | 2026-07-26 |
| TQ-2 | Use stub/fake for MessageDefinitionRepository, swap when EPIC-02 ready | Same pattern, consistent approach | 2026-07-26 |
| TQ-3 | MSG-YYYYMMDD-NNNNNN (sequential daily) | Matches PRD example, human-readable | 2026-07-26 |
| TQ-4 | Collect all validation errors and return them together | Better UX, user sees all issues at once | 2026-07-26 |
| TQ-5 | Java Enum with code and message fields for Persian errors | Type-safe, compile-time checked, no external dependencies | 2026-07-26 |

---

## Iteration History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-07-26 | Initial spec generated from EPIC-01 requirements |
| 1.1 | 2026-07-26 | Resolved TQ-1 to TQ-5, confidence raised from 75% to 95% |

---

## Next Steps

1. ~~**Answer Technical Questions** TQ-1 through TQ-5~~ ✅ Complete
2. **Review and approve** this specification
3. **Begin implementation** following the red-green-refactor cadence in Implementation Plan
