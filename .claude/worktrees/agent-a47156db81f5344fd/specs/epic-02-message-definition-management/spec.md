# Technical Specification: EPIC-02 Message Definition Management

## Confidence Level: 100% — all technical questions resolved

**PRD Confidence Level:** 92%

---

## Architecture

The Message Definition Management feature adds full CRUD lifecycle for message definition templates and field mapping rules. It extends the existing `MessageDefinitionMapping` entity and repository stubs into a complete management subsystem.

```
┌──────────────────────────────────────────────────────────────────┐
│                     Presentation Layer                            │
│  ┌──────────────────────────────────────────────────────────┐    │
│  │  MessageDefinitionController                               │    │
│  │  - GET    /api/v1/message-definitions                     │    │
│  │  - GET    /api/v1/message-definitions/{id}                │    │
│  │  - POST   /api/v1/message-definitions                     │    │
│  │  - PUT    /api/v1/message-definitions/{id}                │    │
│  │  - DELETE /api/v1/message-definitions/{id}                │    │
│  │  - GET    /api/v1/message-definitions/lookup?type=&net=   │    │
│  └──────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│                     Service Layer                                │
│  ┌──────────────────────────────────────────────────────────┐    │
│  │  MessageDefinitionService                                 │    │
│  │  - CRUD operations                                       │    │
│  │  - Active-version lookup (used by EPIC-01)              │    │
│  │  - Version management                                    │    │
│  └──────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│                     Data Layer                                   │
│  ┌──────────────────────────────────────────────────────────┐    │
│  │  MessageDefinitionMappingRepository (existing)           │    │
│  │  - Extended with CRUD query methods                      │    │
│  └──────────────────────────────────────────────────────────┘    │
│  ┌──────────────────────────────────────────────────────────┐    │
│  │  Database (PostgreSQL)                                    │    │
│  │  - message_definition_mappings table (existing)           │    │
│  │  - V2 migration for field_mappings + validation_rules    │    │
│  └──────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────┘
```

## Components

### 1. MessageDefinitionController

| Field | Value |
|-------|-------|
| **Name** | `MessageDefinitionController` |
| **Package** | `com.bank.messaging.controller` |
| **Responsibility** | REST endpoints for managing message definitions |
| **Exposes** | `POST /api/v1/message-definitions` — create definition |
| | `GET /api/v1/message-definitions` — list definitions (with optional filter by messageType, network, isActive) |
| | `GET /api/v1/message-definitions/{id}` — get single definition |
| | `PUT /api/v1/message-definitions/{id}` — update definition |
| | `DELETE /api/v1/message-definitions/{id}` — soft-delete (deactivate) definition |
| | `GET /api/v1/message-definitions/lookup?messageType={type}&network={net}` — lookup active definition (used by EPIC-01) |
| **Consumes** | `MessageDefinitionService` |

### 2. MessageDefinitionService

| Field | Value |
|-------|-------|
| **Name** | `MessageDefinitionService` |
| **Package** | `com.bank.messaging.service` |
| **Responsibility** | Business logic for message definition CRUD and version management |
| **Exposes** | |
| | `createDefinition(request)` — create new definition |
| | `updateDefinition(id, request)` — update existing definition |
| | `getDefinition(id)` — get by ID |
| | `listDefinitions(messageType, network, isActive)` — filtered list |
| | `deleteDefinition(id)` — soft delete (set isActive=false) |
| | `findActiveDefinition(messageType, network)` — lookup used by EPIC-01 |
| **Consumes** | `MessageDefinitionMappingRepository` |

### 3. DTOs (new)

| DTO | Fields | Purpose |
|-----|--------|---------|
| `MessageDefinitionRequest` | messageType, network, version, fieldMappings (JSON string), validationRules (JSON string), isActive | Create/Update input |
| `MessageDefinitionResponse` | id, messageType, network, version, fieldMappings, validationRules, isActive, createdAt, updatedAt | API response |
| `FieldMapping` | fieldName, swiftTag, sepaField, required, maxLength, dataType, validationPattern | Individual field mapping (used within fieldMappings JSON) |

### 4. ValidationRule (value object for JSON structure)

