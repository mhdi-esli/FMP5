# Architecture Questionnaire

**Created:** 2026-08-10
**Feeds:** `.claude/_architecture-reference.md`
**Instructions:** Mark exactly one `[x]` per `(select one)` question. For
`(select all that apply)`, mark as many as apply. Leave `[ ]` untouched for
options you reject. A `← recommended` label is a suggestion only — it is
**not** an answer until you check it.

---

## Context you should know before answering

**Sources already confirmed (you do NOT need to answer questions about these):**
- `set-standards/reference/SWA_101-comm-standards.md` — inter-service communication
- `set-standards/reference/SAW_102-arch-doc-standards.md` — architecture documentation

These are authoritative policy documents. Their rules are carried into the
reference docs with citations, not offered as choices.

**No `brainstorm/Epic_PRD.md` exists**, so nothing could be carried over from a
prior confirmed decision. `docs/PRD.md` exists but has no Agentic Decisions
section. Everything below therefore needs a fresh answer.

**Conflicts detected between shipped code and SWA_101** (verified 2026-08-10):

| # | SWA_101 rule | Current code | Affects |
|---|---|---|---|
| 1 | §1 — **NEVER** an `/api/` prefix; version as `/v1/` | All 3 controllers use `/api/v1/...` | `MessageController`, `InstitutionController`, `MessageDefinitionController`, `CLAUDE.md` |
| 2 | §3 — response envelope `resultData` / `message` / `errorList` | No envelope fields anywhere in `src/` | All controllers + DTOs |
| 3 | §3 — errors are `issuer` (2–6 upper) + integer `code` ≥ 201 | String codes `MSG-001` … `MSG-012` | `ErrorCodes` enum, all validation |
| 4 | §2/§8 — `Idempotency-Key` required, server replays cached result | Not implemented | New cross-cutting concern |
| 5 | §10 — OpenTelemetry SDK + `traceparent` mandatory | Not implemented | New cross-cutting concern |
| 6 | §9 — request payload **MUST** be signed by sender | Not implemented | New cross-cutting concern |

**Also note:** the existing `.claude/_architecture-reference.md` (dated
2026-07-26) records "Spring Boot 4.x", but `pom.xml` pins **3.3.2**. It also
lists `records/`, `validator/`, and `util/` packages that do not exist in
`src/main/java/com/bank/messaging/`.

---

## Section 1 — Architecture Style & Boundaries

### Q1. Which architecture style is authoritative for this project? *(required — select one)*

- [ ] Layered (controller → service → repository) — matches the shipped code today  ← recommended
- [ ] Hexagonal / Ports & Adapters — domain core with driven/driving adapters
- [ ] Clean Architecture — entities / use-cases / interface-adapters / frameworks
- [ ] Modular Monolith — feature modules with enforced internal boundaries

### Q2. How are module boundaries enforced? *(required — select one)*

- [ ] Convention only — package layout, reviewed by humans in PR
- [ ] ArchUnit tests in the build — violations fail `mvn test`  ← recommended
- [ ] Maven multi-module — compile-time isolation
- [ ] Not enforced for now — revisit when the service grows

### Q3. What is the dependency-direction rule? *(required — select one)*

- [ ] Strictly downward: controller → service → repository; no upward or skip-level calls  ← recommended
- [ ] Downward, but controllers may call repositories directly for read-only queries
- [ ] Dependencies point inward to the domain; outer layers depend on interfaces owned by inner layers
- [ ] No formal rule — reviewer discretion

### Q4. May a service call another service in the same application? *(required — select one)*

- [ ] Yes, freely — services compose by direct injection  ← recommended
- [ ] Yes, but only via an interface, never a concrete class
- [ ] No — orchestration happens only in a dedicated orchestrator/facade service
- [ ] No — services are strictly independent; the controller orchestrates

---

## Section 2 — SWA_101 Conformance Posture

> This section decides *when* the conflicts in the table above get fixed, not
> *whether* the SWA_101 rules are correct — they are already authoritative.

### Q5. What is the conformance posture for the three already-shipped epics (EPIC-01/02/03)? *(required — select one)*

