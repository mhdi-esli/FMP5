# Technical Specification: EPIC-01 Message Creation Service

## Metadata

| Field | Value |
|---|---|
| Epic | EPIC-01: Message Creation Service |
| Epic slug | `epic-01-message-creation-service` |
| Source PRD | `brainstorm/Epic_PRD.md` |
| Scoped requirements | FR-001, FR-002, FR-003, FR-004, FR-005, FR-009, FR-010, FR-011, FR-012, FR-014, NFR-001, NFR-002, NFR-003, NFR-004, NFR-005, NFR-006 |
| Scoped acceptance criteria | AC-001, AC-002, AC-003, AC-004, AC-007 |
| PRD Confidence Level | 88% |
| Initially generated | 2026-07-26 |
| Updated | 2026-08-19 |

## Confidence Level

**Confidence Level: 100% — 5/5 ACs resolved (100%) − 0 points for open high-impact TQs.**

All mapped criteria have an actionable test, design, and implementation approach. TQ-9 selects client-identity allowlisting through the JWT `aud`/`azp` claim instead of fine-grained scope authorization; the authorization-model divergence from the applicable standards is retained as a delivery risk. TQ-11 defines a 500,000-row interim performance dataset; its comparison basis and final capacity/operations confirmation remain delivery risks rather than unresolved AC blockers.

## Architecture

EPIC-01 owns the synchronous message-creation entry point and its orchestration. It follows the strict layered flow `controller → service → repository`; controllers contain no business logic, services may compose through direct injection, and dependencies may not call upward or skip layers (`.claude/_architecture-reference.md`, **System Architecture** and **Development Patterns**).

The selected standards-compliant HTTP surface is `POST /v1/messages`, with a `ResponseEnvelope` carrying `resultData`, `message`, and non-null `errorList`; errors use issuer `FMP` and integer codes from 201 upward. This resolves the scoped PRD’s legacy `/api/v1/messages` and `MSG-XXX` contract in favor of the applicable standards (`.claude/_architecture-reference.md`, **Major Design Decisions**; `.claude/_coding-guidelines.md`, **Response Envelope**). The controller, response mapping, error mapping, and HTTP tests implement and assert that selected contract.

The service performs this ordered flow:

1. Receive a deserialized request from the controller.
2. Validate required fields, amount, currency, date, message type, and network.
3. Resolve the active message definition through the EPIC-02 service boundary.
4. Validate sender and receiver through the EPIC-03 service boundary.
5. Allocate the daily sequential message ID with a PostgreSQL-backed daily counter updated in the message transaction.
6. Persist the result transactionally: valid requests create a `Draft` business message, while validation failures persist a `Validation Failed` attempt and never create a `Draft` business message.
7. Emit a structured audit event containing timestamp and correlation/trace ID without logging the request or response body.
8. Return the service result for HTTP response mapping.

OAuth 2.0 resource-server authentication is enforced before controller execution. Tokens are verified for signature and expiry against the configured Dotin SSO JWKS source. Rather than fine-grained scope authorization, FMP permits service-to-service message creation only when the JWT `aud` or `azp` claim identifies an allowlisted trusted client; tokens issued to other clients are rejected. This answered TQ-9 decision differs from the required-scope standard in `.claude/_architecture-reference.md`, **Major Design Decisions — Security**, and is retained as a delivery risk requiring standards-owner review.

The current EPIC-01 runtime baseline remains Java 21 and Spring Boot 3.3.2. Java 25, Spring Boot 4.1.0, Redis, and the retroactive standards migration belong to EPIC-04 (`brainstorm/Epic_PRD.md`, **Epic Breakdown — EPIC-04**; `.claude/_architecture-reference.md`, **Constraints**).

## Components

