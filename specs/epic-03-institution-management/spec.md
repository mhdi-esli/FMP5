# Technical Specification: EPIC-03 Institution Management

## Confidence Level: 72% — 2 open technical questions (TQ-1 high-impact, TQ-2 standard)

**PRD Confidence Level:** 92%

---

## Architecture

The Institution Management feature adds full CRUD lifecycle for financial institution master data and wires institution validation into EPIC-01's message creation flow. It builds on the existing `Institution` entity and `InstitutionRepository` stubs.

```
┌──────────────────────────────────────────────────────────────────┐
│                     Presentation Layer                            │
│  ┌──────────────────────────────────────────────────────────┐    │
│  │  InstitutionController                                    │    │
│  │  - GET    /api/v1/institutions                           │    │
│  │  - GET    /api/v1/institutions/{id}                      │    │
│  │  - POST   /api/v1/institutions                           │    │
│  │  - PUT    /api/v1/institutions/{id}                      │    │
│  │  - DELETE /api/v1/institutions/{id}                      │    │
│  │  - GET    /api/v1/institutions/lookup?institutionId=     │    │
│  └──────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│                     Service Layer                                │
│  ┌──────────────────────────────────────────────────────────┐    │
│  │  InstitutionService                                      │    │
│  │  - CRUD operations                                       │    │
│  │  - Institution validation (called by EPIC-01)            │    │
│  │  - Network support validation                            │    │
│  └──────────────────────────────────────────────────────────┘    │
│  ┌──────────────────────────────────────────────────────────┐    │
│  │  MessageValidationService (EPIC-01)                       │    │
│  │  - validates institution via InstitutionService           │    │
│  └──────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│                     Data Layer                                   │
│  ┌──────────────────────────────────────────────────────────┐    │
│  │  InstitutionRepository (existing)                        │    │
│  │  - Extended with list/filter query methods               │    │
│  └──────────────────────────────────────────────────────────┘    │
│  ┌──────────────────────────────────────────────────────────┐    │
│  │  Database (PostgreSQL)                                    │    │
│  │  - institutions table (existing)                          │    │
│  │  - V3 migration for additional fields if needed           │    │
│  └──────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────┘
```

## Components

### 1. InstitutionController

| Field | Value |
|-------|-------|
| **Name** | `InstitutionController` |
| **Package** | `com.bank.messaging.controller` |
| **Responsibility** | REST endpoints for managing financial institutions |
| **Exposes** | `POST /api/v1/institutions` — create institution |
| | `GET /api/v1/institutions` — list institutions (filterable by name, network, isActive) |
| | `GET /api/v1/institutions/{id}` — get by ID |
| | `PUT /api/v1/institutions/{id}` — update institution |
| | `DELETE /api/v1/institutions/{id}` — soft-delete (deactivate) |
| | `GET /api/v1/institutions/lookup?institutionId=X` — lookup by business ID (used by EPIC-01) |
| **Consumes** | `InstitutionService` |

### 2. InstitutionService

| Field | Value |
|-------|-------|
| **Name** | `InstitutionService` |
| **Package** | `com.bank.messaging.service` |
| **Responsibility** | Business logic for institution CRUD and network validation |
| **Exposes** | |
| | `createInstitution(request)` — create institution |
| | `getInstitution(id)` — get by ID |
| | `listInstitutions(name, network, isActive)` — filtered list |
| | `updateInstitution(id, request)` — update institution |
| | `deleteInstitution(id)` — soft delete |
| | `findByInstitutionId(institutionId)` — lookup by business ID (used by EPIC-01) |
| | `validateInstitution(institutionId, network)` — validate sender institution exists, is active, and supports network |
| **Consumes** | `InstitutionRepository` |

### 3. DTOs (new)

| DTO | Fields | Purpose |
|-----|--------|---------|
| `InstitutionRequest` | institutionId, bic, name, branchId, isActive, supportedNetworks (list of strings) | Create/Update input |
| `InstitutionResponse` | id, institutionId, bic, name, branchId, isActive, supportedNetworks, createdAt, updatedAt | API response |

## Dependencies

### Internal

| Dependency | Type | Detail |
|------------|------|--------|
| EPIC-01 Message Creation Service | Consumer | EPIC-01 will call `InstitutionService.validateInstitution()` |
| EPIC-01 MessageValidationService | Extends | New validation rules for sender/receiver institution |
| Existing `ErrorCode` enum | Shared | Uses MSG-001 for general validation; may need new error codes |
| `InstitutionRepository` | Extends | Existing repository with additional query methods |

### External

| Dependency | Version | Purpose |
|------------|---------|---------|
| Spring Boot Starter Validation | 3.3.2 | Request validation |
| PostgreSQL | — | Primary storage |

## Boundaries

