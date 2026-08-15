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

1. Confirm `specs/<epic-slug>/spec.md` exists
   - If not, stop and report missing spec
2. Check for `specs/<epic-slug>/tasks.md`
   - If exists: work unit = each task, in dependency order
   - If not exists: work unit = each acceptance criterion, in spec order
3. Skip any unit marked `[blocked by TQ-n]` — list in Risks
4. Derive epic-slug the same way as `write-spec`:
   - Epic ID lowercase + epic name slug → `epic-01-message-creation-service`

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

### Phase 4 — Write Implementation Report

Write to `specs/<epic-slug>/implementation-report.md`:

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

## Epic Slug Derivation

Same as `write-spec`:

1. Epic ID from PRD → lowercase → `epic-01`
2. Epic name after colon → lowercase, strip punctuation, hyphens → `message-creation-service`
3. Join: `epic-01-message-creation-service`

---

## Task Status Field

| Status | Meaning |
|--------|---------|
| `pending` | Not yet attempted |
| `in_progress` | Currently being worked on |
| `done` | Test passing, implementation complete |
| `blocked` | Failed after 3 attempts, with reason recorded |

**Only `implement-epic` writes to this field.**

---

## Hard Rules

1. **Never weaken a test** — If test can't pass, mark blocked, don't alter test
2. **Never mark done without passing test** — Must have actual pass result
3. **Never fabricate results** — Coverage/confidence from real test output
4. **Only this skill writes status** — Never let other steps touch task status
5. **Stay grounded in spec** — No implementation details outside spec/task/TQ
6. **Never auto-invoke verify-epic** — Stop and report, explicit next step
7. **Cap retries at 3** — Then mark blocked and move on

---

## Example Usage

```
/implement-epic EPIC-01

Phase 0: Checking spec at specs/epic-01-message-creation-service/spec.md ✓
         No tasks.md found, using acceptance criteria order

Phase 1: Processing AC-001 (Create Valid Message)
  1. Writing failing test... ✗ FAILS (expected)
  2. Implementing minimal code...
  3. Running test... ✓ PASSES
  4. Refactoring...
  5. Marking AC-001 done

Phase 2: Coverage = 80% (4/5 ACs complete)
Phase 3: Confidence = 75% (1 blocked high-impact unit)
Phase 4: Writing implementation-report.md ✓

EPIC-01 implemented at 75% confidence.
Run /verify-epic EPIC-01 to validate.
```
