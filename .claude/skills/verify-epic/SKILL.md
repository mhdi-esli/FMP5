---
name: verify-epic
description: >
  Independently verifies a completed epic implementation against its spec —
  re-derives acceptance-criterion status from actual test execution and code
  inspection rather than trusting the self-reported implementation report,
  flags any test that looks weakened or doesn't assert what the spec called
  for, and reports PASS/PARTIAL/FAIL per criterion with a computed
  confidence level. Use this after implement-epic has produced an
  implementation report and the user wants to confirm the epic is actually
  done — e.g. "verify EPIC-01," "check if this epic is really finished," or
  "audit the implementation against the spec."
---

# Verify Epic Skill

Independently verifies a completed epic implementation against its spec.

## File Layout

```
{project root}/
├── specs/<epic-slug>/spec.md                    (input)
├── specs/<epic-slug>/tasks.md                   (input, if present)
├── specs/<epic-slug>/implementation-report.md   (input — self-reported, NOT trusted)
├── specs/<epic-slug>/verification-report.md     (output)
├── src/...                                      (actual code and tests)
└── .claude/skills/verify-epic/
    └── SKILL.md
```

## Workflow

### Phase 0 — Preflight

1. Confirm `specs/<epic-slug>/spec.md` exists
2. Confirm `specs/<epic-slug>/implementation-report.md` exists
   - If missing: STOP — suggest running `implement-epic` first
3. Derive epic-slug (same as other skills)

### Phase 1 — Independent Re-derivation (The Core Purpose)

**Do NOT trust the implementation report.** Recheck from source.

For each Acceptance Criterion in `spec.md`:

```
┌─────────────────────────────────────────────────────────────────┐
│              INDEPENDENT VERIFICATION PER AC                     │
│                                                                  │
│  1. LOCATE TEST(S)                                               │
│     - From tasks.md or spec's Test strategy                      │
│     - Actually find the test file(s)                             │
│                                                                  │
│  2. EXECUTE TEST(S)                                              │
│     - Run them for real                                          │
│     - Record actual pass/fail                                    │
│     - NEVER copy from implementation-report.md                   │
│                                                                  │
│  3. INSPECT TEST QUALITY                                         │
│     - Does it assert what spec's Test Strategy described?        │
│     - Is it weakened/trivial? (e.g., only checks no exception)   │
│     - Does it cover the edge cases spec called out?              │
│                                                                  │
│  4. INSPECT IMPLEMENTATION                                       │
│     - Does it match spec's Design/Implementation approach?       │
│     - Is there code at the right location?                       │
│     - Does it do what spec described?                            │
│                                                                  │
│  5. ASSIGN STATUS                                                │
│     - PASS: test exists, asserts correctly, passes, impl matches │
│     - PARTIAL: some but not all conditions hold                  │
│     - FAIL: missing entirely, or contradicts spec                │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

#### Status Definitions

| Status | Criteria |
|--------|----------|
| **PASS** | Test exists, asserts the right thing, passes, implementation matches spec |
| **PARTIAL** | Some but not all conditions hold (test passes but misses edge case, impl exists without test) |
| **FAIL** | Missing entirely, or contradicts the spec |

#### Weakened Test Detection

Flag as **weakened** if:
- Test only checks "does not throw" when spec required specific return value
- Test uses `assertTrue(true)` or equivalent no-op
- Test asserts on wrong field/value
- Test ignores edge cases the spec explicitly called out
- Test is commented out or skipped without justification

### Phase 2 — Run Test Suites

Execute for real:
- Unit tests
- Integration tests
- End-to-end tests (where applicable)

Record actual counts:
```
| Suite | Passed | Failed | Skipped |
|-------|--------|--------|---------|
| Unit  | 15     | 0      | 2       |
| Integration | 8 | 0    | 1       |
| E2E   | 4      | 0      | 0       |
```

**NEVER copy counts from implementation-report.md.**

### Phase 3 — Cross-check Implementation Report

Compare `implementation-report.md` claims vs independent findings:

| Claimed | Found | Discrepancy? |
|---------|-------|--------------|
| AC-001: done | AC-001: PASS | No |
| AC-002: done | AC-002: PARTIAL | **Yes** — weakened test |
| AC-003: done | AC-003: FAIL | **Yes** — missing implementation |

**Discrepancy = serious finding** — self-report was wrong, not just incomplete.

### Phase 4 — Compute Confidence Level

```
confidence = (ACs with PASS / total ACs) × 100
             − 2 points per PARTIAL
             − 10 points per discrepancy vs implementation-report.md
