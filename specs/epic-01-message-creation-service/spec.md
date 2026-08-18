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
| Updated | 2026-08-18 |

## Confidence Level

**Confidence Level: 0% — 0/5 ACs resolved (0%) − 20 points for 4 open high-impact TQs, clamped to 0%.**

All five mapped criteria depend on the unresolved API compatibility decision in TQ-6. AC-001 also depends on the message-ID allocation and OAuth-scope decisions; AC-002 has a persistence contradiction and unresolved currency-minor-unit behavior; and AC-003 lacks an exact Persian required-field message.

## Architecture

EPIC-01 owns the synchronous message-creation entry point and its orchestration. It follows the strict layered flow `controller → service → repository`; controllers contain no business logic, services may compose through direct injection, and dependencies may not call upward or skip layers (`.claude/_architecture-reference.md`, **System Architecture** and **Development Patterns**).

The HTTP surface is unresolved because the scoped PRD requires `POST /api/v1/messages` and `MSG-XXX` validation errors, while the applicable standards require `/v1/messages`, a `resultData`/`message`/`errorList` envelope, and `FMP` integer codes starting at 201. The architecture reference also records that the rewrite is decided but not yet scoped to an epic. The controller, response mapping, error mapping, and all HTTP tests therefore remain [blocked by TQ-6].

The service performs this ordered flow:

1. Receive a deserialized request from the controller.
2. Validate required fields, amount, currency, date, message type, and network.
3. Resolve the active message definition through the EPIC-02 service boundary.
4. Validate sender and receiver through the EPIC-03 service boundary.
5. Allocate the daily sequential message ID [blocked by TQ-8].
6. Persist the result transactionally with `Draft` or `Validation Failed` semantics, subject to the AC-002 conflict [blocked by TQ-7].
7. Emit a structured audit event containing timestamp and correlation/trace ID without logging the request or response body.
8. Return the service result for HTTP response mapping [blocked by TQ-6].

OAuth 2.0 resource-server authentication is enforced before controller execution. Tokens must be verified for signature and expiry against the configured Dotin SSO key source; the required authorization scope remains [blocked by TQ-9] (`.claude/_architecture-reference.md`, **Major Design Decisions — Security**).

The current EPIC-01 runtime baseline remains Java 21 and Spring Boot 3.3.2. Java 25, Spring Boot 4.1.0, Redis, and the retroactive standards migration belong to EPIC-04 (`brainstorm/Epic_PRD.md`, **Epic Breakdown — EPIC-04**; `.claude/_architecture-reference.md`, **Constraints**).

## Components

| Component | Responsibility | Exposed interface | Consumed interface |
|---|---|---|---|
| `MessageController` | Accept the authenticated creation request and map the service result to HTTP | Creation endpoint [blocked by TQ-6] | `MessageCreationService` |
| `MessageCreationRequest` record | Carry the 18 PRD input fields and boundary validation metadata without exposing entities | JSON request DTO | Bean Validation and Jackson |
| `MessageCreationResponse` record | Carry message ID, type, network, status, creation time, validation result, and errors | Response payload [blocked by TQ-6] | Service result and error mapper |
| `ResponseEnvelope<T>` and `ErrorItem` | Represent the standards-compliant response surface if selected | `resultData`, `message`, `errorList` [blocked by TQ-6] | Controller and exception handler |
| `MessageCreationService` | Orchestrate validation, dependent services, ID allocation, persistence, and audit emission in a transaction | `createMessage(MessageCreationRequest)` | Validation, definition, institution, ID, repository, audit components |
| `MessageValidationService` | Return all field-validation failures in deterministic request-field order | `validate(MessageCreationRequest)` | Currency minor-unit policy [blocked by TQ-10] |
| `MessageDefinitionService` | Resolve an active definition by message type and network | Existing EPIC-02 service operation | EPIC-02 repository/cache internals |
| `InstitutionService` | Validate sender and receiver compatibility | Existing EPIC-03 service operations | EPIC-03 repository internals |
| `MessageIdGenerator` | Allocate `MSG-YYYYMMDD-NNNNNN` identifiers safely under concurrency | `nextId(LocalDate)` [blocked by TQ-8] | Allocation store [blocked by TQ-8] |
| `MessageRepository` | Persist and retrieve `Message` entities | Spring Data repository operations | PostgreSQL |
| `MessageAuditLogger` | Emit one structured event per attempted creation with timestamp, outcome, and trace/correlation ID | `recordAttempt(...)` | SLF4J/MDC and OpenTelemetry trace context |
| `MessageExceptionHandler` | Map validation and application failures consistently | Global exception handling [blocked by TQ-6] | Error-code/message definitions |
| `SecurityConfiguration` | Configure OAuth 2.0 resource-server verification and endpoint authorization | Spring Security filter chain | Configured SSO metadata/JWK source; required scope [blocked by TQ-9] |

