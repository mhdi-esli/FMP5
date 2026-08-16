# Coding Guidelines

**Last Updated:** 2026-08-16
**Completeness:** 100% (22/22 required coding-questionnaire questions answered)
**Sources:** `standards/00_Coding_Guidelines_Questionnaire.md`, `standards/reference/SWA_101-comm-standards.md`, `brainstorm/Epic_PRD.md`

Authoritative source for coding standards. All advisors and skills must consult this before making code-quality recommendations. Every rule traces to a confirmed answer or a cited SWA_101 section.

---

## Coding Standards

| Aspect | Decision | Source |
|--------|----------|--------|
| Language / framework | Java 21 / Spring Boot 3.3.2 now → **Java 25 / SB 4.1.0** (EPIC-04) | verified / PRD |
| Build | Maven; UTF-8 everywhere | — |
| Formatting | 4 spaces, 120-char lines, K&R braces | Coding Q11 |
| Static analysis | **Checkstyle + Spotless + JaCoCo** added to build | Coding Q9 + Q5 |
| Build gate | Any violation fails `mvn verify` | Coding Q10 |
| Lombok | Allow `@Slf4j`,`@RequiredArgsConstructor`,`@Builder`,`@Getter`/`@Setter`; forbid `@Data`,`@AllArgsConstructor`,`@NoArgsConstructor` on entities | Coding Q16 |

**SWA_101 compliance (mandatory, from SWA_101 §1–§6):** `kebab-case` URL paths + `camelCase` query params (§1); `camelCase` JSON fields, ISO 8601 UTC no offset (§6); **never `float`/`double` for money — `BigDecimal`** (§6); string enums, never numeric (§6); empty arrays `[]` never `null`, `errorList` always present (§3/§6); allowed HTTP status set, **400 not 422** for input validation (§4); pagination under `metadata.pagination` (§5).

**Hard Rules** (Coding Q17 — seven confirmed + one added): (1) no magic numbers; (2) no raw strings — use constants; (3) no swallowed exceptions; (4) no `System.out` — use SLF4J; (5) no mutable service state; (6) no business logic in controllers; (7) no direct entity exposure; **(8) never log a full request/response body** (from SWA_101 §10).

### Response Envelope (from SWA_101 §3)
```java
public record ResponseEnvelope<T>(T resultData, String message, List<ErrorItem> errorList) {}
```

---

## Naming Conventions

| Entity | Convention | Example |
|--------|------------|---------|
| Classes | UpperCamelCase | `MessageController`, `ValidationService` |
| Methods / variables | lowerCamelCase | `validateMessage`, `messageType` |
| Constants | UPPER_SNAKE_CASE | `MAX_MESSAGE_LENGTH` |
| Packages | lower.case | `com.bank.messaging` |
| Test methods | `methodName_scenario_expectedResult` | Coding Q2 |

- **Class scheme confirmed, plus SWA_101 envelope DTO suffixes** — `{Entity}Envelope`, `ErrorItem` (Coding Q1).
- **Package layout by technical layer, as it exists today** — `config`, `controller`, `dto`, `entity`, `enums`, `exception`, `repository`, `service`; phantom `records/`,`validator/`,`util/` dropped (Coding Q3, Arch Q20).
- **Error codes:** issuer **`FMP`**, integer codes ≥201, `MSG-XXX` renumbered from 201 preserving order (Arch Q7/Q8). *(Corrects the earlier `MGS`.)*
- **Comments/Javadoc in English; Persian only for user-facing messages** (Coding Q20). Javadoc required on public APIs, service methods, and non-obvious business logic (Coding Q19).

---

## Testing Practices

| Aspect | Target | Source |
|--------|--------|--------|
| Pyramid | ~70% unit / ~20% integration / ~10% E2E | Coding Q4 |
| Coverage | **80% line, enforced by JaCoCo — build fails below** | Coding Q5 |
| TDD | Failing test → minimal code → refactor; **never weaken a test to force a pass** | Coding Q6 |
| Architecture tests | ArchUnit — violations fail `mvn test` | Arch Q2 |

**Required per new endpoint (Coding Q7):** service unit tests (Mockito); `@WebMvcTest` controller slice; Testcontainers integration (real Postgres); Pact contract tests; Persian-message assertions.
**Conformance (Coding Q8):** a shared fixture asserts the envelope shape (`resultData`/`message`/`errorList`) on every endpoint.

---

## Design Patterns

| Pattern | When to use |
|---------|-------------|
| Layered architecture | Controller → Service → Repository, strictly downward |
| DTO pattern (records) | Never expose entities; DTOs are `record` types |
| Global exception handler | `@ControllerAdvice` for all exceptions |
| Bean Validation + custom validators | Request validation (JSR-380) |
| Idempotency | Header key, Redis-backed replay, SHA-256 fingerprint (from SWA_101 §8) |
| Response envelope | All API responses (from SWA_101 §3) |

---

## Version Control & Process

- **Branching:** Trunk-based — short-lived feature branches merged to `main` daily (Coding Q13).
- **PR approval:** one approving review before merge (Coding Q12).
- **Commits:** Conventional Commits (`feat:`,`fix:`,`docs:`,`refactor:`); doc changes ride in the same commit/PR as the code (Coding Q14, SAW_102 §2).
- **Definition of Done (Coding Q15):** all tests pass; quality gates green (coverage + static analysis); OpenAPI spec updated in the same PR; an ADR written if a significant architectural decision was made; Persian user-facing messages reviewed by a Persian speaker.

---

## Open Issues

- **CI/CD pipeline** `[TBD — needs input]` — see the Architecture Reference Open Issues.
- SpotBugs / SonarQube not adopted (Coding Q9 left them unchecked) — a deliberate choice, revisit if quality scanning is later required.

---

## Iteration History

- **2026-08-16** — Restructured to the set-standards section list (Coding Q19-equivalent). Corrected code-contradicted claims (Arch Q20): `Spring Boot 4.x`→ **3.3.2**, `Java 21+`→ **21**, issuer `MGS`→ **`FMP`**; noted EPIC-04 stack target as a labeled carryover. **Added JaCoCo** to reconcile the Coding Q5 (coverage-enforced) vs Q9 (JaCoCo unticked) conflict — user decision 2026-08-16: enforce with JaCoCo. Added envelope DTO suffixes (Q1), Hard Rule #8 (Q17), Version Control & Process section (Q12–Q15), and compressed the prior 573-line doc to the ~150-line cap by cutting long code samples (Q18 — the code itself is the better example).
- **2026-08-12** — First questionnaire pass: added SWA_101 compliance, error-code mapping, 80% coverage target.
