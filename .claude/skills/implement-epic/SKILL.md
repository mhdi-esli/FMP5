---
name: implement-epic
description: >
  Implements one named epic from its task list (or spec, if no task list
  exists) using a strict per-unit TDD loop — write a failing test,
  implement minimal code to pass it, refactor — updating task status as
  it goes and never weakening a test to force a pass. Use this once a
  spec (and ideally a task list) exists and the user wants to move from
  planning to actual code — e.g. "implement EPIC-01," "start building
  this epic," or "let's write the code for the messaging ingestion
  component."
---

# Implement Epic Skill

Implements one named epic using a strict per-unit TDD loop against its task list or spec.

## File Layout

```
{project root}/
├── specs/<epic-slug>/spec.md                    (input — always required)
├── specs/<epic-slug>/tasks.md                   (input if present — task-by-task mode)
├── specs/<epic-slug>/implementation-report.md   (output)
├── specs/<epic-slug>/advisor-notes.md           (output — advisor findings/overrides)
├── src/...                                      (actual code and tests)
└── .claude/skills/implement-epic/
    └── SKILL.md
```

## Workflow

### Phase 0 — Preflight

**Read the shared standards first**, in this fixed order — never directory-listing order, so the prompt prefix stays identical across runs and prompt caching engages:

1. `.claude/_architecture-reference.md`
2. `.claude/_coding-guidelines.md`
3. `.claude/_documentation-standards.md`

Follow their conventions if they exist. If any are missing, proceed but add a note under Risks recommending `set-standards` be run before further epics are built. Then continue with the checks below.

1. Derive `<epic-slug>` the same way as `write-spec` (epic ID lowercased + epic-name slug → `epic-01-message-creation-service`), then confirm `specs/<epic-slug>/spec.md` exists. If not, stop and report the missing spec.

The three standards and the spec are the **stable prompt prefix** — read them in the fixed order above so caching engages.

**--- CACHE BOUNDARY — read the per-run-changing inputs below only after the stable prefix above ---**

2. Check for `specs/<epic-slug>/tasks.md` (its task `Status` lines change every run):
   - If it exists: work unit = each task, in dependency order; resume from each task's current `Status`
   - If not: work unit = each acceptance criterion, in spec order
3. Skip any unit marked `[blocked by TQ-n]`, or a task already `Status: blocked` — list it in Risks

### Phase 1 — TDD Loop (One Unit at a Time)

For each unit that isn't blocked or already `done`:

```
┌─────────────────────────────────────────────────────────┐
│                    TDD CYCLE                             │
│                                                         │
│  1. RED: Write failing test for this unit               │
│     - Follow Test Strategy from spec/task               │
│     - Run test, confirm it FAILS for expected reason    │
│                                                         │
│  2. GREEN: Implement minimal code to pass               │
│     - Only what this unit requires                      │
│     - Nothing beyond current scope                      │
│                                                         │
│  3. RUN: Execute test                                   │
│     - If FAILS: fix implementation, retry (max 3 times) │
│     - If still FAILS after 3: mark unit `blocked`       │
│     - If PASSES: continue to refactor                   │
│                                                         │
│  4. REFACTOR: Clean up without changing behavior        │
│     - Re-run test to confirm still passes               │
│                                                         │
│  5. UPDATE: Mark task status to `done` or `blocked`     │
│     - This skill is the ONLY one that writes status     │
│                                                         │
└─────────────────────────────────────────────────────────┘
         │
         ▼
    Next unit (respecting dependencies)
```

**Retry Limit:** Maximum 3 attempts per failing unit. After 3 failures:
- Mark unit `blocked`
- Record actual failure output
- Move to next non-dependent unit

**Running tests:** always the quiet form — `mvn test -q -Dstyle.color=never -Dtest=<TestClass>` for a single unit — so only the pass/fail summary, not the full Spring/Testcontainers startup log, enters context (Hard Rule 8).

**Advisors, during the loop (not after):** at RED consult **test-advisor** before finalizing the test; at GREEN consult **architecture-advisor** when the change touches a service boundary, a new component, or data flow, and **security-advisor** when it touches signing, crypto, access control, PII, or the audit trail; at REFACTOR consult **codestyle-advisor**. They are scoped per unit and non-binding — see **Advisor Wiring** below for the scoping table and how overrides are logged.

### Phase 2 — Coverage Computation

```
coverage = (acceptance criteria with passing test AND completed implementation
            / total acceptance criteria in this epic) × 100
```

Computed from actual test-run results, never asserted.

### Phase 3 — Confidence Computation

```
confidence = coverage − 5 points per blocked unit (high-impact)
```

Inherit high-impact tag from:
- Spec's Technical Questions
- Task's own tag

### Phase 4 — Write Outputs

Two outputs live under the epic's spec folder: `advisor-notes.md` (written incrementally during the loop — see Advisor Wiring) and the implementation report. Write the report to `specs/<epic-slug>/implementation-report.md`:

