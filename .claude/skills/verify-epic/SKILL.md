---
name: verify-epic
description: >
  Final quality gate for an epic after implement-epic completes. Confirms
  implementation satisfies spec, standards, and acceptance criteria.
  Produces PASS/NEEDS_REVIEW verdict that drives Jira status transitions.
---

# Verify Epic Skill

Final quality gate confirming implementation satisfies spec, standards, and acceptance criteria.

## PATCH NOTE (reconciliation with actual plan-tasks/set-standards output)
This version fixes four path mismatches from the original draft:
1. Task input is `specs/<epic-slug>/tasks.md` (Markdown `### TASK-...` blocks),
   NOT `tasks/<epic-slug>-tasks.json` — plan-tasks never produces JSON.
2. Generated standards inputs are read in the fixed pipeline order:
   `.claude/_architecture-reference.md`, `.claude/_coding-guidelines.md`, then
   `.claude/_documentation-standards.md`. The authoritative source documents
   follow afterward, also in fixed order, only when a summarized rule needs
   source-level verification: `standards/reference/SWA_101-comm-standards.md`,
   then `standards/reference/SAW_102-arch-doc-standards.md`.
3. Output is `specs/<epic-slug>/verification-report.md`, NOT
   `/verification/<epic-slug>-verify-report.md`.
4. `decisions.md` / `decisions_archive.md` are per-epic, at
   `specs/<epic-slug>/decisions.md` and `specs/<epic-slug>/decisions_archive.md`,
   NOT project-root files.

## Purpose

This skill is the **terminal verification step** that:
1. Validates each task's acceptance criteria against actual implementation
2. Cross-checks implementation against the standards files (distinct from functional checks)
3. Produces a PASS / PASS_WITH_NOTES / NEEDS_REVIEW verdict
4. Transitions Jira tickets or posts failure comments (gated by verdict)
5. Archives decision log when thresholds are met

## Inputs

```
{project root}/
├── .claude/_architecture-reference.md            (from set-standards, if present)
├── .claude/_coding-guidelines.md                 (from set-standards, if present)
├── .claude/_documentation-standards.md           (from set-standards, if present)
├── specs/<epic-slug>/spec.md                     (required — from write-spec)
├── specs/<epic-slug>/tasks.md                    (required — from plan-tasks)
├── standards/reference/SWA_101-comm-standards.md (source check, if needed/present)
├── standards/reference/SAW_102-arch-doc-standards.md (source check, if needed/present)
├── specs/<epic-slug>/implementation-report.md    (from implement-epic)
├── specs/<epic-slug>/decisions.md                (optional — epic-scoped)
└── .claude/skills/verify-epic/SKILL.md
```

Read the three generated standards first in the exact order shown, followed by
the spec and tasks. This is the shared stable prefix used throughout the SDD
pipeline. Never rely on directory-listing order. Read authoritative source docs
only when needed to verify a summarized or cited rule, and then always SWA_101
before SAW_102; they do not replace the three generated standards inputs.

## Output

```
{project root}/
└── specs/<epic-slug>/
    ├── verification-report.md
    └── decisions_archive.md   (created/updated if archival triggered)
```

## Workflow

### Phase 0 — Preflight

1. Resolve the epic and existing slug using the same rule as the other skills.
2. Confirm `specs/<epic-slug>/spec.md` exists.
   - If not, stop and report: "No spec found for this epic."
3. Confirm `specs/<epic-slug>/tasks.md` exists.
   - If not, stop and report: "No task list found. Run /plan-tasks first."
4. Read context in this exact fixed order:

   ```
   1. .claude/_architecture-reference.md   (if present)
   2. .claude/_coding-guidelines.md        (if present)
   3. .claude/_documentation-standards.md  (if present)
   4. specs/<epic-slug>/spec.md
   5. specs/<epic-slug>/tasks.md
   -- CACHE BOUNDARY: stable standards/spec/task inputs above; optional reports,
      source checks, and evolving verification state below --
   6. specs/<epic-slug>/implementation-report.md (if present)
   7. specs/<epic-slug>/decisions.md (if present)
   ```

   Never use a glob or listing to choose this order.
5. If any of generated standards 1–3 is missing, continue and add a Risk to the
   verification report recommending `set-standards` before further epics are
   built. Skip only checks that depend on the missing file; do not skip all
   standards compliance when one file is absent.