`MessageCreationRequest` carries: `messageType`, `network`, `requestReference`, `transactionReference`, `relatedReference`, `valueDate`, `currency`, `amount`, `senderInstitutionIdentifier`, `senderBic`, `senderBranchId`, `receiverInstitutionIdentifier`, `receiverBic`, `receiverBranchId`, `chargeType`, `instructionCode`, `narrative`, and `additionalInformation`. The nine mandatory fields are exactly those listed by FR-002.

DTOs are Java records and entities are never exposed (`.claude/_coding-guidelines.md`, **Coding Standards** and **Design Patterns**). Money uses `BigDecimal`, enums serialize as strings, JSON fields use `camelCase`, and dates use ISO 8601 (`.claude/_coding-guidelines.md`, **SWA_101 compliance**).

## Dependencies

### Internal dependencies

| Dependency | Source | Use in EPIC-01 |
|---|---|---|
| EPIC-02 Message Definition Management | AC-001 precondition; PRD EPIC-02 | Confirm an active MT200/network definition through `MessageDefinitionService` |
| EPIC-03 Institution Management | AC-001 precondition; PRD EPIC-03 | Validate sender and receiver through `InstitutionService` |
| Global exception handling | `.claude/_architecture-reference.md`, Development Patterns | Convert boundary and service failures to the selected response contract [blocked by TQ-6] |
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

No external monetary library is selected while TQ-10 remains open. Caffeine/Redis behavior is outside EPIC-01 and belongs to EPIC-02/EPIC-04.

## Boundaries

### In scope

- One authenticated synchronous MT200 creation operation.
- Acceptance of all 18 FR-001 input fields and mandatory-field validation for the nine FR-002 fields.
- Positive amount, currency decimal, ISO 4217, and `YYYY-MM-DD` validation.
- SWIFT and SEPA request values.
- Coordination with EPIC-02 and EPIC-03 through their service interfaces for AC-001.
- Daily sequential message-ID generation in `MSG-YYYYMMDD-NNNNNN` format.
- Transactional persistence with `Draft` and `Validation Failed` outcomes, once TQ-7 is resolved.
- Persian validation errors and the response/error representation selected by TQ-6.
- OAuth 2.0 authentication, structured logging, correlation/trace propagation, and attempt auditing.
- SpringDoc-generated OpenAPI documentation for the selected endpoint contract.
- Validation response time and concurrency verification once TQ-11 defines the measurement contract.

### Out of scope

- Message-definition CRUD, activation, mapping, and cache ownership (EPIC-02).
- Institution CRUD and master-data ownership (EPIC-03).
- Java 25, Spring Boot 4.1.0, Redis migration, or retrofit verification (EPIC-04).
- Business checks for balances, transfer limits, or authorization.
- Transmission to SWIFT/SEPA, delivery tracking, retries, webhooks, and message history/versioning.
- Message types other than MT200.
- Request-payload signing, which the architecture reference assigns to a later epic.

### System-context edges

An authenticated business system calls the creation endpoint [blocked by TQ-6]. Spring Security calls the configured SSO metadata/JWK source to verify bearer tokens and applies the required scope [blocked by TQ-9]. The service calls EPIC-02 and EPIC-03 service interfaces and PostgreSQL; it emits sanitized structured events to the existing ELK path. It does not call a financial network.

## Data Flow

### Successful creation

1. A client sends all required business data and a bearer token to the selected endpoint [blocked by TQ-6].
2. Spring Security verifies token signature and expiry and applies endpoint authorization [blocked by TQ-9].
3. The request DTO is validated. Currency minor-unit validation remains [blocked by TQ-10].
4. `MessageCreationService` resolves the active definition and validates both institutions.
5. `MessageIdGenerator` obtains the next daily sequence atomically [blocked by TQ-8].
6. A transaction persists one message with status `Draft`, validation result `Success`, and a unique message ID.
7. `MessageAuditLogger` emits a success event with timestamp and the current trace/correlation ID; no request or response body is logged.
8. The controller returns HTTP 200 with the exact body selected by TQ-6.