```markdown
# Implementation Report: EPIC-NN Epic Name

## Coverage: XX%
## Confidence: XX%

### Implemented Features
| AC ID | Description | Status |
|-------|-------------|--------|
| AC-001 | Create Valid Message | ✅ Done |
| AC-002 | Validation Failure | ✅ Done |
| AC-003 | Missing Required Field | ⬜ Blocked |

### Test Results
| Test Suite | Passed | Failed | Skipped |
|------------|--------|--------|---------|
| Unit Tests | 15 | 0 | 2 |
| Integration Tests | 8 | 0 | 1 |

### Risks
- [List blocked units with reasons]
- [Open Technical Questions inherited from spec]

### Summary
[One-line reason for confidence level]
```

### Phase 5 — Stop

Report completion. Do NOT auto-invoke `verify-epic`.

---

## Advisor Wiring

Advisors are consulted **during** the TDD loop, never batched afterward, and each is **scoped to the change in the current unit** — codestyle-advisor applies to nearly every unit at REFACTOR; the others are conditional:

| Advisor | Consult when… | TDD step |
|---------|---------------|----------|
| test-advisor | before finalizing a failing test — does it actually cover the AC/task? | RED |
| architecture-advisor | the change touches a service boundary, a new component, or data flow | GREEN |
| security-advisor | the change touches signing, crypto, access control, PII, or the audit trail | GREEN |
| codestyle-advisor | cleaning up any non-trivial unit | REFACTOR |

Advisors are **advisory, not binding**. When an advisor's recommendation conflicts with one of the three `.claude/_*.md` standards files, the **standards file wins**, and the override is logged. Record every advisor finding that materially shaped (or was overridden in) a unit to `specs/<epic-slug>/advisor-notes.md`:

```
### TASK-epic-01-01
- [architecture-advisor] <finding> — overridden: <reason>
- [test-advisor] <finding> — followed
- [codestyle-advisor] <finding> — followed
```

Skip the log for units where no advisor raised anything.

---

## Epic Slug Derivation

Same as `write-spec`:

1. Epic ID from PRD → lowercase → `epic-01`
2. Epic name after colon → lowercase, strip punctuation, hyphens → `message-creation-service`
3. Join: `epic-01-message-creation-service`

---

## Task Status Field

| Status | Meaning | Set by |
|--------|---------|--------|
| `not started` | Not yet attempted (plan-tasks' initial value) | plan-tasks |
| `in progress` | Currently being worked on | implement-epic |
| `done` | Test passing, implementation complete | implement-epic |
| `blocked` | Failed after 3 attempts, or `[blocked by TQ-n]`, with reason recorded | plan-tasks / implement-epic |

**Only `implement-epic` advances this field** once implementation starts; `plan-tasks` sets the initial `not started`/`blocked` when it creates the task. The vocabulary matches `plan-tasks` exactly.

---

## Hard Rules

1. **Never weaken, skip, delete, or comment out a test** to make a unit pass — if it can't pass, mark the unit `blocked` and leave the test intact
2. **Never mark `done` without a real passing test** — an actual green run, never asserted
3. **Never fabricate results** — coverage and confidence come from real test output only
4. **Only `implement-epic` advances task status** — never let another step move it; `plan-tasks` only sets the initial `not started`/`blocked`
5. **Stay grounded in the spec** — implement nothing outside the spec, a task, or a resolved TQ
6. **Never auto-invoke `verify-epic`** — stop and report; running it is an explicit, separate step
7. **Cap retries at 3** per failing unit — then mark `blocked` and move on
8. **Always use the quiet test form** (`-q -Dstyle.color=never`) and pass only the pass/fail summary — never the full startup log — into context
9. **Advisors are scoped per unit** — consult only the advisor(s) relevant to the change, never all four on every unit
10. **Advisors are non-binding** — when one conflicts with a standards file the **standards file wins**, and the followed/overridden finding is logged to `advisor-notes.md`

---

## Example Usage

```
/implement-epic EPIC-01

Phase 0: Checking spec at specs/epic-01-message-creation-service/spec.md ✓
         No tasks.md found, using acceptance criteria order

Phase 1: Processing AC-001 (Create Valid Message)
  1. Writing failing test... ✗ FAILS (expected)   [test-advisor: covers the AC ✓]
  2. Implementing minimal code...
  3. Running mvn test -q -Dstyle.color=never -Dtest=MessageControllerTest ... ✓ PASSES
  4. Refactoring...   [codestyle-advisor: naming ✓]
  5. Marking AC-001 done

Phase 2: Coverage = 80% (4/5 ACs complete)
Phase 3: Confidence = 75% (1 blocked high-impact unit)
Phase 4: Writing advisor-notes.md + implementation-report.md ✓

EPIC-01 implemented at 75% confidence.
Run /verify-epic EPIC-01 to validate.
```