| Field | Purpose |
|-------|---------|
| `ruleName` | e.g. "amount_positive", "currency_iso" |
| `enabled` | boolean |
| `params` | JSON object of rule-specific parameters |

## Dependencies

### Internal

| Dependency | Type | Detail |
|------------|------|--------|
| EPIC-01 Message Creation Service | Consumer | EPIC-01 already uses `MessageDefinitionMappingRepository.findByMessageTypeAndNetworkAndIsActiveTrue()` — once EPIC-02 provides the full CRUD, the stub data in V1 will be managed through the new endpoints |
| `MessageDefinitionMappingRepository` | Extends | Existing repository needs additional query methods for listing |
| Existing `ErrorCode` enum | Shared | Reuses MSG-004, MSG-005 error codes |

### External

| Dependency | Version | Purpose |
|------------|---------|---------|
| Spring Boot Starter Validation | 3.3.2 | Request validation |
| PostgreSQL JSONB | — | Store field mappings and validation rules |

## Boundaries

| In Scope | Out of Scope |
|----------|-------------|
| CRUD for message definitions | Dynamic field-mapping engine at runtime (future epic) |
| Field mappings stored as JSONB | Message rendering/generation from definitions |
| Validation rules stored as JSONB | Workflow/state machine for definition approval |
| Version management (new versions of same type+network) | Definition comparison/diff tooling |
| Lookup endpoint for EPIC-01 integration | Import/export of definitions |
| Soft-delete (deactivate) | Hard delete |

## Data Flow

### Happy Path — Create Definition

```
Client → POST /api/v1/message-definitions
       → MessageDefinitionController
       → MessageDefinitionService.createDefinition()
            → Validate request fields
            → Check uniqueness of (messageType, network, version)
            → Save MessageDefinitionMapping entity
            → Return MessageDefinitionResponse (201 Created)
```

### Happy Path — Lookup Active Definition (EPIC-01 Integration)

```
MessageCreationService → MessageDefinitionService.findActiveDefinition(type, net)
                      → MessageDefinitionMappingRepository.findByMessageTypeAndNetworkAndIsActiveTrue()
                      → Optional<MessageDefinitionMapping>
```

### Error Path — Duplicate Definition

```
Client → POST /api/v1/message-definitions (same type+network+version)
       → MessageDefinitionService
            → Uniqueness check fails
            → Return 409 Conflict with MSG-008 error
```

### Error Path — Not Found

```
Client → GET /api/v1/message-definitions/{id} (non-existent)
       → MessageDefinitionService
            → Entity not found
            → Return 404 Not Found with MSG-009 error
```

### Error Path — No Active Definition (AC-005)

```
MessageCreationService → MessageDefinitionService.findActiveDefinition(type, net)
                      → Repository returns empty
                      → Return ValidationResult.failed with MSG-004 / MSG-005
                      → MessageController returns 400
```

## Testing Strategy

| Layer | Target | Tools |
|-------|--------|-------|
| Unit (Service) | 60% | JUnit 5 + Mockito |
| Unit (Controller) | 20% | Spring WebMvcTest |
| Integration (Repository) | 15% | Testcontainers + SpringBootTest |
| Integration (Full API) | 5% | Testcontainers + full context |

## Unit Tests

### MessageDefinitionService Tests

| Test Name | Scenario | Expected |
|-----------|----------|----------|
| `createDefinition_success_returnsCreatedDefinition` | Valid request with all fields | Saved entity, 201 |
| `createDefinition_duplicateTypeNetworkVersion_throwsException` | Same type+network+version exists | 409 Conflict |
| `createDefinition_withFieldMappings_storesJsonb` | Valid field mappings JSON | JSON stored correctly |
| `createDefinition_withValidationRules_storesJsonb` | Valid validation rules JSON | JSON stored correctly |
| `getDefinition_existingId_returnsDefinition` | Existing ID | Entity returned |
| `getDefinition_nonExistentId_throwsNotFoundException` | Non-existent ID | 404 Not Found |
| `listDefinitions_noFilter_returnsAll` | No query params | All definitions returned |
| `listDefinitions_filterByType_returnsFiltered` | Filter by messageType | Only matching type |
| `listDefinitions_filterByNetwork_returnsFiltered` | Filter by network | Only matching network |
| `listDefinitions_filterByActive_returnsFiltered` | Filter by isActive | Only active/inactive |
| `updateDefinition_success_updatesFields` | Update existing definition | Fields updated |
| `updateDefinition_deactivates_keepsHistory` | Deactivate a definition | isActive=false, entity preserved |
| `deleteDefinition_softDelete_setsInactive` | Delete existing definition | isActive=false |
| `findActiveDefinition_exists_returnsDefinition` | Active definition found | Definition returned |
| `findActiveDefinition_notFound_returnsEmpty` | No active definition | Optional.empty() |
| `findActiveDefinition_inactive_returnsEmpty` | Only inactive matches | Optional.empty() |