| In Scope | Out of Scope |
|----------|-------------|
| CRUD for institutions | BIC validation against SWIFT directory (future) |
| Institution existence validation | Real-time institution network availability checks |
| Network support validation | Institution hierarchy management (head office / branches) |
| Soft-delete (deactivate) | Hard delete |
| EPIC-01 integration for sender/receiver check | Multi-institution batch operations |

## Data Flow

### Happy Path — Create Institution

```
Client → POST /api/v1/institutions
       → InstitutionController
       → InstitutionService.createInstitution()
            → Validate request fields
            → Check uniqueness of institutionId
            → Save Institution entity
            → Return InstitutionResponse (201 Created)
```

### Happy Path — Message Creation with Institution Validation (EPIC-01 Integration)

```
MessageCreationService → MessageValidationService.validate()
                      → InstitutionService.validateInstitution(senderId, network)
                           → Repository lookup by institutionId + isActive check
                           → Network support check
                      → ValidationResult.success()
```

### Error Path — Duplicate Institution

```
Client → POST /api/v1/institutions (same institutionId)
       → InstitutionService
            → Uniqueness check fails
            → Return 409 Conflict (new error code)
```

### Error Path — Institution Not Found (AC-006)

```
MessageCreationService → MessageValidationService.validate()
                      → InstitutionService.validateInstitution(missingId, network)
                           → Repository returns empty
                      → Return ValidationResult.failed with MSG-010 error
```

## Testing Strategy

| Layer | Target | Tools |
|-------|--------|-------|
| Unit (Service) | 60% | JUnit 5 + Mockito |
| Unit (Controller) | 20% | Spring WebMvcTest |
| Integration (Repository) | 15% | Testcontainers + SpringBootTest |
| Integration (Full API) | 5% | Testcontainers + full context |

## Unit Tests

### InstitutionService Tests

| Test Name | Scenario | Expected |
|-----------|----------|----------|
| `createInstitution_success_returnsCreatedInstitution` | Valid request | Saved entity, 201 |
| `createInstitution_duplicateId_throwsException` | Same institutionId exists | 409 Conflict |
| `createInstitution_withSupportedNetworks_storesNetworks` | Networks provided | Networks stored |
| `createInstitution_withoutActive_defaultsToTrue` | isActive not set | Default true |
| `getInstitution_existingId_returnsInstitution` | Existing ID | Entity returned |
| `getInstitution_nonExistentId_throwsNotFoundException` | Non-existent ID | 404 |
| `listInstitutions_noFilter_returnsAll` | No query params | All institutions |
| `listInstitutions_filterByNetwork_returnsFiltered` | Filter by network | Only matching |
| `listInstitutions_filterByActive_returnsFiltered` | Filter by isActive | Active only |
| `updateInstitution_success_updatesFields` | Update existing | Fields updated |
| `updateInstitution_nonExistentId_throwsNotFoundException` | Non-existent ID | 404 |
| `deleteInstitution_softDelete_setsInactive` | Delete existing | isActive=false |
| `deleteInstitution_nonExistentId_throwsNotFoundException` | Non-existent ID | 404 |
| `findByInstitutionId_exists_returnsInstitution` | By business ID | Institution returned |
| `findByInstitutionId_notFound_returnsEmpty` | By missing ID | Optional.empty() |
| `validateInstitution_valid_returnsSuccess` | Exists + active + supports network | Success |
| `validateInstitution_notFound_returnsFailed` | Does not exist | Failed with MSG-010 |
| `validateInstitution_inactive_returnsFailed` | Exists but inactive | Failed with error |
| `validateInstitution_networkNotSupported_returnsFailed` | Exists but wrong network | Failed with error |

### InstitutionController Tests

| Test Name | Scenario | Expected |
|-----------|----------|----------|
| `postInstitution_valid_returns201` | Valid POST | 201 Created |
| `postInstitution_invalidBody_returns400` | Missing required fields | 400 |
| `postInstitution_duplicate_returns409` | Duplicate institutionId | 409 |
| `getInstitution_existing_returns200` | Existing ID | 200 |
| `getInstitution_nonExistent_returns404` | Non-existent ID | 404 |
| `getInstitutions_noFilter_returns200` | List all | 200 |
| `getInstitutions_filtered_returns200` | With query params | 200, filtered |
| `putInstitution_existing_returns200` | Update existing | 200 |
| `putInstitution_nonExistent_returns404` | Update non-existent | 404 |
| `deleteInstitution_existing_returns204` | Delete existing | 204 |
| `deleteInstitution_nonExistent_returns404` | Delete non-existent | 404 |
| `lookupInstitution_found_returns200` | By institutionId | 200 |
| `lookupInstitution_notFound_returns404` | By missing institutionId | 404 |
| `postInstitution_unauthorized_returns401` | No token | 401 |
| `getInstitutions_unauthorized_returns401` | No token | 401 |