| Component | Responsibility | Exposed interface | Consumed interface |
|---|---|---|---|
| `MessageController` | Accept the authenticated creation request and map the service result to HTTP | Creation endpoint | `MessageCreationService` |
| `MessageCreationRequest` record | Carry the 18 PRD input fields and boundary validation metadata without exposing entities | JSON request DTO | Bean Validation and Jackson |
| `MessageCreationResponse` record | Carry message ID, type, network, status, creation time, validation result, and errors | Response payload | Service result and error mapper |
| `ResponseEnvelope<T>` and `ErrorItem` | Represent the standards-compliant response surface if selected | `resultData`, `message`, `errorList` | Controller and exception handler |
| `MessageCreationService` | Orchestrate validation, dependent services, ID allocation, persistence, and audit emission in a transaction | `createMessage(MessageCreationRequest)` | Validation, definition, institution, ID, repository, audit components |
| `MessageValidationService` | Return all field-validation failures in deterministic request-field order and apply platform-configured or database-backed ISO 4217 minor units | `validate(MessageCreationRequest)` | Currency minor-unit configuration/table |
| `MessageDefinitionService` | Resolve an active definition by message type and network | Existing EPIC-02 service operation | EPIC-02 repository/cache internals |
| `InstitutionService` | Validate sender and receiver compatibility | Existing EPIC-03 service operations | EPIC-03 repository internals |
| `MessageIdGenerator` | Allocate `MSG-YYYYMMDD-NNNNNN` identifiers safely under concurrency | `nextId(LocalDate)` | PostgreSQL-backed daily counter in the message transaction |
| `MessageRepository` | Persist and retrieve `Message` entities | Spring Data repository operations | PostgreSQL |
| `MessageAuditLogger` | Emit one structured event per attempted creation with timestamp, outcome, and trace/correlation ID | `recordAttempt(...)` | SLF4J/MDC and OpenTelemetry trace context |
| `MessageExceptionHandler` | Map validation and application failures consistently | Global exception handling | Error-code/message definitions |
| `SecurityConfiguration` | Configure OAuth 2.0 resource-server verification and trusted-client authorization | Spring Security filter chain | Configured SSO metadata/JWK source; trusted-client allowlist evaluated against JWT `aud`/`azp` |

`MessageCreationRequest` carries: `messageType`, `network`, `requestReference`, `transactionReference`, `relatedReference`, `valueDate`, `currency`, `amount`, `senderInstitutionIdentifier`, `senderBic`, `senderBranchId`, `receiverInstitutionIdentifier`, `receiverBic`, `receiverBranchId`, `chargeType`, `instructionCode`, `narrative`, and `additionalInformation`. The nine mandatory fields are exactly those listed by FR-002.

DTOs are Java records and entities are never exposed (`.claude/_coding-guidelines.md`, **Coding Standards** and **Design Patterns**). Money uses `BigDecimal`, enums serialize as strings, JSON fields use `camelCase`, and dates use ISO 8601 (`.claude/_coding-guidelines.md`, **SWA_101 compliance**).

## Dependencies

### Internal dependencies

| Dependency | Source | Use in EPIC-01 |
|---|---|---|
| EPIC-02 Message Definition Management | AC-001 precondition; PRD EPIC-02 | Confirm an active MT200/network definition through `MessageDefinitionService` |
| EPIC-03 Institution Management | AC-001 precondition; PRD EPIC-03 | Validate sender and receiver through `InstitutionService` |
| Global exception handling | `.claude/_architecture-reference.md`, Development Patterns | Convert boundary and service failures to the selected response contract |
| OpenTelemetry trace context | `.claude/_architecture-reference.md`, Major Design Decisions | Supply the correlation value used by structured audit/error logs |

The AC-001 references to EPIC-02 and EPIC-03 are in scope as integration preconditions, but their management behavior and persistence internals remain owned by those epics.

### External dependencies

| Dependency | Sourced version | Purpose |
|---|---|---|
| Java | 21 (current baseline) | Runtime and `BigDecimal`/date/currency primitives |
| Spring Boot | 3.3.2 (current baseline) | Web, validation, security, and persistence framework |
| PostgreSQL with Flyway | Project baseline | Transactional message persistence and schema evolution |
| Spring Security OAuth2 Resource Server | Managed by Spring Boot | Dotin SSO token verification |
| SpringDoc OpenAPI | 2.6.0 | Generate OpenAPI 3.x documentation |
| JUnit 5 and Mockito | Project testing standard | Unit and slice tests |
| Testcontainers | 1.20.1 | PostgreSQL integration tests |
| Pact | Version not fixed by the scoped sources | Consumer contract verification |
| SLF4J/Logback and ELK | Project observability decision | Structured audit and error logging |

ISO 4217 minor units are maintained in platform configuration or a database table; no external monetary library is selected. Caffeine/Redis behavior is outside EPIC-01 and belongs to EPIC-02/EPIC-04.

## Boundaries

### In scope