### Validation failure

1. Validation collects all detectable field failures in deterministic field order, preserving the prior TQ-4 decision.
2. The error mapper selects the applicable Persian message.
3. The service applies the `Validation Failed` persistence policy. AC-002 conflicts with FR-010 and remains [blocked by TQ-7]. AC-003 and AC-004 use FR-010 unless TQ-7 establishes a broader rule.
4. The audit logger emits a failed-validation attempt with timestamp, trace/correlation ID, and non-sensitive error identifiers.
5. The controller returns HTTP 400 using the response/error contract selected by TQ-6. The exact missing-currency Persian message is [blocked by TQ-12].

### Authentication failure

1. The security filter rejects a missing, malformed, expired, or invalid-signature bearer token before controller invocation.
2. The request-level audit path emits an unauthorized-attempt event with timestamp and trace/correlation ID without recording the body.
3. The HTTP status is 401. Its body representation remains [blocked by TQ-6].

### Transaction and error path

Message-ID allocation and successful persistence occur in one service transaction (`brainstorm/Epic_PRD.md`, NFR-004). A persistence failure rolls back both. The global exception handler produces the selected error representation [blocked by TQ-6], and structured error logging includes the trace/correlation ID but excludes full bodies (`.claude/_coding-guidelines.md`, **Hard Rules**).

## Testing Strategy

Follow failing test → minimum production code → behavior-preserving refactor; never weaken a test to pass (`.claude/_coding-guidelines.md`, **Testing Practices**).

- **Unit:** Mockito-based service tests cover validation orchestration, all field rules, dependent-service outcomes, audit invocation, and message-ID formatting/allocation once TQ-8 is resolved.
- **Controller slice:** `@WebMvcTest` covers deserialization, authentication, status codes, exact Persian messages, and the selected response envelope/error contract [blocked by TQ-6].
- **Integration:** Testcontainers with real PostgreSQL covers transaction boundaries, status/error persistence, uniqueness, rollback, and concurrent creation. No database mocks are used for these assertions.
- **Contract:** Pact covers the selected creation endpoint, request fields, success response, validation response, and unauthorized response [blocked by TQ-6].
- **End-to-end:** The running service exercises the five mapped criteria with real HTTP/security filters and PostgreSQL. EPIC-02/03 preconditions use deployed test fixtures or their real service implementations, not controller-layer bypasses.
- **Conformance:** A shared fixture checks `resultData`, `message`, and non-null `errorList` if the SWA_101 option is selected [blocked by TQ-6].
- **Performance:** Test 100 concurrent creation requests and the `<500ms` requirement, but the statistic, environment, and warm-up/pass conditions remain [blocked by TQ-11].
- **Security:** Verify no token, malformed token, invalid signature, and expired token return 401; verify the configured scope once TQ-9 is answered; verify logs do not contain request/response bodies or bearer tokens.
- **Coverage and quality:** The suite contributes to the project-wide 80% line gate and passes Checkstyle, Spotless, JaCoCo, and ArchUnit checks (`.claude/_coding-guidelines.md`, **Coding Standards** and **Testing Practices**).

## Unit Tests

- `validate_allMandatoryFieldsPresent_returnsNoRequiredFieldErrors`
- `validate_eachMandatoryFieldMissing_returnsMsg001ForThatField`
- `validate_amountZero_returnsInvalidAmountError`
- `validate_amountNegative_returnsInvalidAmountError`
- `validate_amountExceedsCurrencyMinorUnits_returnsConfiguredError` [blocked by TQ-10]
- `validate_unknownCurrency_returnsInvalidCurrencyError`
- `validate_nonIsoValueDate_returnsInvalidDateError`
- `validate_unsupportedNetwork_returnsMsg003`
- `validate_multipleInvalidFields_returnsAllErrorsInFieldOrder`
- `createMessage_validDependencies_persistsDraftAndAuditsSuccess` [blocked by TQ-8]
- `createMessage_validationFailure_appliesPersistencePolicy` [blocked by TQ-7]
- `createMessage_dependencyValidationFails_returnsValidationFailure`
- `nextId_concurrentAllocation_returnsUniqueDailySequence` [blocked by TQ-8]
- `audit_attempt_omitsSensitivePayloadAndIncludesTraceId`

