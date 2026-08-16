<!-- generated from questionnaire state: <SIGNATURE> -->
<!--
  TEMPLATE — the brainstorm skill fills this in from answered questionnaires.
  SIGNATURE above is a change marker the skill computes and rewrites on every
  generation (see SKILL.md → Version marker). Do not edit it by hand.
  Guidance comments (<!-- ... -->) may be deleted once a section is written.
  Every requirement carries a stable ID (BR-/FR-/NFR-/SEC-/CMP-/AC-) so the
  downstream write-spec and plan-tasks skills can trace to it.
-->

# Epic PRD: <Epic Title>

**Confidence Level:** <NN>% — <one-line reason for any shortfall, e.g. "3 high-impact risks open">

<!-- Confidence is COMPUTED, never asserted. See SKILL.md → Confidence formula. -->

## Executive Summary
<!-- 3–5 sentences: what this initiative is and why it matters now. -->

## Problem Statement
<!-- The concrete problem being solved and the pain of the status quo. -->

## Vision
<!-- The desired end state in one paragraph. -->

## Goals
<!-- Bulleted, outcome-oriented, ideally measurable. Trace to Business questions. -->
- 

## Non-Goals
<!-- Explicitly out of scope for this initiative — prevents scope creep. -->
- 

## Epic Breakdown
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

## Stakeholders
| Stakeholder | Interest / Responsibility |
|-------------|---------------------------|
|  |  |

## Business Requirements
<!-- BR-n. Derived from the Business questionnaire answers. -->
- **BR-1:** 

## Functional Requirements
<!-- FR-n. What the platform must DO. Each should be testable. -->
- **FR-1:** 

## Non-Functional Requirements
<!-- NFR-n. Throughput, latency, availability, scalability — pull concrete
     numbers from Architecture / Operations free-text answers, don't invent them. -->
- **NFR-1:** 

## Security Requirements
<!-- SEC-n. From the Security & Compliance questionnaire (signing, encryption,
     access control, audit trail). -->
- **SEC-1:** 

## Compliance Requirements
<!-- CMP-n. Regulatory reporting, sanctions/AML, data residency, retention. -->
- **CMP-1:** 

## Architecture Constraints
<!-- Deployment model, multi-tenancy, processing paradigm, HA/DR — from Architecture. -->
- 

## Integration Requirements
<!-- Networks/rails, protocol adapters, message standards & transformations,
     external systems. From Business + Messaging Standards + Architecture. -->
- 

## Acceptance Criteria
<!-- AC-n. Verifiable pass/fail statements. Each should map to one or more
     FR/NFR/SEC/CMP. write-spec turns each into a failing-test-first plan. -->
- **AC-1:** 

## Risks
<!-- Impact MUST be one of: high | medium | low. Status one of: open | mitigated.
     Every row with Impact=high AND Status=open subtracts 5 from Confidence
     and MUST also appear under Open Issues. -->
| ID | Risk | Impact | Status | Mitigation |
|----|------|--------|--------|------------|
| R-1 |  | high/medium/low | open/mitigated |  |

## Assumptions
<!-- Things taken as true but not confirmed by an answered question. Each is a
     candidate Open Issue if it is load-bearing. -->
- 

## Decision Log
<!-- One row for every question whose selected answer DIVERGED from the
     labeled ← recommended option (and any other material choice). This is the
     record of what a human decided against the default and why it matters. -->
| Date | Question (file § question) | Recommended | Chosen | Why it matters |
|------|----------------------------|-------------|--------|----------------|
|  |  |  |  |  |

## Open Issues
<!-- Every gap that pulls Confidence below 100%, made explicit and actionable:
     each open high-impact risk (−5 each), each unanswered OPTIONAL question,
     and each load-bearing unconfirmed assumption. Not a blocker for generation
     — a visible, honest to-do list. -->
- 

## Iteration History
<!-- Newest first. One dated entry per generation/update describing what changed
     versus the previous SIGNATURE. Never rewritten — only appended. -->
- **<YYYY-MM-DD>** — Initial generation from questionnaire state `<SIGNATURE>`.