- One authenticated synchronous MT200 creation operation.
- Acceptance of all 18 FR-001 input fields and mandatory-field validation for the nine FR-002 fields.
- Positive amount, currency decimal, ISO 4217, and `YYYY-MM-DD` validation.
- SWIFT and SEPA request values.
- Coordination with EPIC-02 and EPIC-03 through their service interfaces for AC-001.
- Daily sequential message-ID generation in `MSG-YYYYMMDD-NNNNNN` format.
- Transactional persistence: valid requests create `Draft` messages; validation failures persist `Validation Failed` attempts without creating a `Draft` business message.
- Persian validation errors, using the selected `ResponseEnvelope` and `FMP` error representation.
- OAuth 2.0 authentication, trusted-client authorization via the JWT `aud`/`azp` allowlist, structured logging, correlation/trace propagation, and attempt auditing.
- SpringDoc-generated OpenAPI documentation for `POST /v1/messages` and its referenced schemas.
- Validation response time and concurrency verification: p95 `<500ms` with 100 concurrent requests in a dedicated production-representative performance environment, after discarding the first 1,000 requests or two minutes of sustained load, whichever occurs first, against a 500,000-row interim dataset. Capacity/operations must confirm the final expected volume before treating the result as a final production-capacity certification.

### Out of scope

- Message-definition CRUD, activation, mapping, and cache ownership (EPIC-02).
- Institution CRUD and master-data ownership (EPIC-03).
- Java 25, Spring Boot 4.1.0, Redis migration, or retrofit verification (EPIC-04).
- Business checks for balances, transfer limits, or authorization.
- Transmission to SWIFT/SEPA, delivery tracking, retries, webhooks, and message history/versioning.
- Message types other than MT200.
- Request-payload signing, which the architecture reference assigns to a later epic.

### System-context edges

An authenticated trusted business system calls the creation endpoint. Spring Security calls the configured SSO metadata/JWK source to verify bearer tokens and authorizes a service caller only when its JWT `aud` or `azp` matches the trusted-client allowlist. The service calls EPIC-02 and EPIC-03 service interfaces and PostgreSQL; it emits sanitized structured events to the existing ELK path. It does not call a financial network.

## Data Flow

### Successful creation

1. A client sends all required business data and a bearer token to `POST /v1/messages`.
2. Spring Security verifies the token signature against the configured Dotin SSO JWKS endpoint, validates `exp`, and permits message creation only if the JWT `aud` or `azp` claim matches the trusted-client allowlist.
3. The request DTO is validated, including ISO 4217 minor units held in platform configuration or a database table.
4. `MessageCreationService` resolves the active definition and validates both institutions.
5. `MessageIdGenerator` atomically updates a PostgreSQL-backed daily counter in the message transaction.
6. A transaction persists one message with status `Draft`, validation result `Success`, and a unique message ID.
7. `MessageAuditLogger` emits a success event with timestamp and the current trace/correlation ID; no request or response body is logged.
8. The controller returns HTTP 200 in `ResponseEnvelope`, with `resultData`, `message`, and non-null `errorList`.

### Validation failure

1. Validation collects all detectable field failures in deterministic field order, preserving the prior TQ-4 decision.
2. The error mapper selects the applicable Persian message; a missing `currency` field uses `نوع ارز مشخص نشده است`.
3. The service persists a `Validation Failed` attempt but never creates a `Draft` business message for a validation failure.
4. The audit logger emits a failed-validation attempt with timestamp, trace/correlation ID, and non-sensitive error identifiers.
5. The controller returns HTTP 400 in the selected `ResponseEnvelope` and `FMP` error representation.

### Authentication and authorization failure

1. The security filter rejects a missing, malformed, expired, or invalid-signature bearer token before controller invocation with HTTP 401.
2. A valid token whose JWT `aud`/`azp` does not match the trusted-client allowlist is rejected with HTTP 403.
3. The request-level audit path emits an unauthorized-attempt event with timestamp and trace/correlation ID without recording the body.
4. The global security error mapping returns the selected envelope representation.

### Transaction and error path

Message-ID allocation and successful persistence occur in one service transaction (`brainstorm/Epic_PRD.md`, NFR-004). The PostgreSQL daily-counter update participates in that transaction, so a persistence failure rolls back both. The global exception handler produces the selected envelope/error representation, and structured error logging includes the trace/correlation ID but excludes full bodies (`.claude/_coding-guidelines.md`, **Hard Rules**).

## Testing Strategy

Follow failing test → minimum production code → behavior-preserving refactor; never weaken a test to pass (`.claude/_coding-guidelines.md`, **Testing Practices**).