## Integration Tests

- Testcontainers persists a valid message with all 18 fields, `Draft`, `Success`, and the generated message ID.
- Testcontainers verifies validation-error JSONB round trips when the selected TQ-7 policy persists failed attempts.
- A forced repository failure rolls back ID allocation and message persistence [blocked by TQ-8].
- Concurrent valid creates produce unique IDs and no lost sequence allocations [blocked by TQ-8].
- Valid EPIC-02/03 fixtures satisfy AC-001; inactive/missing dependent data cannot produce a draft.
- Spring Security accepts a valid signed token with the configured scope [blocked by TQ-9].
- Spring Security rejects missing, expired, and invalid-signature tokens with 401.
- SpringDoc exposes the selected creation operation and referenced request/response schemas [blocked by TQ-6].

## End-to-End Tests

- Submit the AC-001 request through the running application and assert 200, `Draft`, unique message ID, `Success`, persistence, and audit correlation [blocked by TQ-6, TQ-8, TQ-9].
- Submit amount `0` and a negative amount and assert the AC-002 HTTP/error behavior and selected persistence outcome [blocked by TQ-6, TQ-7].
- Omit `currency` and assert AC-003, including the approved Persian message [blocked by TQ-6, TQ-12].
- Submit `INVALID_NETWORK` and assert AC-004, including `شبکه انتخاب‌شده پشتیبانی نمی‌شود` [blocked by TQ-6].
- Submit no token and invalid tokens and assert AC-007 [blocked by TQ-6].
- Run 100 concurrent valid creates and evaluate the sourced latency target under the measurement contract [blocked by TQ-11].

## Acceptance Criteria Mapping

### AC-001: Create Valid Message

**Requirements satisfied:** FR-001, FR-002, FR-003, FR-004, FR-005, FR-009, FR-010, FR-012; supported by FR-014 and NFR-001–NFR-006.

**Test strategy**

Start with a coherent failing set: a service unit test supplies a request containing all mandatory fields, active MT200/SWIFT definition, and valid institutions, then asserts one `Draft` entity is saved, a unique correctly formatted ID is returned, validation result is `Success`, and a correlated success audit event is emitted. A controller slice test supplies a valid signed JWT and asserts HTTP 200 plus the selected response contract [blocked by TQ-6, TQ-9]. A Testcontainers integration test proves persistence and uniqueness under concurrent allocation [blocked by TQ-8]. These tests fail before implementation because no orchestrated validation, allocation, transaction, response mapping, or audit path exists.

**Design approach**

`MessageController` delegates to `MessageCreationService`; the service calls `MessageValidationService`, `MessageDefinitionService`, and `InstitutionService` in order, allocates the ID, persists inside a transaction, and audits the outcome. Strict downward layering and service composition follow `.claude/_architecture-reference.md`, **System Architecture**. The HTTP contract is [blocked by TQ-6], the allocator is [blocked by TQ-8], and the authorized-token scope is [blocked by TQ-9].

**Implementation approach**

Implement only the DTO boundary, orchestration, transactional save, service-interface calls, audit event, and response mapping required by the failing tests. Add the selected endpoint/error contract after TQ-6, the selected atomic allocator after TQ-8, and the selected scope rule after TQ-9. Refactor only duplicated test fixtures, pure mappings, and validation composition while preserving all assertions.

### AC-002: Validation Failure - Invalid Amount

**Requirements satisfied:** FR-003, FR-010, FR-011.

**Test strategy**

First write service unit tests for `0` and a negative amount. Each asserts status `Validation Failed`, the invalid-amount error, and the exact Persian message `مبلغ باید بزرگتر از صفر باشد`. Add a controller slice assertion for HTTP 400 and the selected error code/body [blocked by TQ-6]. Add a Testcontainers assertion for the chosen failed-attempt persistence semantics [blocked by TQ-7]. Add minor-unit boundary cases after the currency policy is selected [blocked by TQ-10]. The tests fail before implementation because the amount rule, mapping, and persistence behavior are absent.