## Integration Tests

| Test Name | Scenario | Expected |
|-----------|----------|----------|
| `saveInstitution_persistsAndRetrieves` | Save + find by ID | Full round-trip |
| `saveInstitution_withNetworks_storesCollection` | Save with supportedNetworks | Networks persisted |
| `findByInstitutionId_returnsMatching` | Query by business ID | Correct result |
| `findActiveInstitutions_returnsOnlyActive` | Mix active/inactive | Only active |
| `findBySupportedNetwork_returnsCorrect` | Query by network | Institutions supporting network |
| `uniqueConstraint_duplicateId_throwsException` | Same institutionId | DataIntegrityViolation |
| `updateInstitution_modifiesFields` | Update in DB | Fields changed correctly |
| `softDelete_setsInactive` | Delete preserves row | Row exists, isActive=false |
| `listAll_returnsAllInstitutions` | Multiple saved | All returned |

## End-to-End Tests

| Test Name | Scenario | Expected |
|-----------|----------|----------|
| `fullInstitutionLifecycle` | Create → Get → Update → List → Delete → Verify inactive | Full CRUD cycle |
| `epic01Integration_institutionValidation` | Create message with valid sender institution → Verify success | EPIC-01 success |
| `epic01Integration_invalidInstitution` | Create message with unknown sender → Verify MSG-010 error | EPIC-01 validation failure |

## Acceptance Criteria Mapping

### AC-006: Invalid Institution

**PRD Reference:**
```gherkin
GIVEN senderInstitutionIdentifier that does not exist in institution table
WHEN POST /api/v1/messages is called
THEN response status is 400
AND validationErrors indicates invalid sender institution
```

**Test Strategy:**
This AC is shared with EPIC-01:
1. EPIC-03 provides `InstitutionService.validateInstitution()` and a lookup endpoint
2. EPIC-01's `MessageValidationService` (or `MessageCreationService`) calls `InstitutionService`
3. When institution is missing inactive or network mismatch → failed validation with specific error

Tests:
- `validateInstitution_notFound_returnsFailed` — service returns failed when institution missing
- E2E: `epic01Integration_invalidInstitution` — full stack test via POST /api/v1/messages

**Design Approach:**
- `InstitutionService` wraps the repository with validation logic
- `validateInstitution(institutionId, network)` returns `ValidationResult` matching EPIC-01's existing pattern
- The validation is called from `MessageCreationService` after message definition validation
- Reuses the existing `ValidationResult` class from EPIC-01

**Implementation Approach:**
1. Create `InstitutionService` with CRUD + `validateInstitution()`
2. Wire into `MessageCreationService.createMessage()` flow
3. Add new error code MSG-010 for institution validation failures

### Local Acceptance Criteria (derived from FR-013, FR-007, FR-008)

#### L-AC-001: Create Institution

```
GIVEN valid institution request with institutionId, name, BIC, and optional fields
WHEN POST /api/v1/institutions is called
THEN institution is created with status 201
AND unique id is returned
```

#### L-AC-002: Duplicate Institution Rejected

```
GIVEN an existing institution with the same institutionId
WHEN POST /api/v1/institutions is called
THEN response status is 409
```

#### L-AC-003: List and Filter Institutions

```
GIVEN multiple institutions with different networks and active statuses
WHEN GET /api/v1/institutions is called
THEN all are returned
AND filtering by network or isActive returns correct subset
```

#### L-AC-004: Update Institution

```
GIVEN an existing institution
WHEN PUT /api/v1/institutions/{id} with updated fields
THEN the institution is updated with status 200
```

#### L-AC-005: Soft-Delete Institution

```
GIVEN an existing institution
WHEN DELETE /api/v1/institutions/{id}
THEN status is 204
AND the institution is marked inactive
```

#### L-AC-006: Institution Lookup by Business ID

```
GIVEN an active institution
WHEN GET /api/v1/institutions/lookup?institutionId=X
THEN the matching institution is returned with status 200
```

#### L-AC-007: Network Support Validation

```
GIVEN an institution exists but does not support a specific network
WHEN validating the institution for that network
THEN validation fails with specific error
```

#### L-AC-008: Unauthorized Access

```
GIVEN request without valid OAuth token
WHEN any EPIC-03 endpoint is called
THEN status is 401
```

## Implementation Plan

### Phase 1: New Error Codes and DTOs

| Step | Task | Verification |
|------|------|-------------|
| 1.1 | Add MSG-010 (institution not found) to ErrorCode enum | Unit test for new enum value |
| 1.2 | Add MSG-011 (institution inactive) to ErrorCode enum | Unit test |
| 1.3 | Add MSG-012 (network not supported for institution) to ErrorCode enum | Unit test |
| 1.4 | Create `InstitutionRequest` DTO with validation annotations | DTO validation test |
| 1.5 | Create `InstitutionResponse` DTO with `fromEntity()` mapping | Serialization test |
| 1.6 | Create `InstitutionNotFoundException` and `DuplicateInstitutionException` | Exception test |

