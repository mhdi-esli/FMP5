# EPIC-01 Message Creation Service — Implementation Tasks

| Field | Value |
|---|---|
| Epic | EPIC-01: Message Creation Service |
| Epic slug | `epic-01-message-creation-service` |
| Spec path | `specs/epic-01-message-creation-service/spec.md` |
| Spec version | 1.5 |
| Spec Confidence Level | 100% |
| Generated | 2026-08-21 |

## Readiness Score

**Readiness: 100% — 20 of 20 tasks ready; no task is blocked by an unresolved Technical Question or Task Planning Question.**

## Scope

This task list implements EPIC-01 Message Creation Service exactly as specified by `specs/epic-01-message-creation-service/spec.md`, version 1.5. It covers AC-001, AC-002, AC-003, AC-004, and AC-007 on the current Java 21 and Spring Boot 3.3.2 baseline. EPIC-02 and EPIC-03 behavior remains outside this epic; EPIC-01 consumes their service interfaces as integration preconditions.

## Foundation Tasks

### TASK-epic-01-01: Establish architecture and quality gates
Maps to: Foundation
Depends on: none
Status: not started
Test-first step: Add the ArchUnit checks for strict controller → service → repository dependency direction and run `mvn verify` to expose the absent or failing architecture, Checkstyle, Spotless, and JaCoCo 80% line gates.
Implementation step: Add the minimum Maven and test configuration needed for ArchUnit, Checkstyle, Spotless, and JaCoCo to fail `mvn verify` on a layer violation, formatting violation, or line coverage below 80%; do not add SpotBugs or SonarQube.

## Tasks by Acceptance Criterion

### AC-003: Missing Required Field

### TASK-epic-01-02: Define the request record and required-field validation
Maps to: AC-003
Depends on: TASK-epic-01-01
Status: not started
Test-first step: Add a parameterized unit test that removes each of the nine FR-002 mandatory fields from an 18-field `MessageCreationRequest` and asserts one corresponding required-field error in deterministic request-field order, including `نوع ارز مشخص نشده است` for `currency`.
Implementation step: Add the 18-component `MessageCreationRequest` record with the minimum record-component Bean Validation constraints and Persian error definitions needed for the nine mandatory fields, preserving raw boundary values needed by controlled validation.

### TASK-epic-01-03: Map required-field failures to the HTTP contract
Maps to: AC-003
Depends on: TASK-epic-01-02
Status: not started
Test-first step: Add a `@WebMvcTest` that omits `currency` and asserts HTTP 400, issuer `FMP`, the selected integer invalid-input code, field association `currency`, exact text `نوع ارز مشخص نشده است`, and a `ResponseEnvelope` with non-null `errorList`.
Implementation step: Add the minimum global Bean Validation exception mapping and `ResponseEnvelope`/`ErrorItem` DTOs required to return the asserted field identity, Persian message, and standards-compliant error envelope.

### AC-004: Unsupported Network

### TASK-epic-01-04: Validate supported network values
Maps to: AC-004
Depends on: TASK-epic-01-02
Status: not started
Test-first step: Add unit and controller-slice tests asserting that raw `SWIFT` and `SEPA` values pass, `INVALID_NETWORK` returns HTTP 400 with the selected `FMP` representation of the legacy `MSG-003` condition and exact text `شبکه انتخاب‌شده پشتیبانی نمی‌شود`, and malformed boundary input uses the same controlled mapping.
Implementation step: Keep the request network value raw through boundary validation, add the minimum supported-value check, convert valid values to the string enum only after validation, and route failures through the shared envelope/error mapping.

### AC-002: Validation Failure - Invalid Amount

### TASK-epic-01-05: Reject non-positive amounts
Maps to: AC-002
Depends on: TASK-epic-01-02
Status: not started
Test-first step: Add service unit tests for amount `0` and a negative amount that assert a `Validation Failed` result, the invalid-amount error, and exact Persian text `مبلغ باید بزرگتر از صفر باشد`; add a controller-slice assertion for HTTP 400 and the selected `FMP` envelope.
Implementation step: Add the minimum `BigDecimal.ZERO` comparison, deterministic validation result, and response mapping needed to reject non-positive values without using binary floating point.