### MessageDefinitionController Tests

| Test Name | Scenario | Expected |
|-----------|----------|----------|
| `postMessageDefinition_valid_returns201` | Valid POST | 201 Created, response body |
| `postMessageDefinition_invalidBody_returns400` | Missing required fields | 400 Bad Request |
| `postMessageDefinition_duplicate_returns409` | Duplicate type+network+version | 409 Conflict |
| `getMessageDefinition_existing_returns200` | Existing ID | 200 OK |
| `getMessageDefinition_nonExistent_returns404` | Non-existent ID | 404 Not Found |
| `getMessageDefinitions_noFilter_returns200` | List all | 200 OK |
| `getMessageDefinitions_filtered_returns200` | With query params | 200 OK, filtered |
| `putMessageDefinition_existing_returns200` | Update existing | 200 OK |
| `putMessageDefinition_nonExistent_returns404` | Update non-existent | 404 Not Found |
| `deleteMessageDefinition_existing_returns204` | Soft delete existing | 204 No Content |
| `deleteMessageDefinition_nonExistent_returns404` | Soft delete non-existent | 404 Not Found |
| `lookupMessageDefinition_found_returns200` | Active definition exists | 200 OK |
| `lookupMessageDefinition_notFound_returns404` | No active definition | 404 Not Found |
| `postMessageDefinition_unauthorized_returns401` | No valid token | 401 Unauthorized |

## Integration Tests

| Test Name | Scenario | Expected |
|-----------|----------|----------|
| `saveDefinition_persistsAndRetrieves` | Save + find by ID | Full round-trip with all fields |
| `saveDefinition_fieldMappings_storedAsJsonb` | Save with fieldMappings | JSONB retrievable with correct structure |
| `saveDefinition_validationRules_storedAsJsonb` | Save with validationRules | JSONB retrievable with correct structure |
| `findByTypeAndNetwork_returnsMatching` | Query by type+network | Correct results |
| `findActiveDefinition_returnsOnlyActive` | Mix of active/inactive | Only active returned |
| `uniqueConstraint_duplicate_throwsException` | Same type+network+version | DataIntegrityViolation |
| `updateDefinition_modifiesFields` | Update in DB | Fields changed correctly |
| `softDelete_setsInactive` | Delete preserves row | Row exists with isActive=false |
| `listAll_returnsAllDefinitions` | Multiple definitions saved | All returned |
| `listFiltered_returnsCorrectSubset` | Filtered by type+network | Correct subset |

## End-to-End Tests

| Test Name | Scenario | Expected |
|-----------|----------|----------|
| `fullDefinitionLifecycle` | Create → Get → Update → List → Delete → Verify inactive | Complete CRUD cycle works end-to-end |
| `epic01Integration_lookupDefinition` | Create definition → Verify EPIC-01 can find it via lookup endpoint | Integration with EPIC-01 works |

## Acceptance Criteria Mapping

### AC-005: Missing Message Definition

**PRD Reference:**
```gherkin
GIVEN no active Message Definition for requested message type and network
WHEN POST /api/v1/messages is called
THEN response status is 400
AND validationErrors contains code MSG-004 or MSG-005
```

**Test Strategy:**
This AC is shared with EPIC-01. EPIC-02 provides:
1. A lookup endpoint `GET /api/v1/message-definitions/lookup` that returns the active definition or 404
2. Verification that when no active definition exists, the correct error flows

Tests:
- `lookupMessageDefinition_notFound_returns404` — lookup endpoint returns 404 when no active definition exists
- Full stack test: Create EPIC-01 message with type+network that has no active definition → assert 400 with MSG-004/MSG-005