- [ ] **Retrofit now** — a dedicated remediation epic brings existing endpoints to SWA_101 before any new epic starts
- [ ] **Grandfather + forward-conform** — existing endpoints stay as-is under `/api/v1/`; all *new* endpoints follow SWA_101; an ADR records the exception and its removal schedule  ← recommended
- [ ] **Retrofit at next major version** — conform when `/v2/` is cut; `/api/v1/` frozen and deprecated
- [ ] **Full immediate rewrite** — breaking change accepted now, consumers coordinated

### Q6. Are the legacy envelope fields `RsCode` and `IsSuccess` included in responses? *(required — select one)*

SWA_101 §3 marks both as "LEGACY ONLY — include for backward compat, remove on schedule."

- [ ] Yes — include both, for compatibility with existing Dotin consumers
- [ ] Yes — include both, and record the removal date in an ADR  ← recommended
- [ ] No — this is a new API surface with no legacy consumers; omit them
- [ ] [TBD — needs input from the consuming International Team]

### Q7. What `issuer` code identifies this service in the SWA_101 error object? *(required — free text)*

SWA_101 §3 requires 2–6 uppercase characters, unique per product/service.
Custom error codes must start at **201** (1–200 are globally reserved).

**Answer:** `________`  (e.g. `FMP`, `MSG`, `SWIFT`)

### Q8. How do the existing `MSG-XXX` error codes map to SWA_101 integer codes? *(required — select one)*

- [ ] Renumber from 201 upward, preserving the existing order (`MSG-001` → 201, `MSG-003` → 202, …)  ← recommended
- [ ] Renumber by category, leaving gaps for growth (validation 201–219, definition 220–239, institution 240–259)
- [ ] Keep `MSG-XXX` in the `details.path` field and assign a single generic integer code per HTTP status
- [ ] Deferred — depends on the Q5 posture; do not map yet

---

## Section 3 — Cross-Cutting Concerns

### Q9. When is `Idempotency-Key` handling (SWA_101 §2/§8) implemented? *(required — select one)*

- [ ] Now — as a shared filter/interceptor, before the next epic  ← recommended
- [ ] Next epic — accepted as a known gap, tracked by an ADR
- [ ] Only on endpoints that mutate state (`POST`/`PUT`), never on `GET`
- [ ] Deferred indefinitely — this service has no retry-sensitive consumers

### Q10. Where is the idempotency key + response cached? *(required — select one)*

- [ ] PostgreSQL table — survives restart, works across instances  ← recommended
- [ ] Caffeine (the existing in-memory cache) — simple, lost on restart, wrong under multi-instance
- [ ] Redis — shared across instances, requires new infrastructure
- [ ] [TBD — needs input]

### Q11. Is a body **fingerprint** stored alongside the idempotency key? *(required — select one)*

SWA_101 §8 recommends it, to return `409`/code `105` on a payload mismatch.

- [ ] Yes — SHA-256 of the canonicalized request body  ← recommended
- [ ] Yes — checksum over selected business fields only
- [ ] No — key-only; treat any reuse of the key as a replay
- [ ] [TBD — needs input]

### Q12. When is OpenTelemetry + `traceparent` (SWA_101 §10) implemented? *(required — select one)*

- [ ] Now — Spring Boot OTel starter, before the next epic  ← recommended
- [ ] Next epic — accepted gap, tracked by an ADR
- [ ] Only propagate `traceparent` for now; defer span export to a collector
- [ ] Deferred indefinitely

### Q13. When is request-payload signing (SWA_101 §9) implemented? *(required — select one)*

This is the heaviest lift in the standard and needs a key-distribution answer.

- [ ] Now — blocking requirement before the next epic
- [ ] Next epic — accepted gap, tracked by an ADR with a named owner  ← recommended
- [ ] Only for inter-service (service-to-service) calls, not for calls from the International Team
- [ ] [TBD — needs input from the security/platform team]

### Q14. Does this service publish or consume async messages? *(required — select one)*

Determines whether SWA_101 §7 channel naming and a SAW_102 AsyncAPI spec apply at all.

- [ ] No — REST only; async is out of scope  ← recommended
- [ ] Yes — publishes domain events (e.g. message-created / validation-failed)
- [ ] Yes — consumes commands from another system
- [ ] Yes — both publishes and consumes
- [ ] Not yet, but planned within the next two epics

