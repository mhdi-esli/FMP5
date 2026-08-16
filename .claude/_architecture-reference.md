# Architecture Reference

**Last Updated:** 2026-08-16
**Completeness:** 100% (20/20 required architecture-questionnaire questions answered)
**Sources:** `standards/00_Architecture_Questionnaire.md`, `standards/reference/SWA_101-comm-standards.md`, `brainstorm/Epic_PRD.md`

Authoritative source for architectural decisions in the Financial Messaging Platform. All advisors and skills must consult this before making recommendations. Every rule traces to a confirmed answer, a PRD carryover, or a cited SWA_101 section.

---

## System Architecture

| Aspect | Decision | Source |
|--------|----------|--------|
| Style | Layered (controller → service → repository) | Arch Q1 |
| Module boundaries | Enforced by ArchUnit tests — violations fail `mvn test` | Arch Q2 |
| Dependency direction | Strictly downward; no upward or skip-level calls | Arch Q3 |
| Service-to-service | Services compose freely by direct injection | Arch Q4 |
| Deployment | Docker now, Kubernetes within the year | Arch Q22 / PRD |
| Communication | REST/HTTP, SWA_101-compliant | from SWA_101 |

**Verified packages** (`com.bank.messaging`): `config`, `controller`, `dto`, `entity`, `enums`, `exception`, `repository`, `service`. The `records/`, `validator/`, `util/` packages from older docs **do not exist** and were dropped (Arch Q20, Coding Q3).

**Protocol selection** (from SWA_101 §11): REST mandatory cross-domain/external; gRPC allowed intra-domain; SOAP never within the Dotin ecosystem.

---

## Major Design Decisions

| Decision | Details | Source |
|----------|---------|--------|
| SWA_101 conformance posture | **Full immediate rewrite** — breaking change accepted now, consumers coordinated | Arch Q5 → ADR |
| URL structure | `/v{major}/{resource}`, **no** `/api/` prefix | from SWA_101 §1 |
| Error object | `issuer` (2–6 upper) + integer `code` ≥201 + `description`; issuer = **`FMP`** | from SWA_101 §3 / Arch Q7 |
| Error renumbering | `MSG-XXX` → 201 upward, preserving existing order | Arch Q8 |
| Legacy envelope fields | `RsCode`/`IsSuccess` **omitted** — new surface, no legacy consumers | Arch Q6 |
| Idempotency | On mutating endpoints (POST/PUT) only, never GET; SHA-256 body fingerprint | from SWA_101 §2/§8 / Arch Q9,Q11 |
| Idempotency store | **Redis** (shared across instances) | Arch Q10 |
| Distributed tracing | OpenTelemetry SDK + `traceparent` **now** (Spring Boot OTel starter) | from SWA_101 §10 / Arch Q12 |
| Request-payload signing | Sender **must sign** request payload; implemented next epic, ADR-tracked, named owner | from SWA_101 §9 / Arch Q13 |
| Async messaging | None today; planned within two epics → then §7 channel naming, `system=core` `domain=messaging` | Arch Q14,Q15 |

**Security** (from SWA_101 §9): OAuth 2.0 Client Credentials via Dotin SSO; JWS tokens, max TTL 4h; validate signature/expiry/scopes; never hardcode the SSO public key (cache + evict-on-error).

---

## Constraints

**Current verified state** (pom.xml, 2026-08-16):

| Constraint | Value |
|------------|-------|
| Language | Java 21 |
| Framework | Spring Boot 3.3.2 |
| Database | PostgreSQL + JSONB, Flyway migrations |
| Caching | Caffeine (in-memory, max 1000, TTL 300s) |
| DTOs | Java `record` (already) |

**Confirmed EPIC-04 migration target** (carried over from Epic_PRD.md — Decision Log #14–16, FR-015–018): Java **25** · Spring Boot **4.1.0** · **Redis** replaces Caffeine · DTOs remain `record`.

**Operational/regulatory:** user-facing validation messages in Persian (project rule); OAuth 2.0 via existing SSO (NFR-005); structured JSON logging + correlation ID to ELK (PRD); response < 500ms (NFR-001); 100 concurrent creates (NFR-002).

---

## Key Dependencies

| Dependency | Purpose | Note |
|------------|---------|------|
| Spring Boot | Framework | 3.3.2 now → 4.1.0 (EPIC-04) |
| PostgreSQL + Flyway | Persistence + migrations | — |
| Caffeine → Redis | Caching + idempotency store | Redis is EPIC-04 target |
| SpringDoc | OpenAPI 3.x generation | — |
| Dotin SSO (OAuth2) | AuthN/AuthZ | JWK endpoint |
| OpenTelemetry SDK | Tracing | to be added (Q12) |
| JUnit 5 · Testcontainers · Pact | Testing | — |
| SLF4J + Logback | JSON logging to ELK | — |

---

## Development Patterns

- **Layered** flow, strictly downward, ArchUnit-enforced; services injected directly (Q1–Q4).
- **Response envelope** `resultData`/`message`/`errorList` on every API response (from SWA_101 §3).
- **Global exception handler** (`@ControllerAdvice`); **Bean Validation** (JSR-380) + custom validators.
- **Idempotency filter/interceptor**, Redis-backed, on mutating endpoints (from SWA_101 §8).
- **OpenTelemetry** propagation; **never log full request/response body** — sanitize/mask PII (from SWA_101 §10).

---

## Open Issues

- **CI/CD approach** `[TBD — needs input]` — left `[TBD]` in the PRD Agentic Decisions; no questionnaire question covers it.
- **SWA_101 endpoint rewrite not yet scoped into an epic.** The full rewrite (remove `/api/`, add envelope, renumber errors to `FMP`+201, idempotency, tracing) is decided (Arch Q5) but EPIC-04's re-verification (AC-011) currently covers only framework/cache/DTO — a remediation epic or expanded EPIC-04 scope is needed.
- **EPIC-04 technical questions** (from Epic_PRD.md): TQ-1 Redis driver/topology rationale; TQ-2 Redis auth/TLS; TQ-3 Spring Boot 4.1.0 ↔ Java 25 compatibility.

---

## Iteration History

- **2026-08-16** — Restructured to the set-standards section list (Arch Q19). Corrected code-contradicted claims (Arch Q20): framework `4.x`→ **3.3.2**, `Java 21+`→ **21**; dropped phantom `records/`/`validator/`/`util/` packages; fixed issuer `MGS`→ **`FMP`** (Arch Q7). Added confirmed EPIC-04 migration target (Java 25 / SB 4.1.0 / Redis) as a labeled carryover alongside current state (user decision: "both, clearly labeled"). Recorded conformance posture = full rewrite (Q5), idempotency store = Redis (Q10), tracing now (Q12), payload signing next epic (Q13 — chosen over the Q23 JWS-only reading). Added CI/CD, rewrite-scope, and EPIC-04 TQs to Open Issues.
- **2026-08-12** — Updated to reflect first questionnaire pass. Removed `/api/` prefix, adopted SWA_101 error format.
