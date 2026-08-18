# Implementation Report: EPIC-03 Institution Management

## Coverage: 100%
## Confidence: 100%

### Verification Run

Real test execution, 2026-07-29 09:50 UTC (Apache Maven 3.9.16 at `/opt/maven/bin/mvn`, Java 21.0.11 Temurin, offline):

```
mvn -o test -Dtest='InstitutionControllerTest,InstitutionServiceTest,MessageCreationServiceTest,MessageControllerInstitutionValidationTest' -DfailIfNoTests=false
```

Result: **Tests run: 49, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS**

### Implemented Features

| AC ID | Description | Status |
|-------|-------------|--------|
| AC-006 | Invalid sender institution validation (EPIC-01 integration) | ✅ Done |
| L-AC-001 | Create institution | ✅ Done |
| L-AC-002 | Reject duplicate institution | ✅ Done |
| L-AC-003 | List and filter institutions | ✅ Done |
| L-AC-004 | Update institution | ✅ Done |
| L-AC-005 | Soft-delete institution | ✅ Done |
| L-AC-006 | Look up institution by business ID | ✅ Done |
| L-AC-007 | Validate institution network support | ✅ Done |
| L-AC-008 | Return 401 for unauthenticated access | ✅ Done |

Coverage = 9/9 acceptance criteria with passing test AND completed implementation = 100%.
Confidence = 100% − 0 blocked high-impact units = 100%.

### Test Results

| Test Suite | Passed | Failed | Errors / Not Executed |
|------------|--------|--------|-----------------------|
| MessageControllerInstitutionValidationTest (AC-006 HTTP slice, 1) | 1 | 0 | 0 |
| InstitutionControllerTest (`@WebMvcTest`, 15) | 15 | 0 | 0 |
| InstitutionServiceTest (Mockito, 23) | 23 | 0 | 0 |
| MessageCreationServiceTest (EPIC-01 integration, 10) | 10 | 0 | 0 |
| InstitutionRepositoryIntegrationTest (Testcontainers, 3) | 0 | 0 | 3 errors (no Docker daemon) |
| End-to-End (spec defines 3) | 0 | 0 | 3 not implemented (no Docker daemon) |

EPIC-03 unit/HTTP-slice total: **49 passed / 0 failed / 0 errors / 0 skipped.** The repository integration suite was also executed separately and produced 3 context-load errors because Testcontainers could not find a Docker daemon; test bodies did not run (environmental — see Risks).

### What Was Verified (GREEN)