### TASK-epic-01-06: Enforce configured currency minor units
Maps to: AC-002
Depends on: TASK-epic-01-05
Status: not started
Test-first step: Add unit tests for configured or database-backed ISO 4217 currencies at and beyond their permitted minor-unit boundary, plus an unknown currency, asserting valid boundary values pass and excessive precision or an unknown code returns the configured validation error.
Implementation step: Add the minimum authoritative platform-configuration or database-table lookup selected by TQ-10 and apply its minor-unit rule in `MessageValidationService`; do not use Java runtime currency metadata or an external monetary library.

### TASK-epic-01-07: Persist validation failures without drafts
Maps to: AC-002, AC-003, AC-004
Depends on: TASK-epic-01-03, TASK-epic-01-04, TASK-epic-01-05, TASK-epic-01-06
Status: not started
Test-first step: Add Testcontainers cases for non-positive amount, missing `currency`, and unsupported network that assert one `Validation Failed` attempt is persisted with round-trippable validation-error JSONB and no `Draft` business message is created.
Implementation step: After confirming the live PostgreSQL structure through the read-only database MCP, add the minimum Flyway/entity/repository and transactional failed-attempt policy selected by TQ-7 to persist validation errors without creating a draft.

### AC-007: Unauthorized Access

### TASK-epic-01-08: Reject unauthenticated and invalid tokens
Maps to: AC-007
Depends on: TASK-epic-01-01
Status: not started
Test-first step: Add controller/security-slice tests for a missing, malformed, expired, and invalid-signature bearer token that assert HTTP 401 in the selected envelope form and verify that neither controller nor service is invoked.
Implementation step: Configure the minimum stateless OAuth2 resource-server filter chain, configured JWKS-based signature and expiry validation, and authentication entry point needed to terminate those requests before controller execution without hardcoding a public key.

### TASK-epic-01-09: Restrict creation to trusted service clients
Maps to: AC-007
Depends on: TASK-epic-01-08
Status: not started
Test-first step: Add security-slice tests proving a valid signed, unexpired JWT is permitted when either `aud` or `azp` identifies an allowlisted trusted client and returns HTTP 403 in the selected envelope form when neither claim is allowlisted.
Implementation step: Add the minimum configurable trusted-client allowlist and JWT `aud`/`azp` authorization rule selected by TQ-9, without introducing fine-grained scope checks not selected by the specification.

### TASK-epic-01-10: Audit rejected requests without sensitive data
Maps to: AC-007
Depends on: TASK-epic-01-08, TASK-epic-01-09
Status: not started
Test-first step: Add tests for 401 and 403 paths that assert one structured unauthorized-attempt event contains a timestamp and trace/correlation ID while excluding bearer tokens and request or response bodies.
Implementation step: Add the minimum request-level security audit hook and structured fields needed for rejected attempts, sourcing correlation from the OpenTelemetry trace context or MDC and never logging full payloads or tokens.

### AC-001: Create Valid Message

### TASK-epic-01-11: Allocate daily sequential message IDs
Maps to: AC-001
Depends on: TASK-epic-01-01
Status: not started
Test-first step: Add a Testcontainers test that concurrently allocates IDs for one date and asserts uniqueness and the `MSG-YYYYMMDD-NNNNNN` daily sequence format, with no lost allocations.
Implementation step: After confirming the live PostgreSQL structure through the read-only database MCP, add the minimum Flyway schema and PostgreSQL-backed daily-counter operation selected by TQ-8, exposed through `MessageIdGenerator.nextId(LocalDate)`.