**Design approach**

`MessageValidationService` compares monetary values with `BigDecimal.ZERO` and never uses binary floating point. It maps non-positive values to the scoped invalid-input error and exact Persian text. Currency decimal validation is [blocked by TQ-10]. The contradiction between AC-002 “message is NOT created” and FR-010 persistence with `Validation Failed` is [blocked by TQ-7]; the external error representation is [blocked by TQ-6].

**Implementation approach**

Implement the non-positive comparison and deterministic validation-error result first. Apply the TQ-7 persistence choice without weakening the “not created” assertion, then apply the TQ-6 response mapper and TQ-10 minor-unit rule. Refactor only shared validation-result construction once the tests pass.

### AC-003: Missing Required Field

**Requirements satisfied:** FR-002, FR-010, FR-011.

**Test strategy**

Write a controller slice test that omits `currency` and asserts HTTP 400, the selected invalid-input code, a field association identifying `currency`, and the approved Persian required-field message [blocked by TQ-6, TQ-12]. Add a parameterized unit test that removes each of the nine FR-002 mandatory fields and asserts one corresponding error. The tests fail before implementation because mandatory-field constraints and exception-to-response mapping are absent.

**Design approach**

Apply Bean Validation annotations that are compatible with record components and use the global exception handler to produce the selected contract. The field identity remains `currency`; error collection follows the preserved TQ-4 decision. The response/error representation is [blocked by TQ-6], and the exact currency-required Persian message is [blocked by TQ-12].

**Implementation approach**

Add the minimum record-component constraints, validation handler mapping, and Persian message lookup required by the tests. Fold in the TQ-6 contract and TQ-12 message verbatim when answered. Refactor common required-field mappings only after all nine parameterized cases pass.

### AC-004: Unsupported Network

**Requirements satisfied:** FR-011, FR-012.

**Test strategy**

Write a controller slice test posting `network: "INVALID_NETWORK"` and assert HTTP 400, the selected representation of MSG-003, and exact text `شبکه انتخاب‌شده پشتیبانی نمی‌شود` [blocked by TQ-6]. Unit tests assert `SWIFT` and `SEPA` pass the network rule and any other value fails. The tests fail before implementation because unsupported values are not yet converted into the required domain validation error.

**Design approach**

Keep the raw network value available to boundary validation so an unsupported string reaches the controlled validation path rather than becoming an unstructured deserialization failure. Convert supported values to the string enum only after validation. The selected external code/envelope remains [blocked by TQ-6].

**Implementation approach**

Implement the smallest supported-value check and map failures to the exact Persian text and selected code representation. Ensure the global handler uses the same mapping for malformed boundary input. Refactor shared enum parsing only after SWIFT, SEPA, and invalid-value tests pass.

### AC-007: Unauthorized Access

**Requirements satisfied:** NFR-005.

**Test strategy**

Write controller/security slice tests that omit the bearer token and provide malformed, expired, and invalid-signature tokens. Assert the controller and service are not invoked and HTTP 401 is returned; assert the selected unauthorized body contract [blocked by TQ-6]. A valid-token happy-path test uses the configured endpoint scope after TQ-9 is answered. These tests fail before implementation because the resource-server filter chain and authentication entry point are not configured.

**Design approach**

Use Spring Security OAuth2 Resource Server with the configured Dotin SSO key source. Validate signature and expiry, never hardcode a public key, and do not log tokens (`.claude/_architecture-reference.md`, **Major Design Decisions — Security**). Authentication failures terminate before controller execution. The required scope is [blocked by TQ-9]; the 401 body representation is [blocked by TQ-6].

**Implementation approach**

Configure the minimum stateless resource-server filter chain and authentication entry point to make missing/invalid-token tests pass. Add endpoint authorization using the TQ-9 scope and map the 401 body using TQ-6. Refactor only reusable security test fixtures after behavior passes.

## Implementation Plan