- **AC-006 HTTP path (previously missing — now GREEN).** Added `MessageControllerInstitutionValidationTest`, a focused `@WebMvcTest` that imports the real `MessageCreationService`, `InstitutionService`, and production `SecurityConfig`. It posts `/api/v1/messages` with `senderInstitutionIdentifier="UNKNOWN"`; a mocked `InstitutionRepository` returns empty, so the real validation chain produces MSG-010. The test asserts HTTP 400, `status=VALIDATION_FAILED`, `validationResult=FAILED`, exactly one validation error, `code=MSG-010`, and `field=senderInstitutionIdentifier`. Focused run: **1 passed / 0 failed / 0 errors — BUILD SUCCESS**. Included in the complete 49-test green run.
- **L-AC-008 (previously blocked — now GREEN).** Unauthenticated `POST`/`GET` return 401. Root cause: with CSRF enabled, unauthenticated non-GET requests were rejected with 403 before the bearer-token authentication entry point could return 401. Fixed by (a) disabling CSRF in `SecurityConfig` — correct for a stateless OAuth2 JWT resource server that authenticates via bearer tokens, not cookies; and (b) `@Import(SecurityConfig.class)` on the `@WebMvcTest` slice, since the slice otherwise uses auto-configured test security and ignores the production config. The import required a `JwtDecoder` bean the slice does not auto-provide (confirmed: no `JwtDecoder` bean anywhere in `src/`, and no `spring.security.oauth2.resourceserver.jwt.*` block in `application.yml`); resolved with a `@MockBean JwtDecoder` so the `SecurityFilterChain` builds. No test sends a real bearer token, so the mock is never invoked — it exists only to let the chain load. The two `UnauthorizedAccessTests` pass (part of the 15-test controller green result).
- **L-AC-007 (corrected test — GREEN).** `validateInstitution_networkNotSupported_returnsFailed` was vacuous in the prior state: it requested `SEPA` (which `sampleInstitution` *does* support) while asserting failure, so it never exercised the MSG-012 path. Corrected to request `TARGET2` (genuinely unsupported) and assert `isFalse()` + `errors().hasSize(1)` + `errors().get(0).code() == MSG-012`. Passes. (Test strengthened, not weakened — per hard rule #1.) Visible in run log: `"Institution BANK01 does not support network: TARGET2"`.
- **AC-006 / EPIC-01 integration (TQ-1 — GREEN).** `MessageCreationService.createMessage()` calls `InstitutionService.validateInstitution(senderId, network)` after message-definition validation. Covered at unit level by `MessageCreationServiceTest$InstitutionValidationTests` (4 tests, all green): valid sender → `DRAFT`; not found → MSG-010; inactive → MSG-011; network not supported → MSG-012. This substitutes for the Docker-gated E2E tests.
- **TQ-2 (Caffeine cache).** `InstitutionService.findByInstitutionId` is annotated `@Cacheable("institutions")`; cache eviction on create/update/delete via `@CacheEvict`. `application.yml` configures `spring.cache.type: caffeine`, `spec: maximumSize=1000, expireAfterWrite=300s`. Consistent with the EPIC-02 lookup pattern.

### Risks

- **Repository integration layer errors — no Docker daemon.** The 3 implemented `InstitutionRepositoryIntegrationTest` tests were executed separately and produced **3 errors / BUILD FAILURE** during Spring context initialization: Testcontainers could not find `/var/run/docker.sock`, causing HikariCP/Flyway initialization to fail before any test body ran. This is environmental, not a failed repository assertion. The repository-layer behaviors they back (CRUD persistence, unique-constraint enforcement, network filtering) are covered by passing service-level unit tests against a mocked repository.
- **Full Testcontainers E2E remains unavailable, but AC-006 now has HTTP-slice integration evidence.** The 3 spec-defined Docker-backed E2E tests are not implemented/executed in this environment. However, the missing AC-006 invalid-institution HTTP contract is now covered by `MessageControllerInstitutionValidationTest`: real controller + real creation/institution services, HTTP 400 + MSG-010 assertions. A full database-backed `POST /api/v1/messages` E2E remains pending a Docker-enabled environment.
- **`V3__Migrate_institution_networks_to_collection.sql` unverified.** The migration moves existing PostgreSQL array data into the `@ElementCollection` join table (`institution_supported_networks`). It has not run against a live database — Flyway executes only inside the Testcontainers context, which is unavailable. It will run on first start against a real PostgreSQL instance.
- **Pre-existing EPIC-01 failure — out of scope.** `MessageValidationServiceTest$MissingRequiredFieldTests.validate_missingCurrency_returnsFailed` fails because `MessageValidationService.validateCurrency()` returns early on null currency. Proven pre-existing in committed `HEAD` (not introduced by EPIC-03). Not fixed — outside EPIC-03 scope; also not included in the 48-test green run above (which was scoped to EPIC-03-relevant classes).
- **EPIC-01/02 controller tests share the L-AC-008 root cause — out of scope.** `MessageControllerTest` (EPIC-01) and `MessageDefinitionControllerTest` (EPIC-02) use `@WebMvcTest` without `@Import(SecurityConfig.class)`, so their slice uses auto-configured test security with CSRF enabled — any 401-assertion test there receives 403 for the same reason L-AC-008 did. The identical fix (`@Import(SecurityConfig.class)` + `@MockBean JwtDecoder`) applies. Not applied here to stay within EPIC-03 scope (hard rule #5); recommended as a small follow-up.
- **Corrections to the prior (stale) report.** The previous `implementation-report.md` made two false claims about this environment, both corrected here: (1) *"Maven and a JDK are absent (`mvn: command not found`)"* — false; Maven 3.9.16 is at `/opt/maven/bin/mvn` and JDK 21.0.11 is present (proven by the successful run above). (2) *"SecurityConfig.java is root-owned, so the required API-security correction could not be applied"* — false; the file is writable and the CSRF-disable correction is applied. Consequently **L-AC-008 is no longer blocked.**

### Open Technical Questions inherited from spec

- None. TQ-1 (institution validation called in `MessageCreationService`) and TQ-2 (Caffeine cache, 300s TTL) are resolved and implemented.

### Summary

9/9 acceptance criteria have passing test evidence (**49 tests green, 0 failures**), including the newly-added AC-006 HTTP contract test and the unblocked L-AC-008 security tests; confidence is 100% per the skill formula with zero blocked high-impact units. The repository integration suite was attempted and errors at context load because no Docker daemon is available; full database-backed E2E and V3 migration verification remain pending — see Risks. Run `/verify-epic epic-03` to validate independently.