- **Unit:** Mockito-based service tests cover validation orchestration, all field rules, dependent-service outcomes, audit invocation, platform-configured/database-backed minor units, and PostgreSQL daily-counter ID formatting/allocation.
- **Controller slice:** `@WebMvcTest` covers deserialization, authentication, status codes, exact Persian messages, and the selected `ResponseEnvelope`/`FMP` error contract.
- **Integration:** Testcontainers with real PostgreSQL covers transaction boundaries, `Validation Failed` attempt persistence, uniqueness, daily-counter rollback, and concurrent creation. No database mocks are used for these assertions.
- **Contract:** Pact covers `POST /v1/messages`, request fields, success response, validation response, and authentication/authorization failures.
- **End-to-end:** The running service exercises the five mapped criteria with real HTTP/security filters and PostgreSQL. EPIC-02/03 preconditions use deployed test fixtures or their real service implementations, not controller-layer bypasses.
- **Conformance:** A shared fixture checks `resultData`, `message`, and non-null `errorList` on the endpoint.
- **Performance:** Test 100 concurrent creation requests against p95 `<500ms` in a dedicated environment matching production instance size, DB tier, and network topology. Discard the first 1,000 requests or two minutes of sustained load, whichever occurs first. Pre-seed 500,000 rows as the interim dataset; confirm the final expected volume with capacity/operations before treating the result as a final production-capacity certification.
- **Security:** Verify no token, malformed token, invalid signature, and expired token return 401; verify a valid token whose JWT `aud`/`azp` is absent from the trusted-client allowlist returns 403; verify logs do not contain request/response bodies or bearer tokens.
- **Coverage and quality:** The suite contributes to the project-wide 80% line gate and passes Checkstyle, Spotless, JaCoCo, and ArchUnit checks (`.claude/_coding-guidelines.md`, **Coding Standards** and **Testing Practices**).

## Unit Tests

- `validate_allMandatoryFieldsPresent_returnsNoRequiredFieldErrors`
- `validate_eachMandatoryFieldMissing_returnsFmpInvalidInputErrorForThatField`
- `validate_amountZero_returnsInvalidAmountError`
- `validate_amountNegative_returnsInvalidAmountError`
- `validate_amountExceedsConfiguredMinorUnits_returnsConfiguredError`
- `validate_unknownCurrency_returnsInvalidCurrencyError`
- `validate_nonIsoValueDate_returnsInvalidDateError`
- `validate_unsupportedNetwork_returnsFmpUnsupportedNetworkError`
- `validate_multipleInvalidFields_returnsAllErrorsInFieldOrder`
- `createMessage_validDependencies_persistsDraftAndAuditsSuccess`
- `createMessage_validationFailure_persistsFailedAttemptWithoutDraft`
- `createMessage_dependencyValidationFails_returnsValidationFailure`
- `nextId_concurrentAllocation_returnsUniqueDailySequence`
- `audit_attempt_omitsSensitivePayloadAndIncludesTraceId`

## Integration Tests

- Testcontainers persists a valid message with all 18 fields, `Draft`, `Success`, and the generated message ID.
- Testcontainers verifies validation-error JSONB round trips for persisted failed attempts.
- A forced repository failure rolls back the PostgreSQL daily-counter update and message persistence.
- Concurrent valid creates produce unique IDs and no lost daily-counter allocations.
- Valid EPIC-02/03 fixtures satisfy AC-001; inactive/missing dependent data cannot produce a draft.
- Spring Security accepts a valid signed, unexpired token whose JWT `aud` or `azp` claim matches a trusted client on the configured allowlist.
- Spring Security rejects missing, expired, and invalid-signature tokens with 401, and a valid token whose JWT `aud`/`azp` is not allowlisted with 403.
- SpringDoc exposes `POST /v1/messages` and referenced request/response schemas.

## End-to-End Tests

- Submit the AC-001 request through the running application with an allowlisted `aud`/`azp` client and assert 200, `Draft`, unique message ID, `Success`, persistence, audit correlation, and the selected envelope.
- Submit amount `0` and a negative amount and assert the AC-002 HTTP/error behavior and persisted `Validation Failed` attempt without a `Draft` business message.
- Omit `currency` and assert AC-003, including `نوع ارز مشخص نشده است`.
- Submit `INVALID_NETWORK` and assert AC-004, including `شبکه انتخاب‌شده پشتیبانی نمی‌شود`.
- Submit no token and invalid tokens and assert 401; submit a valid token with a non-allowlisted `aud`/`azp` and assert 403.
- Pre-seed 500,000 rows, then run 100 concurrent valid creates using the TQ-11 performance environment and warm-up; assert p95 `<500ms` and record that the result uses the interim dataset assumption.

## Acceptance Criteria Mapping

### AC-001: Create Valid Message

**Requirements satisfied:** FR-001, FR-002, FR-003, FR-004, FR-005, FR-009, FR-010, FR-012; supported by FR-014 and NFR-001–NFR-006.

**Test strategy**

