---
name: implement-epic
description: >
  Implements one named epic from its task list (or spec, if no task list
  exists) using a strict per-unit TDD loop — write a failing test,
  implement minimal code to pass it, refactor — updating task status as
  it goes and never weakening a test to force a pass. Consults
  architecture, codestyle, security, and test advisor subagents along the
  way. Use this once a spec (and ideally a task list) exists and the user
  wants to move from planning to actual code — e.g. "implement EPIC-01,"
  "start building this epic," or "let's write the code for the messaging
  ingestion component."
---

# Implement Epic Skill

Implement one named epic using a strict, per-unit test-driven development
loop. Work on one acceptance criterion at a time: write a failing test,
implement only enough code to pass, refactor, and then continue. Never
batch all tests before all implementation.

## Pipeline Position

`brainstorm` → `write-spec` → `plan-tasks` (optional, per epic) →
`implement-epic` → `verify-epic`

Every transition is explicit. This skill never invokes `verify-epic`.

## File Layout

```
{project root}/
├── brainstorm/Epic_PRD.md                       (epic identity source)
├── specs/<epic-slug>/spec.md                    (input — always required)
├── specs/<epic-slug>/tasks.md                   (input if present — task-by-task mode)
├── .claude/_architecture-reference.md           (input, from set-standards, if present)
├── .claude/_coding-guidelines.md                (input, from set-standards, if present)
├── .claude/_documentation-standards.md          (input, from set-standards, if present)
├── specs/<epic-slug>/implementation-report.md   (output)
├── specs/<epic-slug>/advisor-notes.md           (output)
├── src/...                                      (implementation and tests)
└── .claude/skills/implement-epic/
    └── SKILL.md
```

## Input

`Epic name` is required. Resolve it using the same rule as `write-spec`
and `plan-tasks`:

1. Match it to an `EPIC-NN` entry in `brainstorm/Epic_PRD.md`'s Epic Breakdown.
2. Derive the epic slug from the ID and name, for example
   `EPIC-01 Message Creation Service` → `epic-01-message-creation-service`.
3. Require `specs/<epic-slug>/spec.md`. If it does not exist, stop and report:
   `No spec found for this epic. Run /write-spec <epic> first.`

Never infer a different epic when the supplied name is ambiguous or absent.

## Workflow — State Machine

### Phase 0 — Preflight

After resolving the epic slug, read implementation context in this exact
fixed order. Never discover these files through a directory listing or glob;
the stable order preserves prompt-cache reuse across runs.

```
1. .claude/_architecture-reference.md   (if present)
2. .claude/_coding-guidelines.md        (if present)
3. .claude/_documentation-standards.md  (if present)
4. specs/<epic-slug>/spec.md
-- CACHE BOUNDARY: everything above is stable for this run; content
   below changes as the TDD loop progresses --
5. specs/<epic-slug>/tasks.md            (if present)
```

If any of items 1–3 is missing, continue, but add a Risk to the final report
recommending that `set-standards` be run before further epics are built.

Confirm the spec exists before beginning implementation. Then select the
mode of operation:

- **Task mode:** If `tasks.md` exists, each task is a unit of work. Process
  tasks in dependency order, respecting every `Depends on` field.
- **Spec-direct mode:** If `tasks.md` does not exist, each acceptance criterion
  in the spec's Acceptance Criteria Mapping is a unit of work. Process them in
  the listed order.

For every unit, determine its mapped acceptance criterion, test strategy,
dependencies, status, impact tag, and referenced technical questions.

Skip a unit marked `[blocked by TQ-n]` only while that technical question is
unresolved. Record it under Risks instead of implementing it. A resolved TQ
removes the block even if an older prose section still contains the marker;
the current Technical Questions or Decision Log resolution is authoritative.

In task mode, also skip units already marked `done`. Do not treat a task as
`done` unless its recorded completion is backed by a passing test. If that
evidence is absent or stale, report the inconsistency and re-attempt the unit.
Do not edit the task's status merely during preflight.

### Phase 1 — TDD Loop

Process one unblocked unit at a time in dependency order. Do not begin a unit
whose dependency is blocked.

#### 1. RED — Write and Confirm a Failing Test

1. Write a test or the smallest coherent set of tests exercising exactly the
   acceptance criterion mapped to this unit. Follow the test strategy in the
   spec or task.
2. Consult `test-advisor` with only the relevant acceptance criterion, test
   strategy, and proposed test. Fold hard findings into the test. Advisor
   guidance remains non-binding; record followed and overridden guidance in
   `advisor-notes.md` as described under Advisor Wiring.
3. Run only the relevant test class quietly:

   ```bash
   mvn test -q -Dstyle.color=never -Dtest=<TestClass>
   ```

4. Confirm it fails for the expected missing behavior before changing
   implementation code. A compilation error caused solely by a not-yet-created
   production type may count as RED if that type is required by the criterion;
   unrelated compilation, environment, or infrastructure failures do not.