**Design Approach:**
- The lookup endpoint is a thin wrapper over the existing `MessageDefinitionMappingRepository.findByMessageTypeAndNetworkAndIsActiveTrue()`
- EPIC-01 already handles MSG-004 (definition not found) and MSG-005 (definition inactive) — no changes needed there
- The endpoint is cached with Caffeine (short TTL) since definitions don't change frequently

**Implementation Approach:**
1. Create `MessageDefinitionService.findActiveDefinition(type, network)` wrapping the repository
2. Create lookup endpoint in `MessageDefinitionController`
3. Test both found and not-found cases

### Local Acceptance Criteria (derived from FR-006, FR-012)

#### L-AC-001: Create Message Definition

```
GIVEN valid message definition request with message type, network, version, and optional field mappings
WHEN POST /api/v1/message-definitions is called
THEN definition is created with status 201
AND unique id is returned
AND the definition is persisted and retrievable
```

#### L-AC-002: Duplicate Definition Rejected

```
GIVEN an existing message definition with (type, network, version)
WHEN POST /api/v1/message-definitions with same (type, network, version)
THEN response status is 409
AND error code is MSG-008
```

#### L-AC-003: List and Filter Definitions

```
GIVEN multiple message definitions exist with different types and networks
WHEN GET /api/v1/message-definitions is called
THEN all definitions are returned
AND filtering by messageType and network returns correct subset
```

#### L-AC-004: Update Message Definition

```
GIVEN an existing message definition
WHEN PUT /api/v1/message-definitions/{id} with updated fields
THEN the definition is updated
AND response status is 200
```

#### L-AC-005: Soft-Delete (Deactivate) Definition

```
GIVEN an existing message definition
WHEN DELETE /api/v1/message-definitions/{id}
THEN response status is 204
AND the definition is marked as inactive
AND it is not returned in active lookups
```

#### L-AC-006: Active Definition Lookup

```
GIVEN an active message definition exists for (type, network)
WHEN GET /api/v1/message-definitions/lookup?messageType=X&network=Y
THEN response status is 200
AND the matching active definition is returned
```

#### L-AC-007: Unauthorized Access

```
GIVEN request without valid OAuth token
WHEN any EPIC-02 endpoint is called
THEN response status is 401
```

## Implementation Plan

### Phase 1: New Error Codes and DTOs

| Step | Task | Verification |
|------|------|-------------|
| 1.1 | Add MSG-008 (duplicate definition) and MSG-009 (definition not found) to ErrorCode enum | Unit test for new enum values |
| 1.2 | Create `MessageDefinitionRequest` DTO with validation annotations | Unit test for DTO validation |
| 1.3 | Create `MessageDefinitionResponse` DTO | Serialization test |
| 1.4 | Create `FieldMapping` and `ValidationRule` record/value classes | Unit test for structure |

### Phase 2: Service Layer

| Step | Task | Verification |
|------|------|-------------|
| 2.1 | Write failing tests for `MessageDefinitionService` | Tests fail red |
| 2.2 | Implement `createDefinition()` with uniqueness check | Service test passes |
| 2.3 | Implement `getDefinition()` and `listDefinitions()` with filters | Service tests pass |
| 2.4 | Implement `updateDefinition()` | Service test passes |
| 2.5 | Implement `deleteDefinition()` (soft-delete) | Service test passes |
| 2.6 | Implement `findActiveDefinition()` for EPIC-01 integration | Service test passes |
| 2.7 | Validate JSON syntax on create and update operations | Service test for invalid JSON |

### Phase 3: Controller Layer

| Step | Task | Verification |
|------|------|-------------|
| 3.1 | Write failing controller tests | Tests fail red |
| 3.2 | Implement `MessageDefinitionController` with all endpoints | Controller tests pass |
| 3.3 | Add global exception handling for new error codes | Exception handler test |
| 3.4 | Add OpenAPI documentation annotations | Swagger UI renders correctly |
| 3.5 | Add caching to lookup endpoint | Cache configured in application.yml |

### Phase 4: Database Migration

| Step | Task | Verification |
|------|------|-------------|
| 4.1 | Create `V2__Enrich_message_definition_mappings.sql` with sample field mappings and validation rules data | Flyway migration runs |
| 4.2 | Add repository integration tests | Testcontainers tests pass |

