# Coding Guidelines Questionnaire

**Created:** 2026-08-10
**Feeds:** `.claude/_coding-guidelines.md`
**Instructions:** Mark exactly one `[x]` per `(select one)` question. For
`(select all that apply)`, mark as many as apply. Leave `[ ]` untouched for
options you reject. A `← recommended` label is a suggestion only — it is
**not** an answer until you check it.

---

## Context you should know before answering

**Already authoritative — not up for a vote here.** SWA_101 §1–§6, the
header/body-contract part of §7, and §8 route into `.claude/_coding-guidelines.md`
with citations. That means these are already settled and will be written in
regardless of what you answer below:

- `kebab-case` URL path segments, `camelCase` query parameters (§1)
- `camelCase` JSON field names; ISO 8601 UTC dates with no offset (§6)
- **never `float` for monetary values** (§6) — the existing `BigDecimal` usage already complies
- string-based enums, never numeric (§6)
- empty arrays as `[]`, never `null`; `errorList` always present (§3, §6)
- the allowed HTTP status code set, and "do not use 422 for general input validation" (§4)
- pagination metadata under `metadata.pagination`, never in `resultData` (§5)

Answer only what is genuinely open below.

**No `brainstorm/Epic_PRD.md` exists**, so there are no carried-over prior
decisions. Every answer here is fresh.

**Verified repo state (2026-08-10):** Java 21, Spring Boot **3.3.2**, Maven,
Lombok, 19 test classes, **no** Checkstyle / Spotless / JaCoCo / SonarQube /
PMD / SpotBugs plugin in `pom.xml`.

**Note:** the existing `.claude/_coding-guidelines.md` (2026-07-26) is 573 lines
— roughly 4× the skill's ~150-line cap — and is mostly long code samples. It
also claims "Spring Boot 4.x" against the actual 3.3.2. Q18 decides what
happens to it.

---

## Section 1 — Naming Conventions

### Q1. The existing class-naming table (`{Entity}Controller`, `{Entity}{Action}Service`, `{Entity}Request`/`{Entity}Response`, …) matches the shipped code. Confirm it? *(required — select one)*

- [ ] Confirm as written — it is accurate and stays  ← recommended
- [ ] Confirm, but add DTO suffixes for the SWA_101 envelope types (e.g. `{Entity}Envelope`, `ErrorItem`)
- [ ] Replace with a different scheme (specify in Q23)
- [ ] [TBD — needs input]

### Q2. Test method naming *(required — select one)*

The existing doc says `methodName_scenario_expectedResult`.

- [ ] `methodName_scenario_expectedResult` — matches existing tests  ← recommended
- [ ] `should_ExpectedBehavior_when_StateUnderTest`
- [ ] `@DisplayName` prose, method name free-form
- [ ] [TBD — needs input]

### Q3. Java package layout *(required — select one)*

Verified as existing: `config`, `controller`, `dto`, `entity`, `enums`,
`exception`, `repository`, `service`. The old doc also lists `records/`,
`validator/`, `util/` — these **do not exist**.

- [ ] By technical layer, as it exists today — drop the three phantom packages from the docs  ← recommended
- [ ] By technical layer, and actually create `validator/` and `util/` as the code grows
- [ ] By feature (`message/`, `institution/`, `definition/`), each with its own internal layers
- [ ] [TBD — needs input]

---

## Section 2 — Testing

### Q4. Testing pyramid targets *(required — select one)*

- [ ] ~70% unit / ~20% integration / ~10% end-to-end  ← recommended
- [ ] ~60% unit / ~30% integration / ~10% end-to-end
- [ ] ~80% unit / ~20% integration, no separate E2E tier
- [ ] No numeric target — require meaningful coverage of each acceptance criterion instead

### Q5. Line/branch coverage threshold *(required — select one)*

No JaCoCo plugin is configured today, so any threshold here is a new build requirement.

- [ ] 80% line coverage, enforced by JaCoCo — build fails below it  ← recommended
- [ ] 80% line coverage as a reported target, not enforced
- [ ] 70% line coverage, enforced
- [ ] No coverage gate — rely on TDD discipline and review

### Q6. Does the TDD loop in `implement-epic` stay mandatory? *(required — select one)*

- [ ] Yes — failing test first, minimal code to pass, refactor; never weaken a test to force a pass  ← recommended
- [ ] Yes for business logic; tests-after acceptable for config and boilerplate
- [ ] No — tests written alongside or after implementation
- [ ] [TBD — needs input]

### Q7. Which test types are required for a new endpoint? *(required — select all that apply)*

- [ ] Unit tests for the service layer (Mockito)  ← recommended
- [ ] `@WebMvcTest` slice tests for the controller
- [ ] Integration tests against a real Postgres via Testcontainers
- [ ] Contract tests (Pact) — the existing doc lists `pact-jvm-consumer-junit5`, but it is **not** in `pom.xml` today
- [ ] Persian-message assertions — verify user-facing text is actually Persian

### Q8. SWA_101 conformance testing *(required — select one)*

- [ ] A shared test fixture asserts the envelope shape (`resultData`/`message`/`errorList`) on every endpoint  ← recommended
- [ ] Assert the envelope ad hoc, per endpoint test
- [ ] Validate responses against the OpenAPI spec in CI
- [ ] No dedicated conformance test

---

## Section 3 — Static Analysis & Build

### Q9. Which static analysis tools are added to the build? *(required — select all that apply)*

None are configured today — each box is a new `pom.xml` plugin.