```

Example:
- 5 ACs total, 3 PASS, 1 PARTIAL, 1 FAIL
- 2 discrepancies found
- confidence = (3/5) × 100 − 2 − 20 = 60 − 2 − 20 = **38%**

### Phase 5 — Write Verification Report

Write to `specs/<epic-slug>/verification-report.md`:

```markdown
# Verification Report: EPIC-NN Epic Name

## Confidence Level: XX%
## Verification Status: [VERIFIED / ISSUES FOUND / FAILED]

---

## Acceptance Criterion Status

| AC ID | Description | Status | Reason |
|-------|-------------|--------|--------|
| AC-001 | Create Valid Message | ✅ PASS | Test asserts correctly, impl matches |
| AC-002 | Validation Failure | ⚠️ PARTIAL | Test passes but misses zero-boundary case |
| AC-003 | Missing Required Field | ✅ PASS | All conditions met |
| AC-004 | Unsupported Network | ❌ FAIL | Test exists but implementation missing |
| AC-007 | Unauthorized Access | ✅ PASS | OAuth integration verified |

---

## Missing Tests

- [List any ACs without corresponding tests]

---

## Missing Functionality

- [List any ACs where implementation doesn't match spec]

---

## Test Results (Actual Execution)

| Suite | Passed | Failed | Skipped | Total |
|-------|--------|--------|---------|-------|
| Unit | 15 | 0 | 2 | 17 |
| Integration | 8 | 0 | 1 | 9 |
| E2E | 4 | 0 | 0 | 4 |
| **Total** | **27** | **0** | **3** | **30** |

---

## Discrepancies vs Implementation Report

| AC ID | Reported | Found | Severity |
|-------|----------|-------|----------|
| AC-002 | done | PARTIAL | Medium — test weakened |
| AC-004 | done | FAIL | High — implementation missing |

---

## Risks

- [List blocked items, open issues, technical debt]

---

## Recommendations

1. [Specific fix for each PARTIAL]
2. [Specific fix for each FAIL]
3. [Any refactoring suggestions]

---

## Summary

[One-line reason for confidence level]

---

**Verification Date:** YYYY-MM-DD
```

### Phase 6 — Stop and Report

Report completion. Do NOT automatically:
- Fix issues found
- Move to next epic
- Deploy or merge

---

## Hard Rules

1. **Never trust self-report** — Independently re-derive from tests and code
2. **Never modify anything** — This skill only observes and reports
3. **Never fabricate results** — Compute from actual execution
4. **Flag weakened tests** — Never silently accept trivial assertions
5. **PASS requires both** — Actually-passing test AND matching implementation
6. **Discrepancy is serious** — Wrong report is worse than incomplete

---

## Example Usage

```
/verify-epic EPIC-01

Phase 0: Found spec.md ✓
         Found implementation-report.md ✓

Phase 1: Verifying 5 acceptance criteria...

  AC-001: Test found at MessageCreationServiceTest.java:45
          Executing test... ✓ PASS
          Test asserts messageId is not null, status is DRAFT
          Implementation matches spec design
          Status: ✅ PASS

  AC-002: Test found at MessageValidationServiceTest.java:78
          Executing test... ✓ PASS
          WARNING: Test only checks exception thrown, not Persian message
          Status: ⚠️ PARTIAL

  AC-004: Test found at MessageControllerTest.java:102
          Executing test... ✓ PASS
          ERROR: No implementation at MessageController.java
          Status: ❌ FAIL

Phase 2: Running all test suites...
         Unit: 15 passed, 0 failed
         Integration: 8 passed, 0 failed

Phase 3: Cross-checking implementation-report.md...
         Found 2 discrepancies

Phase 4: Computing confidence...
         (3/5 PASS) × 100 − 2 (1 PARTIAL) − 20 (2 discrepancies) = 38%

Phase 5: Writing verification-report.md ✓

VERIFICATION COMPLETE: EPIC-01 at 38% confidence
Status: ISSUES FOUND
Run /implement-epic EPIC-01 to address issues.
```