6. When a generated standard cites a source rule whose exact wording is needed,
   read `standards/reference/SWA_101-comm-standards.md` and then
   `standards/reference/SAW_102-arch-doc-standards.md`, each if present and
   always in that order. A missing source is a traceability Risk, not permission
   to invent or ignore the generated rule.
7. Use the implementation report as context when present; verification can
   continue without it.

### Phase 1 — Task Acceptance Criteria Verification

Parse each `### TASK-<epic>-<NN>` block from `specs/<epic-slug>/tasks.md`.
Each task's acceptance criterion comes from its `Maps to: AC-NN` field,
cross-referenced against the Acceptance Criteria Mapping in
`specs/<epic-slug>/spec.md` for the actual criterion text.

#### Verification Strategy

```
For each task's mapped acceptance criterion:
  1. Parse criterion type:
     - Functional: "returns 201", "validates X", "creates Y"
     - Non-functional: "response time < 200ms", "logs contain Z"
     - Manual QA: "UI displays correctly", "user can navigate"

  2. Determine verification method:
     - Automated test exists → run test, capture pass/fail
     - No test but testable → flag as UNVERIFIED (requires test)
     - Manual QA required → flag as MANUAL_QA_NEEDED

  3. Record result:
     - PASS: Test passed
     - FAIL: Test failed (include failure output)
     - UNVERIFIED: No automated test, criterion is testable
     - MANUAL_QA_NEEDED: Requires human verification
```

#### Verification Methods by Criterion Type

| Criterion Type | Verification Method |
|----------------|---------------------|
| HTTP response code | Run integration test or curl against running app |
| Validation error returned | Run unit/integration test |
| Entity created in DB | Run integration test with Testcontainers, query DB |
| Business rule enforced | Run unit/integration test with boundary cases |
| Performance SLA | Run performance test (flag if no test exists) |
| UI/UX behavior | Flag as MANUAL_QA_NEEDED |
| Security requirement | Run security test or static analysis |

#### Test Execution

1. Identify relevant test files from the task's title/description in `tasks.md`
2. Run tests quietly: `mvn test -q -Dstyle.color=never -Dtest=<TestClass>`
   (project's Surefire config redirects full output to
   `target/surefire-reports/`; only the summary + failures re-enter context)
3. Capture:
   - Test class name
   - Pass/fail count
   - Failure output (truncated to first 500 chars per failure)
4. If no tests found for a testable criterion:
   - Mark as `UNVERIFIED`
   - Record: "No automated test found for criterion: {criterion text}"

#### Task Verification Record

For each task, produce:

```json
{
  "taskId": "TASK-epic-01-01",
  "title": "Add message creation endpoint with validation",
  "criteriaResults": [
    {
      "criterion": "Valid message request returns 201 with message ID",
      "status": "PASS",
      "evidence": "MessageControllerIntegrationTest.createMessage_validRequest_returns201 passed"
    },
    {
      "criterion": "Missing required fields returns 400 with MSG-001 error",
      "status": "PASS",
      "evidence": "MessageControllerTest.createMessage_missingFields_returns400 passed"
    },
    {
      "criterion": "Invalid sender institution returns 400 with MSG-010 error",
      "status": "FAIL",
      "evidence": "MessageControllerIntegrationTest.createMessage_invalidSender failed: Expected status 400, got 500. Error: NullPointerException in InstitutionService"
    },
    {
      "criterion": "Message created with DRAFT status when valid",
      "status": "UNVERIFIED",
      "evidence": "No test found asserting message status field"
    }
  ],
  "summary": {
    "total": 4,
    "passed": 2,
    "failed": 1,
    "unverified": 1,
    "manualQaNeeded": 0
  }
}
```

This JSON is an internal working structure the skill builds while
verifying — it is never written to disk as a separate file. It only
feeds Phase 3's confidence computation and Phase 5's report.

### Phase 2 — Standards Compliance Verification

Distinct from functional acceptance criteria — check implementation
against the standards files loaded in Phase 0, respecting the routing
`set-standards` already established (SWA_101 security/tracing/protocol
items live in `_architecture-reference.md`; URL/envelope/error-code/data-
formatting items live in `_coding-guidelines.md`; SAW_102 items live in
`_documentation-standards.md`).

#### Standards Check Process

For each relevant rule in the loaded standards files:

1. Identify which file/section it came from, e.g.
   `_coding-guidelines.md — URL Structure`
2. Extract the rule
3. Check implementation for compliance

#### Standards Check Categories

| Category | Check Type | Verification Method |
|----------|------------|---------------------|
| URL structure | Pattern match | Parse controller annotations, validate versioned kebab-case path |
| HTTP status codes | Range check | Verify controller returns correct status per operation type |
| Error code format | Pattern match | Verify error codes match `{ISSUER}-{NUMBER}` format, custom codes ≥201 |
| Envelope format | Schema validation | Verify response DTO has `resultData`/`errorList`/`message` structure |
| Headers | Presence check | Verify `Idempotency-Key`, `X-Request-DateTime`, `traceparent` in controller signature or filter |
| Security | Annotation check | Verify OAuth2/JWS handling and required-permission declarations on protected endpoints |
| Layering | Code structure | Verify controller doesn't call repository directly |
| Naming | Pattern match | Verify class/method names follow conventions |
| Logging | Content check | Verify no sensitive fields (`pan`, `cvv2`, `password`, `token`, `nationalCode`) in logs |

#### Standards Violation Record

```json
{
  "standardRef": ".claude/_coding-guidelines.md — Error Code Format",
  "rule": "Error codes must use {ISSUER}-{NUMBER} format, custom codes start from 201",
  "status": "FAIL",
  "violation": "Found error code 'MESSAGE_NOT_FOUND' in InstitutionService.java:42 — expected format 'MSG-XXX'",
  "severity": "MEDIUM"
}
```

**Severity Levels:**
- CRITICAL: Security violation, data integrity risk
- HIGH: Breaks contract (API incompatibility, wrong status code)
- MEDIUM: Standards deviation (wrong error format, naming)
- LOW: Cosmetic (documentation, comment style)

#### Standards Compliance Summary

```json
{
  "standardsChecked": [
    ".claude/_coding-guidelines.md",
    ".claude/_architecture-reference.md",
    ".claude/_documentation-standards.md"
  ],
  "violations": [
    {
      "standardRef": ".claude/_coding-guidelines.md — Error Code Format",
      "severity": "MEDIUM",
      "description": "Error code format violation in InstitutionService.java"
    }
  ],
  "summary": {
    "total": 15,
    "passed": 14,
    "failed": 1,
    "bySeverity": { "CRITICAL": 0, "HIGH": 0, "MEDIUM": 1, "LOW": 0 }
  }
}
```

### Phase 3 — Confidence Computation

Compute epic-level confidence score from task results and standards compliance.

#### Confidence Formula

```
confidence = (
  (task_criteria_passed / task_criteria_total) * 60  // Functional: 60% weight
  +
  (standards_passed / standards_total) * 30            // Standards: 30% weight
  +
  (tasks_with_passing_tests / tasks_total) * 10       // Test coverage: 10% weight
)
-
(5 * unverified_criteria_count)                       // Penalty for unverified
-
(10 * manual_qa_needed_count)                         // Penalty for manual QA
```

#### Verdict Threshold

```
if confidence >= 85 AND critical_failures == 0 AND high_failures == 0:
  verdict = PASS
elif confidence >= 70 AND critical_failures == 0:
  verdict = PASS_WITH_NOTES
else:
  verdict = NEEDS_REVIEW
```

**Threshold Rationale:**
- 85% for clean PASS ensures most criteria verified and standards met
- CRITICAL/HIGH failures block PASS regardless of score (security/contract issues)
- PASS_WITH_NOTES allows near-complete epics to proceed with documented gaps
- NEEDS_REVIEW requires human intervention before proceeding

### Phase 4 — Decision Log Archival

Before writing the verification report, check `specs/<epic-slug>/decisions.md`:

#### Archival Rules

```
archive_trigger = (
  decision_count > 30
  OR
  (verdict == "PASS" AND epic_reached_pass)
)

if archive_trigger:
  1. Read all entries from specs/<epic-slug>/decisions.md
  2. Filter: resolved OR superseded (NOT open)
  3. For each filtered entry:
     - Format as single line: date | decision | outcome | ref
     - Append to specs/<epic-slug>/decisions_archive.md
  4. Remove filtered entries from specs/<epic-slug>/decisions.md
  5. Keep all open entries in specs/<epic-slug>/decisions.md
```

#### Archive Entry Format