Start with a coherent failing set: a service unit test supplies a request containing all mandatory fields, an active MT200/SWIFT definition, and valid institutions, then asserts one `Draft` entity is saved, a unique correctly formatted ID is returned, validation result is `Success`, and a correlated success audit event is emitted. A controller slice test supplies a valid signed, unexpired JWT whose `aud` or `azp` claim is on the trusted-client allowlist and asserts HTTP 200 plus the `ResponseEnvelope` contract. A Testcontainers integration test proves transactional PostgreSQL daily-counter allocation and persistence uniqueness under concurrent allocation. These tests fail before implementation because no orchestrated validation, allocation, transaction, response mapping, or audit path exists.

**Design approach**

`MessageController` delegates to `MessageCreationService`; the service calls `MessageValidationService`, `MessageDefinitionService`, and `InstitutionService` in order, allocates the ID through a PostgreSQL-backed daily counter in the transaction, persists, and audits the outcome. Strict downward layering and service composition follow `.claude/_architecture-reference.md`, **System Architecture**. The controller maps the result to the selected `/v1/messages` `ResponseEnvelope` contract. Spring Security grants endpoint access only to signed, unexpired tokens whose `aud` or `azp` claim matches the trusted-client allowlist.

**Implementation approach**

Implement only the record DTO boundary, selected endpoint/error contract, orchestration, transactional PostgreSQL daily-counter allocation and save, service-interface calls, audit event, response mapping, and an `aud`/`azp` trusted-client authorization rule required by the failing tests. Refactor only duplicated test fixtures, pure mappings, and validation composition while preserving all assertions.

### AC-002: Validation Failure - Invalid Amount

**Requirements satisfied:** FR-003, FR-010, FR-011.

**Test strategy**

First write service unit tests for `0` and a negative amount. Each asserts status `Validation Failed`, the invalid-amount error, and the exact Persian message `مبلغ باید بزرگتر از صفر باشد`. Add a controller slice assertion for HTTP 400 and the selected `ResponseEnvelope`/`FMP` error representation. Add a Testcontainers assertion that the failed attempt is persisted without a `Draft` business message. Add minor-unit boundary cases sourced from platform configuration or a database table. The tests fail before implementation because the amount rule, mapping, and persistence behavior are absent.

**Design approach**

`MessageValidationService` compares monetary values with `BigDecimal.ZERO` and never uses binary floating point. It maps non-positive values to the scoped invalid-input error and exact Persian text and reads ISO 4217 minor units from platform configuration or a database table. A validation failure persists a `Validation Failed` attempt and does not create a `Draft` business message. The controller maps the result to the selected external envelope/error representation.

**Implementation approach**

Implement the non-positive comparison, configured/database-backed minor-unit lookup, deterministic validation-error result, and failed-attempt persistence policy first. Map the result using the selected response contract. Refactor only shared validation-result construction once the tests pass.

### AC-003: Missing Required Field

**Requirements satisfied:** FR-002, FR-010, FR-011.

**Test strategy**

Write a controller slice test that omits `currency` and asserts HTTP 400, the selected `FMP` invalid-input code, a field association identifying `currency`, and the Persian required-field message `نوع ارز مشخص نشده است`. Add a parameterized unit test that removes each of the nine FR-002 mandatory fields and asserts one corresponding error. The tests fail before implementation because mandatory-field constraints and exception-to-response mapping are absent.

**Design approach**

Apply Bean Validation annotations that are compatible with record components and use the global exception handler to produce the selected `ResponseEnvelope` contract. The field identity remains `currency`, the exact currency-required Persian message is `نوع ارز مشخص نشده است`, and error collection follows the preserved TQ-4 decision.

**Implementation approach**

Add the minimum record-component constraints, validation handler mapping, and verbatim Persian message lookup required by the tests. Refactor common required-field mappings only after all nine parameterized cases pass.

### AC-004: Unsupported Network

**Requirements satisfied:** FR-011, FR-012.

**Test strategy**

Write a controller slice test posting `network: "INVALID_NETWORK"` and assert HTTP 400, the selected `FMP` representation of the legacy `MSG-003` condition, and exact text `شبکه انتخاب‌شده پشتیبانی نمی‌شود`. Unit tests assert `SWIFT` and `SEPA` pass the network rule and any other value fails. The tests fail before implementation because unsupported values are not yet converted into the required domain validation error.

**Design approach**

Keep the raw network value available to boundary validation so an unsupported string reaches the controlled validation path rather than becoming an unstructured deserialization failure. Convert supported values to the string enum only after validation, and map failure to the selected `ResponseEnvelope`/`FMP` error form.

**Implementation approach**

