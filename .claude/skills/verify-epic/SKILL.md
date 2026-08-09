---
name: verify-epic
description: >
  Final quality gate for an epic after implement-epic completes. Confirms
  implementation satisfies spec, standards, and acceptance criteria.
  Produces PASS/NEEDS_REVIEW verdict that drives Jira status transitions.
---

# Verify Epic Skill

Final quality gate confirming implementation satisfies spec, standards, and acceptance criteria.

## Purpose

This skill is the **terminal verification step** that:
1. Validates each task's acceptance criteria against actual implementation
2. Cross-checks implementation against `/standards/*.md` (distinct from functional checks)
3. Produces a PASS/NEEDS_REVIEW verdict
4. Transitions Jira tickets or posts failure comments (gated by verdict)
5. Archives decision log when thresholds are met

## Inputs

```
{project root}/
├── tasks/<epic-slug>-tasks.json           (required — from plan-tasks)
├── specs/<epic-slug>/spec.md              (required — from write-spec)
├── standards/*.md                          (required — from set-standards)
├── specs/<epic-slug>/implementation-report.md  (from implement-epic)
├── decisions.md                            (optional — project or epic scoped)
└── .claude/skills/verify-epic/SKILL.md
```

## Output

```
{project root}/
├── verification/<epic-slug>-verify-report.md
└── decisions_archive.md                    (created/updated if archival triggered)
```

## Workflow

### Phase 0 — Preflight

1. Confirm `tasks/<epic-slug>-tasks.json` exists
   - If not, stop and report: "No task list found. Run /plan-tasks first."
2. Confirm `specs/<epic-slug>/spec.md` exists
   - If not, stop and report: "No spec found for this epic."
3. Load `/standards/*.md` files
   - If missing, warn: "Standards not found. Standards compliance checks will be skipped."
4. Check for `implementation-report.md`
   - If present, use as context for what was implemented
   - If absent, continue (verification can run without it)
5. Derive epic-slug using same rule as other skills:
   - Epic ID lowercase + epic name slug → `epic-01-message-creation-service`

### Phase 1 — Task Acceptance Criteria Verification

For each task in `/tasks/<epic-slug>-tasks.json`:

#### Verification Strategy