```markdown
| Date | Decision | Outcome | Ref |
|------|----------|---------|-----|
| 2026-08-09 | Use Caffeine cache for message definitions | Implemented | ADR-005 |
| 2026-08-07 | REST over gRPC for external API | Implemented | ADR-003 |
| 2026-08-05 | PostgreSQL over MongoDB for ACID compliance | Implemented | ADR-001 |
```

**Note:** `decisions_archive.md` is write-only from the agent's perspective.
It's never loaded as context. The archival keeps `decisions.md` small and
cheap to load on every skill run.

### Phase 5 — Write Verification Report

Write structured Markdown report to `specs/<epic-slug>/verification-report.md`.

#### Report Structure

```markdown
# Verification Report: EPIC-01 Message Creation Service

**Epic**: epic-01-message-creation-service
**Spec**: specs/epic-01-message-creation-service/spec.md
**Tasks**: specs/epic-01-message-creation-service/tasks.md
**Generated**: 2026-08-09T20:01:58.456Z

---

## Verdict: PASS_WITH_NOTES

**Confidence**: 82/100

**Reason**: Functional criteria mostly met, standards compliant, but 2 criteria require manual verification.

**Blocking Issues**: None

---

## Task Results

### TASK-epic-01-01: Add message creation endpoint with validation

**Status**: PARTIAL_PASS

| Criterion | Status | Evidence |
|-----------|--------|----------|
| Valid message request returns 201 with message ID | ✅ PASS | MessageControllerIntegrationTest.createMessage_validRequest_returns201 passed |
| Missing required fields returns 400 with MSG-001 error | ✅ PASS | MessageControllerTest.createMessage_missingFields_returns400 passed |
| Invalid sender institution returns 400 with MSG-010 error | ❌ FAIL | Expected 400, got 500. NullPointerException in InstitutionService |
| Message created with DRAFT status when valid | ⚠ UNVERIFIED | No test found asserting message status field |

**Summary**: 2/4 passed, 1 failed, 1 unverified

---

## Standards Compliance

**Standards Checked**: .claude/_coding-guidelines.md, .claude/_architecture-reference.md, .claude/_documentation-standards.md

| Standard | Status | Details |
|----------|--------|---------|
| _coding-guidelines.md — URL Structure | ✅ PASS | All endpoints follow versioned kebab-case pattern |
| _coding-guidelines.md — HTTP Status Codes | ✅ PASS | Correct status codes used per operation |
| _coding-guidelines.md — Error Code Format | ❌ FAIL | **MEDIUM**: Found 'MESSAGE_NOT_FOUND' in InstitutionService.java:42, expected 'MSG-XXX' |
| _architecture-reference.md — Security | ✅ PASS | All endpoints protected, OAuth2/JWS enforced |
| _coding-guidelines.md — Layering | ✅ PASS | No direct repository calls from controllers |

**Summary**: 14/15 passed, 1 MEDIUM severity violation

---

## Test Execution Summary

| Test Suite | Passed | Failed | Skipped |
|------------|--------|--------|---------|
| Unit Tests | 23 | 0 | 0 |
| Integration Tests | 12 | 1 | 0 |

**Failed Tests**:
- `MessageControllerIntegrationTest.createMessage_invalidSender` — NullPointerException in InstitutionService

---

## Recommendations for PASS

To achieve full PASS status:

1. **Fix failing test**: Resolve NullPointerException in InstitutionService when validating invalid sender
   - File: `src/main/java/com/bank/messaging/service/InstitutionService.java:42`
   - Expected: Return 400 with MSG-010 error

2. **Add missing test**: Verify message status is set to DRAFT on creation
   - Add assertion to: `MessageControllerIntegrationTest.createMessage_validRequest_returns201`

3. **Fix standards violation**: Change error code from 'MESSAGE_NOT_FOUND' to 'MSG-XXX' format
   - File: `src/main/java/com/bank/messaging/service/InstitutionService.java:42`

---

## Decision Log Archived

`decisions.md` archived 5 resolved entries to `decisions_archive.md`.
Current `decisions.md` now contains 8 open entries.

---

## Next Steps

1. Review failed criteria and standards violations above
2. Apply fixes to address recommendations
3. Re-run `/verify-epic epic-01-message-creation-service` to update verdict
```

### Phase 6 — Jira Status Transition