Implement the smallest supported-value check and map failures to the exact Persian text and selected `FMP` code representation. Ensure the global handler uses the same mapping for malformed boundary input. Refactor shared enum parsing only after SWIFT, SEPA, and invalid-value tests pass.

### AC-007: Unauthorized Access

**Requirements satisfied:** NFR-005.

**Test strategy**

Write controller/security slice tests that omit the bearer token and provide malformed, expired, and invalid-signature tokens. Assert the controller and service are not invoked and HTTP 401 is returned in the selected envelope form. Add a test for a valid token whose `aud`/`azp` does not match the trusted-client allowlist and assert HTTP 403. These tests fail before implementation because the resource-server filter chain and authentication entry point are not configured.

**Design approach**

Use Spring Security OAuth2 Resource Server with the configured Dotin SSO JWKS endpoint. Validate signature and `exp`, never hardcode a public key, and do not log tokens (`.claude/_architecture-reference.md`, **Major Design Decisions — Security**). Authentication failures terminate before controller execution. Authorization is an allowlist comparison of the JWT `aud` or `azp` claim against trusted service client IDs; security errors use the selected response representation. This selection conflicts with the standards requirement to validate required scopes and requires standards-owner review.

**Implementation approach**

Configure the minimum stateless resource-server filter chain, JWKS-based signature/expiry validation, trusted-client `aud`/`azp` allowlist authorization, and authentication entry point to make missing/invalid-token and non-allowlisted-client tests pass. Refactor only reusable security test fixtures after behavior passes.

## Implementation Plan

1. **RED — contract characterization (AC-001–AC-004, AC-007):** add failing controller/Pact tests for `POST /v1/messages`, the `ResponseEnvelope`, and `FMP` error representation.
2. **RED — field validation (AC-002, AC-003, AC-004):** add failing unit tests for mandatory fields, non-positive amounts, configuration/database-backed currency minor units, date/network validation, deterministic error collection, and exact sourced Persian messages.
3. **GREEN — validation boundary (AC-002, AC-003, AC-004):** implement request-record constraints, `MessageValidationService`, global validation mapping, failed-attempt persistence, and the selected response representation.
4. **REFACTOR:** consolidate validation-result construction without introducing validator packages that violate the verified package layout.
5. **RED — security (AC-007, AC-001):** add failing missing/invalid-token 401 tests and a valid-token-with-non-allowlisted-`aud`/`azp` 403 test.
6. **GREEN — security:** configure OAuth2 resource-server JWKS signature/expiry verification, trusted-client `aud`/`azp` allowlist authorization, stateless access rules, and sanitized unauthorized auditing; obtain standards-owner review of the no-fine-grained-scope decision.
7. **RED — creation orchestration (AC-001):** add failing service and Testcontainers tests for dependent-service checks, transactional PostgreSQL daily-counter allocation, `Draft` persistence, response values, and correlated audit logging.
8. **GREEN — creation orchestration:** implement service composition, allocator, repository operation, transaction boundary, and success response.
9. **REFACTOR:** remove duplication in mappings and test fixtures while preserving strict layer dependencies and public behavior.
10. **RED/GREEN — NFR verification:** add structured-log assertions, OpenAPI/Pact conformance, daily-counter rollback tests, and the 100-concurrent-request p95 test after the TQ-11 warm-up against the 500,000-row interim dataset; record the interim assumption with the result.
11. **VERIFY:** run the complete suite and quality gates; inspect the generated OpenAPI output for the selected versioned endpoint and referenced schemas.

This sequence is technical guidance only; `plan-tasks` owns the downstream task breakdown.

## Technical Questions

TQ-1 through TQ-5 exist only as historical answered entries in the preserved Decision Log; the earlier spec did not retain their literal question text, checkbox syntax, impact tags, or blocker lists. They are not treated as open or reparsed into new answers. New IDs therefore begin at TQ-6.

**TQ-6: Which API compatibility contract governs this retroactive EPIC-01 specification? (select one)**

[x] Apply the current standards now: `POST /v1/messages`, `ResponseEnvelope`, and `FMP` integer errors from 201 upward ← recommended
[ ] Preserve the scoped PRD contract: `POST /api/v1/messages`, direct message response, `validationErrors`, and `MSG-XXX` codes until a remediation epic

Blocks: Architecture, Components, Boundaries, Data Flow, Testing Strategy, Integration Tests, End-to-End Tests, AC-001, AC-002, AC-003, AC-004, AC-007, Implementation Plan
Impact: (high-impact)

**TQ-7: What does AC-002 “message is NOT created” mean relative to FR-010’s requirement to store `Validation Failed` messages? (select one)**

