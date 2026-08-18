<!-- brainstorm:state {"answers":{},"idHighWater":{"BR":0,"FR":0,"NFR":0,"SEC":0,"CMP":0,"AC":0}} -->
<!--
  TEMPLATE — the brainstorm skill fills this in from answered questionnaires.
  The state metadata above stores canonical answers and ID high-water marks on
  every generation (see SKILL.md → Version marker). Do not edit it by hand.
  HTML guidance comments may be deleted once a section is written.
  Every requirement carries a stable ID (BR-/FR-/NFR-/SEC-/CMP-/AC-) so the
  downstream write-spec and plan-tasks skills can trace to it.
-->

<!-- brainstorm:generated:Title and Confidence:start -->
# Epic PRD: <Epic Title>

**Confidence Level:** <NN>% — <one-line reason for any shortfall, e.g. "3 high-impact risks open">

<!-- Confidence is COMPUTED, never asserted. See SKILL.md → Confidence formula. -->
<!-- brainstorm:generated:Title and Confidence:end -->

## Executive Summary
<!-- brainstorm:generated:Executive Summary:start -->
<!-- 3–5 sentences: what this initiative is and why it matters now. -->
<!-- brainstorm:generated:Executive Summary:end -->

## Problem Statement
<!-- brainstorm:generated:Problem Statement:start -->
<!-- The concrete problem being solved and the pain of the status quo. -->
<!-- brainstorm:generated:Problem Statement:end -->

## Vision
<!-- brainstorm:generated:Vision:start -->
<!-- The desired end state in one paragraph. -->
<!-- brainstorm:generated:Vision:end -->

## Goals
<!-- brainstorm:generated:Goals:start -->
<!-- Bulleted, outcome-oriented, ideally measurable. Trace to Business questions. -->
- 
<!-- brainstorm:generated:Goals:end -->

## Non-Goals
<!-- brainstorm:generated:Non-Goals:start -->
<!-- Explicitly out of scope for this initiative — prevents scope creep. -->
- 
<!-- brainstorm:generated:Non-Goals:end -->

## Epic Breakdown
<!-- brainstorm:generated:Epic Breakdown:start -->
<!-- The unit write-spec and plan-tasks operate on. Each epic gets a stable
     EPIC-NN id and a title after the colon; write-spec derives its <epic-slug>
     from the two ("EPIC-02: Message Validation" → epic-02-message-validation) and
     reads ONLY the requirement/AC IDs tagged to the epic it was invoked on, never
     the whole PRD. So every FR-/NFR-/SEC-/CMP-/AC- id defined in the sections
     below must be claimed by at least one epic here. Keep the "EPIC-NN: Title"
     heading shape exactly — the slug derivation depends on the colon. -->

### EPIC-01: <Epic Title>
<!-- One line on this epic's scope. -->
**Requirements:** <FR-/NFR-/SEC-/CMP- ids from the sections below, comma-separated>
**Acceptance Criteria:** <AC- ids from the section below>

### EPIC-02: <Epic Title>
**Requirements:** 
**Acceptance Criteria:** 
<!-- brainstorm:generated:Epic Breakdown:end -->

## Stakeholders
<!-- brainstorm:generated:Stakeholders:start -->
| Stakeholder | Interest / Responsibility |
|-------------|---------------------------|
|  |  |
<!-- brainstorm:generated:Stakeholders:end -->

## Business Requirements
<!-- brainstorm:generated:Business Requirements:start -->
<!-- BR-n. Derived from the Business questionnaire answers. -->
- **BR-1:** 
<!-- brainstorm:generated:Business Requirements:end -->

## Functional Requirements
<!-- brainstorm:generated:Functional Requirements:start -->
<!-- FR-n. What the platform must DO. Each should be testable. -->
- **FR-1:** 
<!-- brainstorm:generated:Functional Requirements:end -->

## Non-Functional Requirements
<!-- brainstorm:generated:Non-Functional Requirements:start -->
<!-- NFR-n. Throughput, latency, availability, scalability — pull concrete
     numbers from Architecture / Operations free-text answers, don't invent them. -->
- **NFR-1:** 
<!-- brainstorm:generated:Non-Functional Requirements:end -->

## Security Requirements
<!-- brainstorm:generated:Security Requirements:start -->
<!-- SEC-n. From the Security & Compliance questionnaire (signing, encryption,
     access control, audit trail). -->
- **SEC-1:** 
<!-- brainstorm:generated:Security Requirements:end -->

## Compliance Requirements
<!-- brainstorm:generated:Compliance Requirements:start -->
<!-- CMP-n. Regulatory reporting, sanctions/AML, data residency, retention. -->
- **CMP-1:** 
<!-- brainstorm:generated:Compliance Requirements:end -->

## Architecture Constraints
<!-- brainstorm:generated:Architecture Constraints:start -->
<!-- Deployment model, multi-tenancy, processing paradigm, HA/DR — from Architecture. -->
- 
<!-- brainstorm:generated:Architecture Constraints:end -->

## Integration Requirements
<!-- brainstorm:generated:Integration Requirements:start -->
<!-- Networks/rails, protocol adapters, message standards & transformations,
     external systems. From Business + Messaging Standards + Architecture. -->
- 
<!-- brainstorm:generated:Integration Requirements:end -->

## Acceptance Criteria
<!-- brainstorm:generated:Acceptance Criteria:start -->
<!-- AC-n. Verifiable pass/fail statements. Each should map to one or more
     FR/NFR/SEC/CMP. write-spec turns each into a failing-test-first plan. -->
- **AC-1:** 
<!-- brainstorm:generated:Acceptance Criteria:end -->

## Risks
<!-- brainstorm:generated:Risks:start -->
<!-- Impact MUST be one of: high | medium | low. Status one of: open | mitigated.
     Every row with Impact=high AND Status=open subtracts 5 from Confidence
     and MUST also appear under Open Issues. -->
| ID | Risk | Impact | Status | Mitigation |
|----|------|--------|--------|------------|
| R-1 |  | high/medium/low | open/mitigated |  |
<!-- brainstorm:generated:Risks:end -->

## Assumptions
<!-- brainstorm:generated:Assumptions:start -->
<!-- Things taken as true but not confirmed by an answered question. Each is a
     candidate Open Issue if it is load-bearing. -->
- 
<!-- brainstorm:generated:Assumptions:end -->

## Decision Log
<!-- brainstorm:generated:Decision Log:start -->
<!-- One row for every question whose selected answer DIVERGED from the
     labeled ← recommended option. This historical log records what a human
     decided against the default and why it matters; later rows may explicitly
     supersede or revert earlier rows. -->
| Date | Question (file § question) | Recommended | Chosen | Why it matters |
|------|----------------------------|-------------|--------|----------------|
|  |  |  |  |  |
<!-- brainstorm:generated:Decision Log:end -->

## Open Issues
<!-- brainstorm:generated:Open Issues:start -->
<!-- Every unresolved gap, made explicit and actionable: each open high-impact
     risk (−5 each), each unanswered optional question, and each load-bearing
     unconfirmed assumption. Mark which items affect Confidence; informational
     gaps do not. Generation is not blocked by these issues. -->
- 
<!-- brainstorm:generated:Open Issues:end -->

## Iteration History
<!-- Newest first. One dated entry per generation/update describing what changed
     versus the previous canonical answer state. Never rewrite older entries. -->
- **<YYYY-MM-DD>** — Initial generation from questionnaire state `<CANONICAL-STATE>`.