### TASK-epic-01-12: Emit a sanitized success audit event
Maps to: AC-001
Depends on: TASK-epic-01-01
Status: not started
Test-first step: Add a unit test asserting one successful creation attempt event contains timestamp, outcome, and the current trace/correlation ID while omitting the request body, response body, bearer token, and other sensitive payload values.
Implementation step: Add the minimum `MessageAuditLogger` structured event implementation using SLF4J and the OpenTelemetry trace context or MDC to satisfy the asserted fields and exclusions.

### TASK-epic-01-13: Orchestrate valid message creation
Maps to: AC-001
Depends on: TASK-epic-01-02, TASK-epic-01-04, TASK-epic-01-05, TASK-epic-01-06, TASK-epic-01-11, TASK-epic-01-12
Status: not started
Test-first step: Add a Mockito service test with an active MT200/SWIFT definition and valid sender and receiver that asserts validation, definition resolution, institution validation, ID allocation, one `Draft` save with validation result `Success`, the returned response values, and one correlated success audit in the specified order.
Implementation step: Add the minimum stateless `MessageCreationService` orchestration through `MessageValidationService`, the existing EPIC-02 `MessageDefinitionService`, the existing EPIC-03 `InstitutionService`, `MessageIdGenerator`, `MessageRepository`, and `MessageAuditLogger`, with no business logic in the controller.

### TASK-epic-01-14: Make allocation and persistence one transaction
Maps to: AC-001
Depends on: TASK-epic-01-07, TASK-epic-01-11, TASK-epic-01-13
Status: not started
Test-first step: Add Testcontainers tests that persist a valid 18-field message as `Draft`/`Success`, force a repository failure and assert both message persistence and the daily-counter update roll back, and concurrently create valid messages without duplicate IDs or lost counter allocations.
Implementation step: Add the minimum transaction boundary around daily-counter allocation and successful repository persistence so both commit or roll back together under concurrent creation.

### TASK-epic-01-15: Enforce definition and institution preconditions
Maps to: AC-001
Depends on: TASK-epic-01-13, TASK-epic-01-14
Status: not started
Test-first step: Add integration cases proving valid EPIC-02/EPIC-03 fixtures permit a draft while an inactive or missing message definition and invalid sender or receiver produce validation failure and cannot create a draft.
Implementation step: Wire the creation service only to the EPIC-02 and EPIC-03 service interfaces and add the minimum dependent-service failure mapping needed by the tests; do not couple EPIC-01 to their repositories or management internals.

### TASK-epic-01-16: Expose successful creation at POST /v1/messages
Maps to: AC-001
Depends on: TASK-epic-01-03, TASK-epic-01-04, TASK-epic-01-05, TASK-epic-01-06, TASK-epic-01-08, TASK-epic-01-09, TASK-epic-01-13, TASK-epic-01-14, TASK-epic-01-15
Status: not started
Test-first step: Add a `@WebMvcTest` that posts all 18 fields with an allowlisted signed, unexpired JWT and asserts HTTP 200, `Draft`, `Success`, the generated message ID and creation time, plus `resultData`, `message`, and a non-null `errorList` at `POST /v1/messages`.
Implementation step: Add the minimum `MessageController`, `MessageCreationResponse` record, and service-result mapping needed for the selected versioned endpoint and envelope, exposing no persistence entity.

### TASK-epic-01-17: Verify the endpoint contract with Pact
Maps to: AC-001, AC-002, AC-003, AC-004, AC-007
Depends on: TASK-epic-01-07, TASK-epic-01-08, TASK-epic-01-09, TASK-epic-01-16
Status: not started
Test-first step: Add Pact interactions for the 18-field success request, non-positive amount, missing `currency`, unsupported network, missing or invalid authentication, and non-allowlisted client, asserting the selected status codes and `ResponseEnvelope` schemas.
Implementation step: Add only the contract-test configuration and provider states needed for those interactions to exercise `POST /v1/messages`; adjust endpoint mappings only where the contract tests expose a mismatch with the specification.

