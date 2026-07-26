---
name: write-spec
description: >
  Converts one named epic from an approved Epic PRD into a detailed technical
  specification — architecture, components, data flow, and a TDD-ordered
  test-first breakdown of every acceptance criterion. Use this after an Epic
  PRD exists and the user wants to move from "what are we building" to "how
  exactly are we building it" — e.g. "write the spec for EPIC-01," "let's
  design the messaging ingestion component," or "I'm ready to start
  implementation planning for this epic."
---

# Write Spec Skill

Converts one named epic from an approved Epic PRD into a detailed technical specification following TDD principles.

## Workflow

### Phase 0 — Preflight

1. Confirm `docs/PRD.md` (or `brainstorm/Epic_PRD.md`) exists
2. Verify the named epic is listed in the Epic Breakdown section
3. If epic not found, stop and list available epic IDs
4. Read only the requirements and acceptance criteria tagged to that epic
5. Note the PRD's Confidence Level for traceability

### Phase 1 — Draft

Generate the spec with these required sections:

#### Required Sections

1. **Confidence Level** — Computed score plus one-line reason for any shortfall

2. **Architecture** — How this epic's components fit into the platform

3. **Components** — For each component:
   - Name
   - Responsibility
   - Interface (exposes/consumes)

4. **Dependencies** — Split:
   - Internal (other components/epics)
   - External (libraries, third-party services) with versions

5. **Boundaries** — What's in scope vs. handled elsewhere

6. **Data Flow** — Path through components including error branches

7. **Testing Strategy** — Target split across unit/integration/E2E

8. **Unit Tests** — Concrete scenarios

9. **Integration Tests** — Concrete scenarios

10. **End-to-End Tests** — Concrete scenarios

11. **Acceptance Criteria Mapping** — For each AC (by PRD ID):
    - Test strategy (what a failing test looks like)
    - Design approach
    - Implementation approach

12. **Implementation Plan** — Ordered steps in red-green-refactor cadence

13. **Technical Questions** — Open items with:
    - 2-5 options tagged `(select one)` or `(select all that apply)`
    - One option may have `← recommended` label
    - Free text for specific values
    - Unchecked boxes as `[ ]` (space between brackets)
    - Tag each `(high-impact)` or `(standard)`

14. **Risks** — Identified risks

15. **Decision Log** — Answers that diverged from recommendations

16. **Iteration History** — Dated entries on each re-run

### Phase 2 — Update (Re-run)

When re-running after technical questions are answered:

1. Locate existing spec using Epic slug rule (never create duplicate folder)
2. For each answered question:
   - Fold answer into blocking section
   - Remove `[blocked by TQ-n]` marker
   - Add Decision Log entry if diverged from recommendation
3. Recompute Confidence Level
4. Append dated entry to Iteration History
5. Preserve any human-added content

## Confidence Level Formula

```
confidence = (acceptance criteria with resolved test + design + implementation
              / total acceptance criteria in this epic) × 100
             - 5 points per open (high-impact) Technical Question
```

Report as: `Confidence Level: 72% — 2 blocking technical questions`

## Epic Slug Derivation

Derive deterministically on every run:

1. Take epic ID → lowercase → `epic-01`
2. Take epic name after colon → lowercase, strip punctuation, collapse whitespace to hyphens → `message-creation-service`
3. Join with hyphen: `epic-01-message-creation-service`
4. Before creating new folder, check `specs/` for existing folder starting with same epic ID prefix → update that folder if found

## File Locations

```
{project root}/
├── docs/PRD.md                    # Input (Epic PRD)
├── specs/<epic-slug>/spec.md      # Output (Technical Spec)
└── .claude/skills/write-spec/
    └── SKILL.md                   # This file
```

## Technical Question Format

```
**TQ-1: Question text here? (select one)**

[ ] Option A ← recommended
[ ] Option B
[ ] Option C
[ ] Option D

Blocks: Architecture, Data Flow
Impact: (high-impact)
```

## Hard Rules

1. Never pre-check a technical question's recommended option
2. Never fabricate Confidence Level — compute after content populated
3. Never resolve an ambiguous (multi-checked) question yourself — report it
4. Never silently drop a `[blocked by TQ-n]` marker
5. Never rewrite a human-edited spec from scratch — preserve additions
6. Always write unchecked boxes as `[ ]` — literal space between brackets
7. Never introduce facts/libraries/SLAs that don't trace to requirements, PRD, answered question, or platform convention — add Technical Question instead
8. Never derive epic slug any other way than the rule above
9. Never create a second output folder for an epic that already has one

## Exit Criteria

Skill is done when spec is written/updated with:
- Accurate computed Confidence Level
- All open technical questions either answered-and-folded-in or clearly listed with blockers

"Confidence Level ≥ 90%" is the threshold for downstream implementation skill — not a gate for this skill to produce output.