[x] Persist a `Validation Failed` attempt but do not create a `Draft` business message ← recommended
[ ] Do not persist a message row; return a response-only failed status and emit only the audit log
[ ] Persist the failed attempt outside the `messages` aggregate

Blocks: Architecture, Data Flow, Unit Tests, Integration Tests, End-to-End Tests, AC-002, Implementation Plan
Impact: (high-impact)

**TQ-8: Which atomic allocation mechanism must implement the daily `MSG-YYYYMMDD-NNNNNN` sequence under 100 concurrent creates? (select one)**

[x] A PostgreSQL-backed daily counter updated in the message transaction ← recommended
[ ] A PostgreSQL sequence combined with the date, accepting that the numeric sequence does not reset daily
[ ] A Redis atomic counter, making EPIC-01 depend on the EPIC-04 Redis baseline

Blocks: Architecture, Components, Data Flow, Unit Tests, Integration Tests, End-to-End Tests, AC-001, Implementation Plan
Impact: (high-impact)

**TQ-9: What exact OAuth 2.0 scope authorizes message creation? (free text)**

Answer: `Token validation: signature verified against JWKS (https://sso.tps.ir/...), exp checked. No fine-grained scope is enforced — the calling service is responsible for business/user-level authorization prior to invocation. FMP restricts callers to a defined allowlist of trusted client IDs (aud/azp claim) authorized for service-to-service message creation; requests from tokens issued to other clients are rejected.`

Answered: authenticate using JWKS signature and `exp` validation, then authorize service-to-service creation only when the JWT `aud` or `azp` claim identifies a client in the trusted-client allowlist. Tokens issued to all other clients receive HTTP 403. This deliberately uses client-identity authorization rather than the fine-grained required scopes mandated by `.claude/_architecture-reference.md`, **Major Design Decisions — Security**; retain the conflict in Risks for standards-owner review.

Blocks: None — folded into Architecture, Components, Boundaries, Data Flow, Testing Strategy, Integration Tests, AC-001, AC-007, and Implementation Plan
Impact: (high-impact)

**TQ-10: Which authoritative minor-unit policy validates amount precision for ISO 4217 currencies? (select one)**

[ ] Use the Java runtime ISO 4217 data through `java.util.Currency.getDefaultFractionDigits()` ← recommended
[x] Maintain currency minor units in platform configuration or a database table
[ ] Use an approved external monetary library (name and version required in the answer)

Blocks: Components, Data Flow, Testing Strategy, Unit Tests, AC-002
Impact: (standard)

**TQ-11: What statistic, environment, dataset, and warm-up conditions define a pass for `<500ms` with 100 concurrent requests? (free text)**

Answer: `Statistic: p95 < 500ms (p99 tracked, non-blocking)
Environment: dedicated performance-test environment matching production instance size, DB tier, network topology
Dataset: 500,000 rows (interim assumption, based on [comparable service/estimate]) — to be confirmed by [capacity/ops owner] before Testing Strategy is finalized. If the confirmed figure differs materially, the performance environment sizing may need revisiting.
Warm-up: discard first 1,000 requests or 2 minutes of sustained load (whichever occurs first) before measurement begins`

Answered: test p95 `<500ms` with 100 concurrent requests in a dedicated environment matching production instance size, DB tier, and network topology. Pre-seed 500,000 rows as the interim dataset, then discard the first 1,000 requests or two minutes of sustained load, whichever occurs first. Record the interim dataset assumption in test evidence. Capacity/operations confirmation remains a delivery risk; it does not block the specified test.

Blocks: None — folded into Boundaries, Testing Strategy, End-to-End Tests, and Implementation Plan
Impact: (standard)

**TQ-12: What exact Persian message must represent a missing `currency` field? (free text)**

Answer: `نوع ارز مشخص نشده است `

Blocks: Data Flow, End-to-End Tests, AC-003
Impact: (standard)

## Risks

| Risk | Source/evidence | Delivery effect | Treatment |
|---|---|---|---|
| Trusted-client allowlisting conflicts with required-scope authorization | Answered TQ-9 authorizes by JWT `aud`/`azp` trusted-client allowlist, while `.claude/_architecture-reference.md`, **Major Design Decisions — Security**, requires validation of scopes | Client identity may not express the least-privilege permission model required by the platform standard | Obtain standards-owner approval for this exception or replace the allowlist rule with the required-scope design before implementation |
| Performance dataset row count is an interim assumption | TQ-11 sets a 500,000-row interim volume based on a comparable service/estimate, pending capacity/operations confirmation | A result at this volume is not a final production-capacity certification if the confirmed volume differs materially | Record the 500,000-row assumption with the evidence; reassess environment sizing and re-run the performance test when capacity/operations confirms a materially different volume |
| Preserved TQ-1/TQ-2 decisions call for repository stubs, while current architecture requires service composition and strict layering | Existing Decision Log versus architecture reference | Reusing old repository coupling would violate current standards | Retain the history, but implement through EPIC-02/03 service interfaces and flag this conflict for human review |
| EPIC-04 changes the runtime and cache baseline later | PRD EPIC-04 | EPIC-01 may require subsequent retrofit verification | Keep current/future baselines clearly separated |