- [ ] Checkstyle — enforces the formatting rules in Section 4  ← recommended
- [ ] Spotless — auto-formats, fails the build on unformatted code
- [ ] JaCoCo — coverage measurement (required if you gated on coverage in Q5)
- [ ] SpotBugs or SonarQube — bug/quality scanning (name which one in Q23)
- [ ] None for now — defer all tooling

### Q10. Do static analysis failures break the build? *(required — select one)*

- [ ] Yes — any violation fails `mvn verify`  ← recommended
- [ ] Warnings only — reported, never blocking
- [ ] Errors block; warnings are advisory
- [ ] Not applicable — selected "None for now" in Q9

### Q11. Formatting rules *(required — select one)*

The existing doc specifies 4 spaces, 120-char lines, K&R braces.

- [ ] Confirm as written — 4 spaces, 120 chars, K&R  ← recommended
- [ ] Same, but 100-character lines
- [ ] Adopt google-java-format defaults instead
- [ ] [TBD — needs input]

---

## Section 4 — Code Review & Version Control

### Q12. Pull request approval requirement *(required — select one)*

- [ ] One approving review before merge  ← recommended
- [ ] Two approving reviews before merge
- [ ] One approval, plus a second for security-sensitive paths (auth, signing, key handling)
- [ ] No formal requirement

### Q13. Branching strategy *(required — select one)*

- [ ] Trunk-based — short-lived feature branches merged to `main` daily  ← recommended
- [ ] GitFlow — `develop` / `release` / `hotfix` branches
- [ ] GitHub Flow — feature branch → PR → `main`
- [ ] [TBD — needs input]

### Q14. Commit message convention *(required — select one)*

SAW_102 §2 separately requires that doc changes ride in the **same commit or PR**
as the related code change — that rule applies regardless of your choice here.

- [ ] Conventional Commits (`feat:`, `fix:`, `docs:`, `refactor:`)  ← recommended
- [ ] Free-form, but must reference a ticket ID
- [ ] Free-form
- [ ] [TBD — needs input]

### Q15. Definition of Done for a task *(required — select all that apply)*

- [ ] All tests pass (`mvn test`)  ← recommended
- [ ] Quality gates green — coverage per Q5 and static analysis per Q9/Q10
- [ ] OpenAPI spec updated in the same PR (SAW_102 §2)
- [ ] An ADR written if the task made a significant architectural decision (SAW_102 §4)
- [ ] Persian user-facing messages reviewed by a Persian speaker

---

## Section 5 — Language & Framework Conventions

### Q16. Lombok policy *(required — select one)*

The existing doc allows `@Slf4j`, `@RequiredArgsConstructor`, `@Builder`,
`@Getter`/`@Setter`, and forbids `@Data`, `@AllArgsConstructor`, and
`@NoArgsConstructor` on entities.

- [ ] Confirm the existing allow/deny list as written  ← recommended
- [ ] Confirm, but also forbid `@Builder` on JPA entities specifically
- [ ] Allow all Lombok annotations — no restrictions
- [ ] Remove Lombok entirely — use Java records and explicit constructors
- [ ] [TBD — needs input]

### Q17. The existing "Hard Rules" list (no magic numbers, no raw strings, no swallowed exceptions, no `System.out`, no mutable service state, no business logic in controllers, no direct entity exposure). Confirm? *(required — select one)*

- [ ] Confirm all seven as written  ← recommended
- [ ] Confirm all seven, and add: never log a full request/response body (SWA_101 §10 already mandates this — this vote makes it locally visible too)
- [ ] Confirm with exceptions I will note in Q23
- [ ] [TBD — needs input]

### Q18. The existing `_coding-guidelines.md` is 573 lines against the skill's ~150-line cap. How is that resolved? *(required — select one)*

- [ ] Compress to the cap — keep the rules, cut the long code samples, since the code itself is the better example  ← recommended
- [ ] Compress to the cap, and move the full code samples to a separate `.claude/_code-examples.md`
- [ ] Keep it long — the samples are worth more than the cap
- [ ] [TBD — needs input]

---

## Section 6 — Documentation Conventions

### Q19. Javadoc requirements *(required — select one)*

- [ ] Required on public APIs, service methods, and non-obvious business logic  ← recommended
- [ ] Required on every public member, without exception
- [ ] Only where behavior is not obvious from the signature
- [ ] Not required

### Q20. Comment language *(required — select one)*

User-facing validation messages are Persian by an existing project rule; this
question is only about code comments and Javadoc.

- [ ] English for all code comments and Javadoc; Persian only for user-facing messages  ← recommended
- [ ] Persian for both
- [ ] Author's choice
- [ ] [TBD — needs input]

### Q21. ADR language *(required — select one)*

SAW_102 §4 gives the MADR template in Persian with Persian status values.

- [ ] Persian, per the SAW_102 template exactly  ← recommended
- [ ] Persian headings per the template, English body text
- [ ] English throughout — deviation recorded in an ADR
- [ ] [TBD — needs input]

### Q22. Is the OpenAPI spec hand-written or generated? *(required — select one)*

SpringDoc is already a dependency, so annotations currently generate the spec at
runtime. SAW_102 §6 requires a committed `/documents/openapi/openapi.yaml`.

- [ ] Generate from SpringDoc annotations, then export the YAML to `/documents/openapi/` in CI  ← recommended
- [ ] Hand-write `openapi.yaml` as the contract, and validate the implementation against it (design-first)
- [ ] Both — hand-written is authoritative, generated output is diffed against it in CI
- [ ] [TBD — needs input]

---

## Section 7 — Optional

### Q23. Any coding convention, exception, or house rule not captured above? *(optional — free text)*

```
(your answer here)
```