5. If the new test unexpectedly passes, determine whether existing behavior
   already satisfies the criterion. Verify the test is meaningful with
   `test-advisor`; do not manufacture a failure. If the behavior and test are
   valid, retain the passing evidence and proceed to refactor/status handling
   without unnecessary implementation.

Never weaken, skip, delete, disable, or comment out a test to make the unit
pass.

#### 2. GREEN — Implement the Minimum Behavior

Implement only what the current unit requires. Do not add adjacent behavior
visible later in the plan.

Before finalizing the approach, consult advisors only when their scope applies:

- Consult `architecture-advisor` if the unit introduces or changes a service
  boundary, component, persistence interaction, or data flow decision.
- Consult `security-advisor` if the unit touches cryptography, signing,
  authentication, authorization, access control, PII, secrets, or audit-log
  integrity.

Ground every implementation detail in at least one of:

- the spec;
- the current task;
- a resolved technical question;
- `.claude/_architecture-reference.md`; or
- `.claude/_coding-guidelines.md`.

If an advisor suggestion conflicts with an explicit standards rule, the
standards file wins. Record the conflict and outcome in `advisor-notes.md`.

#### 3. RUN — Make the Test Pass

Run the relevant test class quietly:

```bash
mvn test -q -Dstyle.color=never -Dtest=<TestClass>
```

If it fails, change implementation code and retry, for at most three GREEN
attempts total. The initial RED run is not one of these attempts.

On each failure, use only the relevant assertion and stack frame from console
output or a bounded view of `target/surefire-reports/`; never load full Maven
logs or large report files into context.

After three failed GREEN attempts:

1. Mark the unit `blocked`.
2. Record the actual failure, truncated to the relevant assertion and stack
   frame rather than an arbitrary full log.
3. In task mode, write `blocked` to the task's `status` field and record the
   reason in that task block.
4. Continue with the next unit that does not depend on this one.

Never spend unbounded retries on one unit.

#### 4. REFACTOR — Improve Without Changing Behavior

Once the test passes, refactor only the code touched by this unit for clarity
and duplication without changing behavior.

Consult `codestyle-advisor` on the relevant refactored code and standards.
Fold hard findings into the refactor or record why they are overridden. Then
run the same relevant test class again with the quiet Maven command. The unit
is not complete unless this post-refactor run passes.

If the post-refactor test fails, repair or revert only the refactor. This does
not grant permission to weaken the test. Apply the same three-attempt cap to
restoring a passing implementation; otherwise mark the unit blocked.

#### 5. UPDATE — Record Unit Status

After the post-refactor test passes:

- Record the test class, actual pass/fail/skipped counts, and covered AC IDs for
  report computation.
- In task mode, update this unit's `status` field to `done`.
- If blocked, set it to `blocked` and retain the real reason.

`implement-epic` is the only skill that writes task `status` fields. Preserve
all other task content and formatting. In spec-direct mode, do not add status
fields to the spec; status belongs in the implementation report.

Repeat RED → GREEN → RUN → REFACTOR → UPDATE for the next unit. Never write all
tests first and implementation later.

## Advisor Wiring

All advisor consultations happen during Phase 1 so they shape the code rather
than critique it after the epic is complete. Scope each request to the current
unit and relevant files; subagent exploration stays in the advisor's context,
and only its compact verdict returns to this skill.

| Advisor | Required scope |
|---------|----------------|
| `test-advisor` | Every unit, before finalizing the test |
| `architecture-advisor` | Service boundary, new component, persistence interaction, or data flow changes |
| `security-advisor` | Crypto, signing, auth, access control, PII, secrets, or audit-log integrity |
| `codestyle-advisor` | Every unit, after GREEN while refactoring |

Do not invoke all four advisors mechanically for every unit.

Maintain `specs/<epic-slug>/advisor-notes.md` as a concise, per-unit record of
advisor outcomes. Create it when the first advisor is consulted and update it
incrementally so a partial run does not lose findings.

```markdown
### TASK-<epic>-<NN>
- [architecture-advisor] <finding> — followed
- [test-advisor] <finding> — overridden: <one-line reason>
```

In spec-direct mode, use the AC ID as the heading:

```markdown
### AC-001
- [test-advisor] <finding> — followed
```

Log hard findings and any soft suggestion that is overridden. A compact
`— followed` entry may be used for accepted guidance needed by the later
summary. Do not duplicate advisor exploration or long rationale. Where an
advisor conflicts with `.claude/_*.md`, cite the standards file and record
that the standards rule prevailed.

Advisor guidance is non-binding; explicit project standards are binding.

### Phase 2 — Coverage Computation

After every eligible unit has been attempted, compute:

```
coverage = (
  acceptance criteria with a passing test AND completed implementation
  / total acceptance criteria in this epic
) × 100
```

Use distinct AC IDs for numerator and denominator so multiple tasks mapped to
one criterion do not double-count it. A criterion is covered only when all
implementation work required for it is complete and its test evidence passes.
Blocked, unattempted, manual-only, and passing-test-with-incomplete-code
criteria are not covered.

Compute coverage from actual test runs. Never assert or estimate it.

### Phase 3 — Confidence Computation

Compute:

```
confidence = coverage − (5 × high-impact blocked units)
```

Inherit `high-impact` from the task's tag or the corresponding Technical
Question in the spec. Do not invent impact tags. If the spec provides no
high-impact tag, the unit does not receive this penalty, but remains visible
as blocked and lowers coverage when its AC is uncovered.

Clamp the final confidence to the range 0–100 and state the arithmetic in the
report.

### Phase 4 — Write the Implementation Report

Write `specs/<epic-slug>/implementation-report.md` with:

1. **Implemented Features** — every acceptance criterion ID, description, and
   implemented/blocked status.
2. **Acceptance Criteria Coverage** — computed score and explicit covered vs.
   blocked/uncovered AC IDs.
3. **Test Results** — actual test classes and pass/fail/skipped counts from the
   runs performed. Use Maven/Surefire result summaries, not estimates. Avoid
   double-counting repeated RED/GREEN/refactor runs in the final-state totals;
   separately note blocked attempts where relevant.
4. **Risks** — every blocked unit, every unresolved Technical Question, units
   skipped because of dependency blocks, and the missing-standards warning if
   applicable.
5. **Advisor Findings Summary** — compact counts of findings followed and
   overridden by advisor, with a link to `advisor-notes.md`; do not duplicate
   detailed findings.
6. **Confidence** — computed score and a one-line reason for any shortfall.

Use this structure:

```markdown
# Implementation Report: EPIC-NN <Epic Name>

**Epic**: <epic-slug>
**Coverage**: <N>%
**Confidence**: <N>%

## Implemented Features

| AC | Description | Status | Evidence |
|----|-------------|--------|----------|
| AC-001 | <criterion> | Implemented | `<TestClass.method>` passed |

## Acceptance Criteria Coverage

- Covered: <AC IDs>
- Blocked/uncovered: <AC IDs>
- Calculation: <covered>/<total> × 100 = <N>%

## Test Results

| Test Class | Passed | Failed | Skipped |
|------------|--------|--------|---------|
| `<TestClass>` | <N> | <N> | <N> |

## Risks

- <blocked units and open technical questions, or "None">

## Advisor Findings Summary

- Followed: <N>
- Overridden: <N>
- Details: [advisor-notes.md](advisor-notes.md)

## Confidence

<N>% — <one-line reason and arithmetic>
```

If no advisor guidance was overridden, say so. If no risks remain, write
`None`; never omit the section.

### Phase 5 — Stop

Report to the user:

`EPIC-NN <name> implemented at <confidence>% confidence. Implementation report: specs/<epic-slug>/implementation-report.md`

Mention blocked units concisely when present. Do not invoke `verify-epic`; it
is a separate explicit next step.

## Exit Criteria

The run is complete when:

1. Every eligible unit has been attempted in dependency order.
2. Each unit is implemented with passing test evidence or explicitly blocked
   with a real reason.
3. Task statuses have been updated in task mode.
4. Coverage and confidence have been computed from actual results.
5. Advisor outcomes have been recorded.
6. `specs/<epic-slug>/implementation-report.md` has been written.

Completion is not gated on a confidence threshold. Confidence is reported,
not used to hide unfinished work.

## Hard Rules

1. Never weaken, skip, delete, disable, or comment out a test to make it pass.
   If implementation cannot pass, block the unit and report the real failure.
2. Never mark a task or criterion done without an actually passing test.
3. Never fabricate test results, coverage, or confidence numbers; compute them
   from real test-run output.
4. Only `implement-epic` writes task `status` fields, and only during Phase 1's
   UPDATE step.
5. Never introduce an implementation detail not grounded in the spec, task,
   a resolved technical question, or applicable `.claude/_*.md` rule.
6. Never auto-invoke `verify-epic`.
7. Cap implementation retries at three per unit, then block and continue with
   independent work.
8. Always run Maven tests with `-q -Dstyle.color=never`; never use bare
   `mvn test`. Return only concise failure evidence to context.
9. Always read Phase 0 context in the fixed order, with the cache boundary
   between stable standards/spec inputs and evolving task content.
10. Scope advisor consultations per the Phase 1 mapping; never invoke all four
    advisors for every unit by default.
11. Never batch all test creation before implementation. Complete the full
    RED → GREEN → RUN → REFACTOR → UPDATE loop for one unit before the next.
12. Never edit the spec or acceptance criterion to fit the implementation.
