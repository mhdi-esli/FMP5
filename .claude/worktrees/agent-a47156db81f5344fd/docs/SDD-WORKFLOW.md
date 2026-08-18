# Specification-Driven Development (SDD) Workflow

**Last Updated:** 2026-07-26
**Version:** 1.0

---

## Overview

Specification-Driven Development (SDD) is a structured pipeline that moves from ideation to verified implementation through discrete, tool-supported stages. Each stage produces artifacts that become inputs for the next stage.

```
┌─────────────┐   ┌─────────────┐   ┌─────────────┐   ┌─────────────┐   ┌─────────────┐
│  brainstorm │ → │  write-spec │ → │ implement   │ → │ verify-epic │ → │  init-docs  │
│             │   │             │   │   -epic     │   │             │   │             │
└─────────────┘   └─────────────┘   └─────────────┘   └─────────────┘   └─────────────┘
      ↓                 ↓                 ↓                 ↓                 ↓
   PRD.md           spec.md          code + tests    verification.md    domain docs
```

---

## Pipeline Stages

### Stage 1: Brainstorm (Product Requirements)

**Skill:** `/brainstorm`

**Purpose:** Transform initial idea or requirements document into a structured Epic PRD.

**Input:**
- Business requirements document
- User story or problem statement
- Technical constraints

**Output:**
- `docs/PRD.md` — Epic Product Requirements Document

**What happens:**
1. Creates PRD structure with confidence level
2. Asks clarification questions until ≥90% confidence
3. Defines goals, non-goals, requirements
4. Breaks down into epics with acceptance criteria
5. Records decisions in Decision Log

**When to run:**
- Starting a new project
- Major feature addition
- Requirements clarification needed

**Example:**
```
/brainstorm

Create a PRD for a Financial Messaging Platform...
```

---

### Stage 2: Write Spec (Technical Specification)

**Skill:** `/write-spec <epic-id>`

**Purpose:** Convert one epic from PRD into a detailed technical specification ready for implementation.

**Input:**
- `docs/PRD.md` (Epic PRD from Stage 1)

**Output:**
- `specs/<epic-slug>/spec.md` — Technical specification

**What happens:**
1. Extracts epic requirements and acceptance criteria
2. Designs architecture and components
3. Defines data flow and boundaries
4. Creates test strategy per acceptance criterion
5. Maps each AC to test + design + implementation approach
6. Asks technical questions for unresolved decisions
7. Computes confidence level

**When to run:**
- After PRD is approved (≥90% confidence)
- Before starting implementation
- When design decisions need clarity

**Example:**
```
/write-spec EPIC-01
```

**Required sections in spec.md:**
- Confidence Level
- Architecture
- Components
- Dependencies
- Boundaries
- Data Flow
- Testing Strategy
- Acceptance Criteria Mapping (Test Strategy → Design → Implementation)
- Implementation Plan
- Technical Questions
- Risks
- Decision Log

---

### Stage 3: Implement Epic (Code + Tests)

**Skill:** `/implement-epic <epic-id>`

**Purpose:** Implement one epic using strict TDD — one failing test, minimal code to pass, refactor.

**Input:**
- `specs/<epic-slug>/spec.md` (required)
- `specs/<epic-slug>/tasks.md` (optional — task-by-task mode)

**Output:**
- `src/...` — Implementation code
- `src/test/...` — Test code
- `specs/<epic-slug>/implementation-report.md`

**What happens:**
1. Preflight: Verify spec exists, determine work units
2. For each acceptance criterion (in order):
   - **RED:** Write failing test per spec's Test Strategy
   - **GREEN:** Implement minimal code to pass
   - **REFACTOR:** Clean up without changing behavior
   - Update task status
3. Compute coverage from actual test results
4. Compute confidence (coverage - blocked units)
5. Write implementation report

**TDD Rules:**
- Never batch all tests before implementation
- Never weaken a test to force a pass
- Maximum 3 retries per failing unit, then mark blocked
- Only this skill writes task status

**When to run:**
- After spec is ready (≥90% confidence recommended)
- When ready to write actual code

**Example:**
```
/implement-epic EPIC-01
```

---

### Stage 4: Verify Epic (Independent Verification)

**Skill:** `/verify-epic <epic-id>`

**Purpose:** Independently verify implementation against spec — don't trust self-reported results.

**Input:**
- `specs/<epic-slug>/spec.md`
- `specs/<epic-slug>/implementation-report.md`
- Actual code and tests