### Phase 2: Service Layer

| Step | Task | Verification |
|------|------|-------------|
| 2.1 | Write failing tests for `InstitutionService` | Tests fail red |
| 2.2 | Implement `createInstitution()` with uniqueness check | Service test passes |
| 2.3 | Implement `getInstitution()` and `listInstitutions()` with filters | Service tests pass |
| 2.4 | Implement `updateInstitution()` | Service test passes |
| 2.5 | Implement `deleteInstitution()` (soft-delete) | Service test passes |
| 2.6 | Implement `findByInstitutionId()` and `validateInstitution()` | Service tests pass |

### Phase 3: Controller Layer

| Step | Task | Verification |
|------|------|-------------|
| 3.1 | Write failing controller tests | Tests fail red |
| 3.2 | Implement `InstitutionController` with all endpoints | Controller tests pass |
| 3.3 | Add exception handling for new error codes in `GlobalExceptionHandler` | Exception handler test |

### Phase 4: Database Migration

| Step | Task | Verification |
|------|------|-------------|
| 4.1 | Create `V3__Enrich_institutions.sql` with additional sample data | Flyway migration runs |
| 4.2 | Add repository integration tests | Testcontainers tests pass |

### Phase 5: EPIC-01 Integration

| Step | Task | Verification |
|------|------|-------------|
| 5.1 | Wire `InstitutionService` into `MessageCreationService` | Existing EPIC-01 tests still pass |
| 5.2 | Add institution validation call in message creation flow | Service test for institution validation |
| 5.3 | E2E test: valid institution → message created successfully | E2E test passes |
| 5.4 | E2E test: invalid institution → MSG-010 error | E2E test passes |

### Phase 6: Verification

| Step | Task | Verification |
|------|------|-------------|
| 6.1 | Run full test suite | All EPIC-01 + EPIC-02 + EPIC-03 tests pass |
| 6.2 | Verify OpenAPI documentation | Swagger shows all new endpoints |

## Technical Questions

**TQ-1: Where should institution validation be called in the EPIC-01 message creation flow? (select one)**

[ ] **In MessageCreationService** — Add institution validation call directly in `createMessage()`, after message definition validation. Keeps all validation calls in one service. ← recommended
[ ] **In MessageValidationService** — Add institution validation alongside field-level validation. Keeps validation logic together.
[ ] **As a separate validation step in MessageController** — Call InstitutionService from the controller before delegating to MessageCreationService.

Blocks: Phase 5 (EPIC-01 Integration)
Impact: (high-impact)

Rationale for recommendation: Adding it to `MessageCreationService` is cleanest — the service already has `validateMessageDefinition()` as a private method. Adding `validateInstitution()` alongside it keeps the pattern consistent. `MessageValidationService` handles field-level syntax/format validation; institution existence is a business-level check that belongs in the message creation flow.

**TQ-2: Should the lookup endpoint use caching? (select one)**

[ ] **Yes, Caffeine cache (same as EPIC-01/EPIC-02)** — Cache `findByInstitutionId` results for 300s ← recommended
[ ] **No caching** — Institutions change rarely, DB query is indexed and fast
[ ] **Cache-Control HTTP header only** — Let clients/reverse proxy decide

Blocks: Phase 3
Impact: (standard)

Rationale for recommendation: Same pattern as EPIC-02's definition lookup. Institutions change rarely, and this endpoint is called on every message creation. Caffeine is already configured.

## New Error Codes

| Code | Message (Persian) | Message (English) | HTTP Status |
|------|-------------------|-------------------|-------------|
| MSG-010 | مؤسسه فرستنده یافت نشد | Sender institution not found | 400 |
| MSG-011 | مؤسسه فرستنده فعال نیست | Sender institution is inactive | 400 |
| MSG-012 | مؤسسه از شبکه انتخاب‌شده پشتیبانی نمی‌کند | Institution does not support the selected network | 400 |

## Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| EPIC-01 integration adds new validation that could break existing message creation | High | Phase 5 dedicated to integration; existing 25 EPIC-01 tests act as regression suite |
| `supportedNetworks` mapping uses `@ElementCollection` — separate join table from institutions table | Low | Matches existing stub; query methods already handle it via `MEMBER OF` JPQL |
| Institution lookup duplicates validation in both service and controller | Low | Controller just proxies; all business logic in service |

## Decision Log

| Date | Decision | Rationale |
|------|----------|-----------|
| — | — | (First iteration — no questions answered yet) |

## Iteration History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-07-27 | Initial spec generated from EPIC-03 requirements in PRD |