1. **RED — contract characterization (AC-001–AC-004, AC-007):** add failing controller/Pact tests around the API contract once TQ-6 is answered; retain the PRD assertions as explicit compatibility tests until that decision is made.
2. **RED — field validation (AC-002, AC-003, AC-004):** add failing unit tests for mandatory fields, non-positive amounts, currency/date/network validation, deterministic error collection, and exact sourced Persian messages; add TQ-10/TQ-12 cases after resolution.
3. **GREEN — validation boundary (AC-002, AC-003, AC-004):** implement request-record constraints, `MessageValidationService`, global validation mapping, and the selected response representation.
4. **REFACTOR:** consolidate validation-result construction without introducing validator packages that violate the verified package layout.
5. **RED — security (AC-007, AC-001):** add failing missing/invalid-token tests and a valid-token test after TQ-9 defines the scope.
6. **GREEN — security:** configure the OAuth2 resource server, key discovery, stateless access rules, and sanitized unauthorized auditing.
7. **RED — creation orchestration (AC-001):** add failing service and Testcontainers tests for dependent-service checks, atomic ID allocation, one transaction, `Draft` persistence, response values, and correlated audit logging after TQ-8.
8. **GREEN — creation orchestration:** implement service composition, allocator, repository operation, transaction boundary, and success response.
9. **RED/GREEN — failed persistence (AC-002):** add and satisfy the TQ-7 persistence decision without changing the criterion’s observable assertions.
10. **REFACTOR:** remove duplication in mappings and test fixtures while preserving strict layer dependencies and public behavior.
11. **RED/GREEN — NFR verification:** add structured-log assertions, OpenAPI/Pact conformance, rollback tests, and the TQ-11 concurrency/latency test.
12. **VERIFY:** run the complete suite and quality gates; inspect the generated OpenAPI output for the selected versioned endpoint and referenced schemas.

This sequence is technical guidance only; `plan-tasks` owns the downstream task breakdown.

## Technical Questions

TQ-1 through TQ-5 exist only as historical answered entries in the preserved Decision Log; the earlier spec did not retain their literal question text, checkbox syntax, impact tags, or blocker lists. They are not treated as open or reparsed into new answers. New IDs therefore begin at TQ-6.

**TQ-6: Which API compatibility contract governs this retroactive EPIC-01 specification? (select one)**

[ ] Apply the current standards now: `POST /v1/messages`, `ResponseEnvelope`, and `FMP` integer errors from 201 upward ← recommended
[ ] Preserve the scoped PRD contract: `POST /api/v1/messages`, direct message response, `validationErrors`, and `MSG-XXX` codes until a remediation epic

Blocks: Architecture, Components, Boundaries, Data Flow, Testing Strategy, Integration Tests, End-to-End Tests, AC-001, AC-002, AC-003, AC-004, AC-007, Implementation Plan
Impact: (high-impact)

**TQ-7: What does AC-002 “message is NOT created” mean relative to FR-010’s requirement to store `Validation Failed` messages? (select one)**

[ ] Persist a `Validation Failed` attempt but do not create a `Draft` business message ← recommended
[ ] Do not persist a message row; return a response-only failed status and emit only the audit log
[ ] Persist the failed attempt outside the `messages` aggregate

Blocks: Architecture, Data Flow, Unit Tests, Integration Tests, End-to-End Tests, AC-002, Implementation Plan
Impact: (high-impact)

**TQ-8: Which atomic allocation mechanism must implement the daily `MSG-YYYYMMDD-NNNNNN` sequence under 100 concurrent creates? (select one)**

[ ] A PostgreSQL-backed daily counter updated in the message transaction ← recommended
[ ] A PostgreSQL sequence combined with the date, accepting that the numeric sequence does not reset daily
[ ] A Redis atomic counter, making EPIC-01 depend on the EPIC-04 Redis baseline

Blocks: Architecture, Components, Data Flow, Unit Tests, Integration Tests, End-to-End Tests, AC-001, Implementation Plan
Impact: (high-impact)

**TQ-9: What exact OAuth 2.0 scope authorizes message creation? (free text)**

Answer: `____________________`

Blocks: Architecture, Components, Boundaries, Data Flow, Testing Strategy, Integration Tests, AC-001, Implementation Plan
Impact: (high-impact)

**TQ-10: Which authoritative minor-unit policy validates amount precision for ISO 4217 currencies? (select one)**

[ ] Use the Java runtime ISO 4217 data through `java.util.Currency.getDefaultFractionDigits()` ← recommended
[ ] Maintain currency minor units in platform configuration or a database table
[ ] Use an approved external monetary library (name and version required in the answer)

Blocks: Components, Data Flow, Testing Strategy, Unit Tests, AC-002
Impact: (standard)