### Phase 5: EPIC-01 Integration

| Step | Task | Verification |
|------|------|-------------|
| 5.1 | Wire `MessageDefinitionService` into `MessageCreationService` (replacing direct repository usage) | Existing EPIC-01 tests still pass |
| 5.2 | Add integration test: create definition → create message → verify success | E2E test passes |
| 5.3 | Add integration test: message with missing definition → verify MSG-004 error | E2E test passes |

### Phase 6: Verification

| Step | Task | Verification |
|------|------|-------------|
| 6.1 | Run full test suite | All 25 EPIC-01 tests + new EPIC-02 tests pass |
| 6.2 | Verify OpenAPI documentation | Swagger shows all new endpoints |
| 6.3 | Verify EPIC-01 integration test | End-to-end flow works |

## Technical Questions

**TQ-1: How should field_mappings and validation_rules JSONB content be modeled in Java? (select one)**

[x] **Raw String** — Keep `String fieldMappings` and `String validationRules` as-is, let consumers parse JSON. Service performs no structural validation on the stored JSON.
[ ] **Structured Object** — Replace with typed `List<FieldMapping>` and `List<ValidationRule>` using a JPA `@Convert` or custom Hibernate `UserType`
[ ] **Hybrid** — Store as raw string but provide a DTO layer for requests/responses that includes optional JSON schema validation at the controller boundary

**Decision:** Raw String. EPIC-02 stores and retrieves; interpretation is EPIC-04's responsibility. Request validation checks JSON syntax only.

**TQ-2: What pagination strategy for list endpoint? (select one)**

[x] **Full list (no pagination)** — Return all matching definitions. Simple, acceptable while total definitions are < 100.
[ ] **Spring Data Pageable** — Standard Spring pagination with page/pageSize/sort query params
[ ] **Cursor-based** — Use last ID as cursor for infinite scroll

**Decision:** Full list. Definitions number in dozens. Pagination can be added later without breaking backward compatibility.

**TQ-3: Should the lookup endpoint use caching? (select one)**

[x] **Yes, Caffeine cache (same as EPIC-01)** — Cache `findActiveDefinition` results for 300s. Low TTL since definitions rarely change, but caching avoids DB load on every message creation.
[ ] **No caching** — Simplest implementation. DB load is minimal given the query is indexed.
[ ] **Cache-Control HTTP header only** — Let clients and reverse proxy decide caching.

**Decision:** Caffeine cache with 300s TTL. Already configured in project; zero new dependencies. Cache evicted on definition updates/deletes.

## New Error Codes

| Code | Message (Persian) | Message (English) | HTTP Status |
|------|-------------------|-------------------|-------------|
| MSG-008 | تعریف پیام تکراری است | Duplicate message definition | 409 |
| MSG-009 | تعریف پیام یافت نشد | Message definition not found | 404 |

## Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| EPIC-01 integration requires no breaking changes | High | Phase 5 dedicated to integration; existing tests act as regression suite |
| Field mappings JSON format may change when EPIC-04 interprets them | Medium | Raw JSON storage avoids coupling Java types to structure; format evolves independently |
| Duplicate definition detection races in concurrent requests | Low | DB unique constraint is the source of truth; service-level check is a fast-path optimization |
| Large field_mappings JSON could impact query performance | Low | JSONB is indexed only if we add a GIN index; definitions are small (~KB) per row |

## Decision Log

| Date | Decision | Rationale |
|------|----------|-----------|
| 2026-07-26 | TQ-1: Raw String for JSONB | Storage doesn't interpret field mappings; EPIC-04 will. Syntax validation added to service layer. |
| 2026-07-26 | TQ-2: Full list, no pagination | Definitions < 100. Cursor/pagination backward-compatible if ever needed. |
| 2026-07-26 | TQ-3: Caffeine cache for lookup | Already configured in project; evict on definition updates. |

## Iteration History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-07-26 | Initial spec generated from EPIC-02 requirements in PRD |
| 1.1 | 2026-07-26 | All 3 TQs answered and folded in. Confidence Level raised from 95% to 100%. Blocked markers removed from Phase 2. Added JSON syntax validation step 2.7. |