```
For each acceptanceCriteria:
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

1. Check for relevant test files in `filesAffected` list from task JSON
2. Run tests: `mvn test -Dtest=<TestClass>` (or appropriate test runner)
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

### Phase 2 — Standards Compliance Verification

Distinct from functional acceptance criteria — check implementation against `/standards/*.md`.

#### Standards Check Process

For each `standardsRefs` entry in each task:

1. Parse the reference: `api-and-messaging.md#url-structure` → file + section
2. Extract the rule from the standards file
3. Check implementation for compliance

#### Standards Check Categories

| Category | Check Type | Verification Method |
|----------|------------|---------------------|
| URL structure | Pattern match | Parse controller annotations, validate `/api/v{N}/{resource}` |
| HTTP status codes | Range check | Verify controller returns correct status per operation type |
| Error code format | Pattern match | Verify error codes match `{DOMAIN}-{NUMBER}` format |
| Envelope format | Schema validation | Verify response DTO has `data`, `meta`, `errors` structure |
| Headers | Presence check | Verify required headers in controller signature or filter |
| Security | Annotation check | Verify `@PreAuthorize` or equivalent on protected endpoints |
| Layering | Code structure | Verify controller doesn't call repository directly |
| Naming | Pattern match | Verify class/method names follow conventions |
| Logging | Content check | Verify no sensitive data in logs (scan for token/password patterns) |

#### Standards Violation Record

```json
{
  "standardRef": "api-and-messaging.md#error-codes",
  "rule": "Error codes must use {DOMAIN}-{NUMBER} format with Persian messages",
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
    "api-and-messaging.md",
    "security-and-auth.md",
    "coding-conventions.md"
  ],
  "violations": [
    {
      "standardRef": "api-and-messaging.md#error-codes",
      "severity": "MEDIUM",
      "description": "Error code format violation in InstitutionService.java"
    }
  ],
  "summary": {
    "total": 15,
    "passed": 14,
    "failed": 1,
    "bySeverity": {
      "CRITICAL": 0,
      "HIGH": 0,
      "MEDIUM": 1,
      "LOW": 0
    }
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

#### Confidence Breakdown

```json
{
  "score": 82,
  "maxScore": 100,
  "breakdown": {
    "functional": {
      "score": 50,
      "maxScore": 60,
      "detail": "23/28 criteria passed"
    },
    "standards": {
      "score": 28,
      "maxScore": 30,
      "detail": "14/15 standards checks passed"
    },
    "testCoverage": {
      "score": 10,
      "maxScore": 10,
      "detail": "All tasks have passing tests"
    },
    "penalties": {
      "unverifiedCriteria": -5,
      "manualQaNeeded": 0
    }
  },
  "verdict": "PASS_WITH_NOTES",
  "blockingIssues": [],
  "reason": "Functional criteria mostly met, standards compliant, but 2 criteria require manual verification"
}
```

### Phase 4 — Decision Log Archival

Before writing verification report, check `decisions.md`:

#### Archival Rules

```
archive_trigger = (
  decision_count > 30
  OR
  (verdict == "PASS" AND epic_reached_pass)
)

if archive_trigger:
  1. Read all entries from decisions.md
  2. Filter: resolved OR superseded (NOT open)
  3. For each filtered entry:
     - Format as single line: date | decision | outcome | ref
     - Append to decisions_archive.md
  4. Remove filtered entries from decisions.md
  5. Keep all open entries in decisions.md
```

#### Archive Entry Format

```markdown
| Date | Decision | Outcome | Ref |
|------|----------|---------|-----|
| 2026-08-09 | Use Caffeine cache for message definitions | Implemented | ADR-005 |
| 2026-08-07 | REST over gRPC for external API | Implemented | ADR-003 |
| 2026-08-05 | PostgreSQL over MongoDB for ACID compliance | Implemented | ADR-001 |
```

**Note:** `decisions_archive.md` is write-only from the agent's perspective. It's never loaded as context. The archival keeps `decisions.md` small and cheap to load on every skill run.

### Phase 5 — Write Verification Report

Write structured Markdown report to `/verification/<epic-slug>-verify-report.md`.

#### Report Structure

```markdown
# Verification Report: EPIC-01 Message Creation Service

**Epic**: epic-01-message-creation-service
**Spec**: specs/epic-01-message-creation-service/spec.md
**Tasks**: tasks/epic-01-message-creation-service-tasks.json
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

### TASK-epic-01-02: Add message validation service

**Status**: PASS

| Criterion | Status | Evidence |
|-----------|--------|----------|
| Validates all required fields present | ✅ PASS | MessageValidationServiceTest.validateAllFields_present passes |
| Returns Persian error messages | ✅ PASS | MessageValidationServiceTest.errorMessages_inPersian passes |

**Summary**: 2/2 passed

---

## Standards Compliance

**Standards Checked**: api-and-messaging.md, security-and-auth.md, coding-conventions.md

| Standard | Status | Details |
|----------|--------|---------|
| api-and-messaging.md#url-structure | ✅ PASS | All endpoints follow /api/v{N}/{resource} pattern |
| api-and-messaging.md#http-status-codes | ✅ PASS | Correct status codes used per operation |
| api-and-messaging.md#error-codes | ❌ FAIL | **MEDIUM**: Found 'MESSAGE_NOT_FOUND' in InstitutionService.java:42, expected 'MSG-XXX' |
| security-and-auth.md#oauth2-resource-server | ✅ PASS | All endpoints protected with @PreAuthorize |
| coding-conventions.md#layering-pattern | ✅ PASS | No direct repository calls from controllers |

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

**Only execute if Jira MCP tool is available.**

#### For PASS or PASS_WITH_NOTES Verdict

```
For each task in tasks.json:
  1. Extract Jira issue ID from task (if present)
  2. Call Jira MCP tool: transitionJiraIssue
     - issueId: <jira-issue-id>
     - transition: "Ready for Review" (default, configurable)
  3. Log transition result
```

**Default Target Status**: "Ready for Review"

**Rationale**: Even with automated PASS, keep human as final approver for compliance-sensitive platform. The target status is configurable via:
- Project setting: `.claude/settings.json` → `verify.jiraPassStatus`
- Or fallback to default: "Ready for Review"

#### For NEEDS_REVIEW Verdict

```
For each task in tasks.json:
  1. Extract Jira issue ID from task (if present)
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
- api-and-messaging.md#error-codes: Error code format incorrect (MEDIUM)

To resolve:
1. Fix NullPointerException in InstitutionService
2. Add test for DRAFT status assertion
3. Correct error code format

Re-run verification after fixes.
```

**Do NOT transition status** — ticket stays in current state, visibly blocked by the comment.

#### MCP Tool Call

Direct invocation (not webhook):

```javascript
// PASS scenario
await jiraClient.transitionIssue({
  issueId: "PROJ-123",
  transition: "Ready for Review"
});

// NEEDS_REVIEW scenario
await jiraClient.addComment({
  issueId: "PROJ-123",
  comment: "⚠️ Automated Verification Failed\n\n..."
});
```

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

## Exit Criteria

Skill is done when:
1. Verification report written to `/verification/<epic-slug>-verify-report.md`
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
3. After manual QA completion (update criteria status to PASS manually, then re-run)

**Manual QA process**:
1. Review criteria flagged as MANUAL_QA_NEEDED
2. Perform manual verification
3. Update task JSON: change status to PASS or FAIL
4. Re-run `verify-epic` to recompute confidence and verdict

### Integration with CI/CD

Can be wired as CI gate:

```yaml
# .github/workflows/verify-epic.yml
- name: Verify Epic
  run: |
    # Run verification
    claude /verify-epic $EPIC_SLUG

    # Check verdict
    VERDICT=$(jq -r '.verdict' verification/$EPIC_SLUG-verify-report.md)
    if [ "$VERDICT" == "NEEDS_REVIEW" ]; then
      echo "Epic verification failed"
      exit 1
    fi
```

### Jira Integration Requirements

For Jira status transitions to work:

1. Jira MCP server configured in `.claude/settings.json`
2. Tasks have Jira issue IDs in JSON (set during Jira sync step)
3. User has permission to transition issues

If Jira integration not configured, verification still runs — Jira actions are skipped with a warning.