### Q15. If async applies, what are the `system` and `domain` segments of the channel name? *(optional — free text)*

SWA_101 §7 pattern: `corridor.[system].[domain].{component}.[event|command].[topic|queue].v[N]`

**system:** `________`  (e.g. `core`, `esb`, `switch`)
**domain:** `________`  (e.g. `messaging`, `deposit`)

---

## Section 4 — Documentation Adoption (SAW_102)

### Q16. SAW_102 §1 phases artifacts differently for new vs. existing products. Which applies here? *(required — select one)*

Three epics have already shipped, so "existing product" is defensible; the repo is also only weeks old.

- [ ] **Existing product** — phased rollout: C4 in Phase 1, ADRs in Phase 2, Context Map / OpenAPI / AsyncAPI in Phase 3  ← recommended
- [ ] **New product** — all artifacts required from day 1
- [ ] Existing product, but accelerate ADRs to Phase 1 (decisions are being made now and will be lost otherwise)
- [ ] [TBD — needs input]

### Q17. SAW_102 §2 mandates docs under `/documents/`. The repo currently has `docs/` and `specs/`. How is this reconciled? *(required — select one)*

- [ ] Create `/documents/` per SAW_102; leave `docs/` and `specs/` as SDD working artifacts  ← recommended
- [ ] Rename `docs/` → `documents/` and restructure to the SAW_102 subfolder layout
- [ ] Keep `docs/`, and record a documented deviation from SAW_102 §2 in an ADR
- [ ] [TBD — needs input]

### Q18. Are the C4 / ERD diagrams actually hosted on Confluence as SAW_102 requires? *(required — select one)*

SAW_102 §3/§9 mandate draw.io on Confluence for C4 and ERD, with `README.md` files holding the links.

- [ ] Yes — Confluence is available; diagrams go there and READMEs link to them  ← recommended
- [ ] No Confluence access — diagrams stay in-repo, deviation recorded in an ADR
- [ ] Confluence exists but is not yet provisioned for this team — link placeholders for now
- [ ] [TBD — needs input]

---

## Section 5 — Handling the Existing Reference Documents

### Q19. `.claude/_architecture-reference.md` and `.claude/_coding-guidelines.md` already exist (dated 2026-07-26, written before these standards). How should they be treated? *(required — select one)*

- [ ] **Restructure to the skill's section list, preserving every still-accurate decision**, and log all changes in Iteration History  ← recommended
- [ ] Rewrite from scratch using only answers in these questionnaires — discard unconfirmed prior content
- [ ] Leave them untouched; write the SWA_101/SAW_102 rules into new, separate files
- [ ] Restructure, but first show me a diff of what would be dropped

### Q20. The existing docs contain claims contradicted by the code (Spring Boot 4.x vs. 3.3.2; `records/`, `validator/`, `util/` packages that do not exist). How are these resolved? *(required — select one)*

- [ ] Correct them to match the verified code, and note the correction in Iteration History  ← recommended
- [ ] The doc states intent, not current state — keep them as the target and mark the gap under Open Issues
- [ ] Drop the contradicted claims entirely and mark each `[TBD — needs input]`
- [ ] [TBD — needs input]

### Q21. Should a UX/design reference be created? *(required — select one)*

The skill omits one by default unless there is a real user-facing interface.

- [ ] No — this is a headless REST API; Swagger UI is a developer tool, not a product surface  ← recommended
- [ ] Yes — there is a user-facing interface not visible in this repo
- [ ] [TBD — needs input]

---

## Section 6 — Optional Context

### Q22. Target deployment platform *(optional — select one)*

The existing doc claims "Docker on standalone servers, path to Kubernetes" — unconfirmed.

- [ ] Docker on standalone servers, single instance
- [ ] Kubernetes
- [ ] Docker now, Kubernetes within the year  ← recommended
- [ ] [TBD — needs input]

### Q23. Anything else that constrains the architecture and is not captured above? *(optional — free text)*

Regulatory constraints, data-residency rules, mandated platform services, capacity targets, hard deadlines:

```
(your answer here)
```