**Output:**
- `specs/<epic-slug>/verification-report.md`

**What happens:**
1. Preflight: Verify spec and implementation report exist
2. For each acceptance criterion:
   - Locate and execute actual tests
   - Check test quality (does it assert the right thing?)
   - Check implementation matches design approach
   - Assign status: PASS / PARTIAL / FAIL
3. Run all test suites, record actual counts
4. Cross-check against implementation-report.md
5. Compute confidence: (PASS / total) - discrepancies
6. Write verification report

**Verification Criteria:**
| Status | Requirements |
|--------|--------------|
| PASS | Test exists, asserts correctly, passes, implementation matches spec |
| PARTIAL | Some but not all conditions hold |
| FAIL | Missing entirely, or contradicts spec |

**When to run:**
- After implement-epic completes
- Before considering an epic "done"
- When auditing implementation quality

**Example:**
```
/verify-epic EPIC-01
```

---

### Stage 5: Init Docs (Living Documentation)

**Skill:** `/init-docs [domain]`

**Purpose:** Generate domain-driven documentation from actual code — not from spec or intent.

**Input:**
- Source code
- Tests
- API definitions

**Output:**
- `docs/domain-index.md`
- `docs/domain-<name>.md` (one per domain)

**What happens:**
1. Enumerate domains from package structure
2. Analyze source code, tests, APIs per domain
3. Extract domain model, endpoints, components, behaviors
4. Document only what's evidenced in code
5. Flag unverified/ambiguous items
6. Cross-reference acceptance criteria where traceable
7. Compute coverage (files analyzed / total files)
8. Generate/update documentation

**Documentation Includes:**
- Domain Model (entities, value objects, aggregates)
- API Endpoints (requests, responses, contracts)
- Key Behaviors/Business Rules (with code evidence)
- Key Components
- Features
- Acceptance Criteria mapping
- Unverified items
- Open Issues

**When to run:**
- After epic has implemented code
- When onboarding new team members
- Before releases
- When code has drifted from spec

**Example:**
```
/init-docs messaging
```

---

## Optional Stage: Plan Tasks

**Skill:** `/plan-tasks <epic-id>` (if exists)

**Purpose:** Break down epic into fine-grained tasks before implementation.

**Input:**
- `specs/<epic-slug>/spec.md`

**Output:**
- `specs/<epic-slug>/tasks.md`

**What happens:**
1. Decompose epic into tasks
2. Define dependencies between tasks
3. Estimate effort
4. Prioritize by dependency order

**When to run:**
- When epic is too large for single implementation pass
- When team needs granular tracking
- Before implement-epic

---

## Advisor Agents

Advisors provide specialized guidance during implementation and verification.

| Agent | Purpose | Source of Truth |
|-------|---------|-----------------|
| `architecture-advisor` | Architecture decisions, scalability, boundaries | `_architecture-reference.md` |
| `codestyle-advisor` | Code quality, standards compliance | `_coding-guidelines.md` |
| `security-advisor` | Security, compliance, encryption, access control | spec.md + `_architecture-reference.md` |
| `test-advisor` | Test coverage, test quality, spec alignment | spec.md (AC Mapping + Test Strategy) |

**When to invoke:**
- During implementation when decisions arise
- During verification when issues found
- When user asks for specialized review

---

## File Structure

```
{project root}/
├── docs/
│   ├── PRD.md                        # Epic PRD (Stage 1)
│   ├── domain-index.md               # Domain index (Stage 5)
│   └── domain-<name>.md              # Domain docs (Stage 5)
│
├── specs/
│   └── <epic-slug>/
│       ├── spec.md                   # Technical spec (Stage 2)
│       ├── tasks.md                  # Task breakdown (optional)
│       ├── implementation-report.md  # Implementation report (Stage 3)
│       └── verification-report.md    # Verification report (Stage 4)
│
├── src/
│   ├── main/                         # Implementation code (Stage 3)
│   └── test/                         # Test code (Stage 3)
│
└── .claude/
    ├── _architecture-reference.md    # Architecture decisions
    ├── _coding-guidelines.md         # Coding standards
    ├── agents/                       # Advisor agents
    └── skills/                       # SDD skills
```

---

## Confidence Levels

Each stage computes a confidence level:

| Stage | Formula |
|-------|---------|
| brainstorm | (answered questions / total questions) × 100 |
| write-spec | (ACs with resolved test+design+impl / total ACs) × 100 - 5×(open high-impact TQs) |
| implement-epic | (ACs with passing test AND impl / total ACs) × 100 - 5×(blocked high-impact units) |
| verify-epic | (ACs with PASS / total ACs) × 100 - 2×(PARTIAL) - 10×(discrepancies) |
| init-docs | (files analyzed / total files) × 100 |

**Target:** ≥90% confidence before proceeding to next stage.

---

## Decision Flow

```
                    ┌──────────────┐
                    │   Start      │
                    └──────┬───────┘
                           │
                           ▼
                    ┌──────────────┐
                    │  /brainstorm │
                    └──────┬───────┘
                           │
                    < 90% confidence?
                           │
              ┌────────────┼────────────┐
              │ Yes                     │ No
              ▼                         ▼
        Answer questions          ┌──────────────┐
              │                   │  PRD Ready   │
              └──────────────────►└──────┬───────┘
                                         │
                                         ▼
                                  ┌──────────────┐
                                  │ /write-spec  │
                                  └──────┬───────┘
                                         │
                                  < 90% confidence?
                                         │
                          ┌──────────────┼────────────┐
                          │ Yes                       │ No
                          ▼                           ▼
                    Answer TQs              ┌──────────────┐
                          │                 │  Spec Ready  │
                          └────────────────►└──────┬───────┘
                                                   │
                                                   ▼
                                            ┌──────────────┐
                                            │/implement-epic│
                                            └──────┬───────┘
                                                   │
                                                   ▼
                                            ┌──────────────┐
                                            │ /verify-epic │
                                            └──────┬───────┘
                                                   │
                                            Issues found?
                                                   │
                          ┌────────────────────────┼────────────┐
                          │ Yes                                 │ No
                          ▼                                     ▼
                    Fix and re-verify                    ┌──────────────┐
                          │                               │   Verified   │
                          └──────────────────────────────►└──────┬───────┘
                                                                 │
                                                                 ▼
                                                          ┌──────────────┐
                                                          │  /init-docs  │
                                                          └──────┬───────┘
                                                                 │
                                                                 ▼
                                                          ┌──────────────┐
                                                          │    Done      │
                                                          └──────────────┘
```

---

## Quick Reference

| Stage | Command | Input | Output | Confidence Target |
|-------|---------|-------|--------|-------------------|
| 1 | `/brainstorm` | Requirements | `docs/PRD.md` | ≥90% |
| 2 | `/write-spec EPIC-NN` | PRD | `specs/.../spec.md` | ≥90% |
| 3 | `/implement-epic EPIC-NN` | spec.md | Code + Tests + Report | Report actual |
| 4 | `/verify-epic EPIC-NN` | Code + Tests + Report | Verification report | Report actual |
| 5 | `/init-docs [domain]` | Code + Tests | Domain docs | Report actual |

---

## Common Patterns

### Starting a New Epic

```
1. /brainstorm          → Create PRD
2. /write-spec EPIC-01  → Create spec
3. (answer TQs)         → Reach 90% confidence
4. /implement-epic EPIC-01 → Build with TDD
5. /verify-epic EPIC-01 → Verify independently
6. /init-docs           → Document reality
```

### Fixing Issues Found in Verification

```
1. Review verification-report.md
2. Fix code/tests for each FAIL/PARTIAL
3. Re-run /verify-epic EPIC-01
4. Update docs with /init-docs if significant changes
```

### Adding a New Domain

```
1. Update PRD with new epic
2. /write-spec EPIC-02
3. /implement-epic EPIC-02
4. /verify-epic EPIC-02
5. /init-docs new-domain
```

---

## Hard Rules

1. **Never skip stages** — Each stage's output is the next stage's input
2. **Never fabricate confidence** — Always compute from actual results
3. **Never weaken tests** — If test fails, fix code, not test
4. **Never trust self-reports** — verify-epic re-derives from actual code
5. **Document reality, not intent** — init-docs analyzes code, not spec

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| PRD confidence stuck | Answer remaining questions, add missing requirements |
| Spec has blocked TQs | Answer technical questions before implementing |
| Tests won't pass | Check implementation matches design approach |
| Verification shows discrepancies | Fix implementation or update spec (with justification) |
| Docs don't match code | Re-run init-docs after code changes |

---

## References

- Architecture Reference: `.claude/_architecture-reference.md`
- Coding Guidelines: `.claude/_coding-guidelines.md`
- Skills Directory: `.claude/skills/`
- Agents Directory: `.claude/agents/`