**TQ-11: What statistic, environment, dataset, and warm-up conditions define a pass for `<500ms` with 100 concurrent requests? (free text)**

Answer: `____________________`

Blocks: Boundaries, Testing Strategy, End-to-End Tests, Implementation Plan
Impact: (standard)

**TQ-12: What exact Persian message must represent a missing `currency` field? (free text)**

Answer: `____________________`

Blocks: Data Flow, End-to-End Tests, AC-003
Impact: (standard)

## Risks

| Risk | Source/evidence | Delivery effect | Treatment |
|---|---|---|---|
| Scoped API contract conflicts with mandatory current standards | PRD FR-001/ACs versus architecture/coding standards | Every HTTP criterion is ambiguous | Resolve TQ-6 before implementation planning |
| AC-002 contradicts FR-010 | “message is NOT created” versus storing `Validation Failed` | Persistence and transaction tests cannot be finalized | Resolve TQ-7 |
| Concurrent daily ID allocation is unspecified | FR-009, NFR-002, project ID convention | Duplicate IDs or broken daily sequencing under load | Resolve TQ-8 and test with real PostgreSQL |
| Endpoint authorization scope is absent | NFR-005 and security standard requiring scope validation | Happy-path authorization cannot be configured safely | Resolve TQ-9 with the SSO owner |
| Currency minor-unit authority is absent | FR-003/FR-004 | Amount precision can differ by runtime/configuration | Resolve TQ-10 |
| Performance pass conditions are incomplete | NFR-001/NFR-002 | Results may be non-reproducible | Resolve TQ-11 before the load test |
| Exact missing-currency Persian text is absent | AC-003 and FR-011 | Exact response assertion cannot be written | Resolve TQ-12 and obtain Persian-language review |
| Preserved TQ-1/TQ-2 decisions call for repository stubs, while current architecture requires service composition and strict layering | Existing Decision Log versus architecture reference | Reusing old repository coupling would violate current standards | Retain the history, but implement through EPIC-02/03 service interfaces and flag this conflict for human review |
| EPIC-04 changes the runtime and cache baseline later | PRD EPIC-04 | EPIC-01 may require subsequent retrofit verification | Keep current/future baselines clearly separated |

## Decision Log

The following entries are preserved verbatim from the prior specification. No newly answered question diverges from a recommendation in this iteration.

| # | Decision | Rationale | Date |
|---|----------|-----------|------|
| 1 | Use standard Spring Boot layered architecture | Matches PRD decision, team familiarity | 2026-07-26 |
| 2 | Generate Message ID in service layer | Centralized logic, easier testing | 2026-07-26 |
| TQ-1 | Use stub/fake for InstitutionRepository, swap when EPIC-03 ready | Allows parallel development, no EPIC-01 rework needed | 2026-07-26 |
| TQ-2 | Use stub/fake for MessageDefinitionRepository, swap when EPIC-02 ready | Same pattern, consistent approach | 2026-07-26 |
| TQ-3 | MSG-YYYYMMDD-NNNNNN (sequential daily) | Matches PRD example, human-readable | 2026-07-26 |
| TQ-4 | Collect all validation errors and return them together | Better UX, user sees all issues at once | 2026-07-26 |
| TQ-5 | Java Enum with code and message fields for Persian errors | Type-safe, compile-time checked, no external dependencies | 2026-07-26 |

## Iteration History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-07-26 | Initial spec generated from EPIC-01 requirements |
| 1.1 | 2026-07-26 | Resolved TQ-1 to TQ-5, confidence raised from 75% to 95% |
| 1.2 | 2026-08-18 | Reconciled the spec with the current PRD and all three standards files; corrected PRD confidence and current stack; restored exact epic-scoped traceability and test → design → implementation mappings; preserved prior decisions; exposed API/error, persistence, ID-allocation, OAuth-scope, currency, performance, and Persian-message blockers as TQ-6 through TQ-12; recomputed confidence to 0%. |

## Next Steps

1. Resolve TQ-6 through TQ-12 without modifying the recommendation checkboxes unless selecting an answer.
2. Re-run `/write-spec epic-01` to fold unambiguous answers into every blocked location and recompute confidence.
3. Invoke `/plan-tasks epic-01` only after the specification reaches the project’s desired readiness threshold.