### TASK-epic-01-18: Exercise all acceptance paths end to end
Maps to: AC-001, AC-002, AC-003, AC-004, AC-007
Depends on: TASK-epic-01-07, TASK-epic-01-10, TASK-epic-01-14, TASK-epic-01-15, TASK-epic-01-16, TASK-epic-01-17
Status: not started
Test-first step: Through the running service and real security filters/PostgreSQL, add the five specified acceptance flows: successful allowlisted creation; zero and negative amount; missing `currency`; `INVALID_NETWORK`; and 401/403 authentication/authorization, asserting persistence, exact Persian messages, envelope shape, and audit correlation where specified.
Implementation step: Add the minimum deployed test fixtures for real EPIC-02/EPIC-03 service implementations and correct only behavior that prevents the five acceptance flows from matching the specification; do not bypass controller or security layers.

### TASK-epic-01-19: Publish the creation operation through SpringDoc
Maps to: AC-001
Depends on: TASK-epic-01-16
Status: not started
Test-first step: Add an integration assertion that SpringDoc exposes `POST /v1/messages`, describes the operation and all fields, references request/response component schemas rather than inline bodies, documents success and error responses, and applies the OAuth2 security scheme.
Implementation step: Add the minimum SpringDoc annotations and schema descriptions required for the generated OpenAPI 3.x document to satisfy those assertions.

### TASK-epic-01-20: Verify concurrent response time
Maps to: AC-001
Depends on: TASK-epic-01-14, TASK-epic-01-15, TASK-epic-01-16
Status: not started
Test-first step: In the dedicated production-representative performance environment, pre-seed 500,000 rows, discard the first 1,000 requests or two minutes of sustained load whichever occurs first, run 100 concurrent valid creates, and assert p95 is below 500 ms while recording p99 and the interim dataset assumption.
Implementation step: Make only evidence-driven changes needed for the specified p95 assertion while preserving transactionality and validation behavior, then record that the result is interim until capacity/operations confirms the representative dataset volume.

## Dependency Notes

- Validation rules `TASK-epic-01-02` through `TASK-epic-01-06`, security tasks `TASK-epic-01-08` through `TASK-epic-01-10`, ID allocation `TASK-epic-01-11`, and success auditing `TASK-epic-01-12` may proceed in parallel after the foundation gate.
- `TASK-epic-01-07` establishes the shared failed-attempt persistence behavior used by all three validation criteria.
- The successful vertical slice is `TASK-epic-01-11`/`TASK-epic-01-12` → `TASK-epic-01-13` → `TASK-epic-01-14` → `TASK-epic-01-15` → `TASK-epic-01-16`.
- Contract, end-to-end, OpenAPI, and performance verification follow the corresponding executable endpoint; OpenAPI and performance work may proceed in parallel once their listed dependencies are complete.

## Blocked Tasks

None.

## Decision Log

None.

## Risks

- TQ-9 selects JWT `aud`/`azp` trusted-client allowlisting rather than the platform standard’s required-scope authorization. Obtain standards-owner approval for the exception or revise the specification before relying on this as the production authorization model.
- TQ-11’s 500,000-row dataset is an interim assumption. Capacity/operations must confirm the expected production volume; a materially different value requires environment reassessment and another performance run.
- Preserved historical TQ-1/TQ-2 repository-stub decisions conflict with the current service-composition and strict-layering standards. Tasks intentionally use EPIC-02/EPIC-03 service interfaces and require human review before any repository stub is retained.
- EPIC-04 will later change the Java, Spring Boot, and cache baseline and re-verify EPIC-01; those migration changes are outside this task list.

## Iteration History

- **2026-08-21 — Initial generation:** Created 20 dependency-ordered RED → GREEN → REFACTOR units from spec version 1.5; mapped every scoped acceptance criterion, carried all answered TQ-6 through TQ-12 decisions into executable steps, and retained the authorization and performance assumptions as delivery risks rather than blockers.
