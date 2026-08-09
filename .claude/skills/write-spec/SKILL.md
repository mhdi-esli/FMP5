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

1. Confirm `brainstorm/Epic_PRD.md` exists (fallback: `docs/PRD.md`)
2. Verify the named epic is listed in the Epic Breakdown section
   - If epic not found, stop and list available epic IDs instead of guessing
3. Read only the requirements and acceptance criteria tagged to that epic — not the whole PRD
4. Note the PRD's Confidence Level in the spec header for traceability, but do not block on it — a spec can legitimately surface gaps the PRD didn't catch

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
    - Exactly one option may carry `← recommended` as a text label — never a pre-checked box
    - Free text allowed where the answer is a specific value (library version, numeric SLA) rather than a closed choice
    - Unchecked boxes as `[ ]` — literal space between brackets, never `[]`
    - Tag each `(high-impact)` or `(standard)` — high-impact blocks a core acceptance-criterion path
    - Cross-reference to the section(s) it blocks

14. **Risks** — Identified risks

15. **Decision Log** — Answers that diverged from recommendations

16. **Iteration History** — Dated entries on each re-run

### Phase 2 — Update (Re-run)

When re-running after technical questions are answered:

1. Locate existing spec using Epic slug rule above — never create a second folder for an epic that already has one
2. A technical question is answered when:
   - Select-one: exactly one box checked
   - Select-all: at least one box checked
   - Multiple checks on a select-one is ambiguous — report it, don't resolve it yourself
   - Malformed checkbox (not `[ ]` or `[x]`) counts as unanswered and must be flagged
3. For each newly-answered question:
   - Fold the answer into the section it was blocking
   - Remove the `[blocked by TQ-n]` marker
   - If the selected answer differs from the labeled recommendation, add a Decision Log entry explaining the choice
4. Recompute Confidence Level after all answers folded in
5. Before appending Iteration History entry:
   - Check current entry count for this epic
   - If >15 entries, collapse all entries older than the 5 most recent into a single summary line:
     `entries 1–N collapsed — net effect: <one-line summary of what changed across them>`
   - Never collapse the 5 most recent entries
   - Never touch Decision Log — only Iteration History
6. Append dated entry to Iteration History describing what changed
7. Preserve any content a human added directly to the spec that didn't come from a technical question answer

## Confidence Level Formula

```
confidence = (acceptance criteria with resolved test + design + implementation
              / total acceptance criteria in this epic) × 100
             - 5 points per open (high-impact) Technical Question
```

Report as: `Confidence Level: 72% — 2 blocking technical questions`

## Epic Slug Derivation

Derive deterministically on every run:

1. Take the epic ID exactly as it appears in Epic Breakdown (e.g. `EPIC-02`), lowercase it → `epic-02`
2. Take the epic name following the colon (e.g. `Message Validation & Transformation`), lowercase it, strip punctuation, collapse whitespace to single hyphens → `message-validation-transformation`
3. Join the two with a hyphen: `epic-02-message-validation-transformation`
4. Before creating a new folder, check `specs/` for any existing folder starting with the same epic ID prefix (`epic-02-*`)
   - If one exists — even under a slightly different name (e.g. the epic was renamed in the PRD since the last run) — treat that as the target for update, not a new folder
   - Note the rename in that spec's Iteration History rather than creating a duplicate

## File Locations

```
{project root}/
├── brainstorm/Epic_PRD.md         # Input (primary — produced by brainstorm)
├── docs/PRD.md                    # Input (fallback)
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
- All open technical questions either answered-and-folded-in or clearly listed with what they block

"Confidence Level ≥ 90%" is the threshold a downstream implementation skill should check before starting to code against this spec — not a gate for this skill to produce output. A 60%-confidence draft that honestly shows its gaps is more useful than no output.