## Decision Log

The following prior entries are preserved verbatim. TQ-9's answered authorization model and TQ-10’s selected minor-unit option diverge from applicable recommendations or standards; their decision-log entries record why those divergences matter.

| # | Decision | Rationale | Date |
|---|----------|-----------|------|
| 1 | Use standard Spring Boot layered architecture | Matches PRD decision, team familiarity | 2026-07-26 |
| 2 | Generate Message ID in service layer | Centralized logic, easier testing | 2026-07-26 |
| TQ-1 | Use stub/fake for InstitutionRepository, swap when EPIC-03 ready | Allows parallel development, no EPIC-01 rework needed | 2026-07-26 |
| TQ-2 | Use stub/fake for MessageDefinitionRepository, swap when EPIC-02 ready | Same pattern, consistent approach | 2026-07-26 |
| TQ-3 | MSG-YYYYMMDD-NNNNNN (sequential daily) | Matches PRD example, human-readable | 2026-07-26 |
| TQ-4 | Collect all validation errors and return them together | Better UX, user sees all issues at once | 2026-07-26 |
| TQ-5 | Java Enum with code and message fields for Persian errors | Type-safe, compile-time checked, no external dependencies | 2026-07-26 |
| TQ-10 | Use a platform configuration or database table for ISO 4217 minor units instead of Java runtime currency data | The selected option differs from the recommendation; implementation must define and maintain the authoritative platform source, avoiding reliance on runtime-specific currency metadata | 2026-08-19 |
| TQ-9 | Authenticate by JWKS signature and `exp`, then authorize trusted service clients through a JWT `aud`/`azp` allowlist rather than fine-grained scopes | The answer deliberately selects client-identity authorization. It makes 403 behavior testable but conflicts with the platform standard requiring scopes, so standards-owner review is required before implementation | 2026-08-19 |

## Iteration History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-07-26 | Initial spec generated from EPIC-01 requirements |
| 1.1 | 2026-07-26 | Resolved TQ-1 to TQ-5, confidence raised from 75% to 95% |
| 1.2 | 2026-08-18 | Reconciled the spec with the current PRD and all three standards files; corrected PRD confidence and current stack; restored exact epic-scoped traceability and test → design → implementation mappings; preserved prior decisions; exposed API/error, persistence, ID-allocation, OAuth-scope, currency, performance, and Persian-message blockers as TQ-6 through TQ-12; recomputed confidence to 0%. |
| 1.3 | 2026-08-19 | Reconciled unambiguous TQ-6, TQ-7, TQ-8, TQ-10, and TQ-12 answers into the API, persistence, identifier, minor-unit, and required-currency-message design; retained TQ-9 because its free-text answer does not supply a creation permission/scope or claim mapping, and retained incomplete TQ-11 placeholders; recomputed confidence to 55%. |
| 1.4 | 2026-08-19 | Reconciled revised TQ-9 and TQ-11 answers: folded JWKS/`exp` verification plus trusted-client JWT `aud`/`azp` allowlisting and 401/403 tests into the specification; folded the p95, environment, and warm-up conditions into performance testing; raised confidence to 100%. Preserved the trusted-client versus required-scope standards conflict and retained the literal TQ-11 dataset row-count placeholder as a standard-impact blocker and risk. |
| 1.5 | 2026-08-19 | Reconciled the completed TQ-11 answer by replacing the obsolete placeholder with the 500,000-row interim dataset, and folded that value into boundaries, performance strategy, end-to-end coverage, and NFR verification. Kept pending capacity/operations confirmation as a delivery risk rather than a blocker. |

## Next Steps

1. Obtain standards-owner approval for the TQ-9 trusted-client allowlist exception or replace it with the required-scope authorization design before implementation.
2. Have capacity/operations confirm whether the 500,000-row interim volume is representative; if it differs materially, reassess environment sizing and rerun the performance test.
3. Invoke `/plan-tasks epic-01` only after the specification reaches the project’s desired readiness threshold.