**Only execute if the Jira MCP tool (mcp-atlassian, PAT-authenticated
against jira.dotin.ir) is available.**

#### For PASS or PASS_WITH_NOTES Verdict

```
For each task in tasks.md:
  1. Extract Jira issue ID from the task (set during the Jira sync step
     that follows plan-tasks)
  2. Call Jira MCP tool: transitionJiraIssue
     - issueId: <jira-issue-id>
     - transition: "Ready for Review" (default, configurable)
  3. Log transition result
```

**Default Target Status**: "Ready for Review"

**Rationale**: Even with automated PASS, keep human as final approver for
compliance-sensitive platform. The target status is configurable via:
- Project setting: `.claude/settings.json` → `verify.jiraPassStatus`
- Or fallback to default: "Ready for Review"

#### For NEEDS_REVIEW Verdict

```
For each task in tasks.md:
  1. Extract Jira issue ID from the task
  2. Build failure summary:
     - List failed criteria
     - List standards violations
     - Include confidence score
  3. Call Jira MCP tool: addComment
     - issueId: <jira-issue-id>
     - comment: <formatted failure summary>
  4. Log comment result
```

**Comment Format**:

```
⚠️ Automated Verification Failed

Epic: EPIC-01 Message Creation Service
Confidence: 65/100
Verdict: NEEDS_REVIEW

Failed Criteria:
- TASK-epic-01-01: Invalid sender institution returns 400 — FAIL (NullPointerException)
- TASK-epic-01-01: Message created with DRAFT status — UNVERIFIED (no test)

Standards Violations:
- .claude/_coding-guidelines.md — Error Code Format: incorrect (MEDIUM)

To resolve:
1. Fix NullPointerException in InstitutionService
2. Add test for DRAFT status assertion
3. Correct error code format

Re-run verification after fixes.
```

**Do NOT transition status** — ticket stays in current state, visibly
blocked by the comment.

**Error Handling**:
- If Jira MCP tool unavailable: Log warning, continue without Jira actions
- If transition fails: Log error, include in verification report
- If comment fails: Log error, include in verification report

## Hard Rules

1. Never auto-transition to "Done" — always "Ready for Review" (or configured status)
2. Never fail verification silently — always produce a report with verdict
3. Never mark criterion PASS without evidence (test result or manual verification note)
4. Never conflate functional failures with standards violations — report separately
5. Never archive open decisions — only resolved/superseded
6. Never re-read `decisions_archive.md` — it's write-only from agent perspective
7. Always compute confidence score — even if 0/100
8. Always explain what would need to change to move from NEEDS_REVIEW to PASS
9. Always read task data from `specs/<epic-slug>/tasks.md`, never expect a JSON task file
10. Always read the three generated standards first in architecture → coding → documentation order, then spec and tasks; never use a directory glob. Read source docs only for cited source-level checks and always SWA_101 before SAW_102.
11. If any generated standards file is missing, continue but add the required `set-standards` Risk and skip only checks dependent on that file.

## Exit Criteria

Skill is done when:
1. Verification report written to `specs/<epic-slug>/verification-report.md`
2. Verdict clearly stated (PASS / PASS_WITH_NOTES / NEEDS_REVIEW)
3. All task acceptance criteria verified or flagged (PASS/FAIL/UNVERIFIED/MANUAL_QA_NEEDED)
4. Standards compliance checked and violations documented
5. Decision log archived if thresholds met
6. Jira status transitioned or comment posted (if Jira MCP available)

---

## Consumption Contract

### Re-running Verification

`verify-epic` is **idempotent** and safe to run repeatedly:
- **Same epic, same code**: Produces same verdict (deterministic)
- **After fixes applied**: Re-evaluates all criteria against new code
- **Side effects**: Only Jira status/comment actions (gated by verdict)

**When to re-run**:
1. After fixing failing tests or standards violations
2. After adding missing tests for UNVERIFIED criteria
3. After manual QA completion (update criteria status manually in tasks.md notes, then re-run)

### Jira Integration Requirements

For Jira status transitions to work:
1. The `mcp-atlassian` MCP server configured (PAT-authenticated against
   `https://jira.dotin.ir`), not the Atlassian Rovo Cloud connector
2. Tasks have Jira issue IDs recorded (set during the Jira sync step after plan-tasks)
3. User has permission to transition issues

If Jira integration isn't configured, verification still runs — Jira
actions are skipped with a warning.
