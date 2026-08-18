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

Convert one named epic from an approved Epic PRD into exactly one detailed,
TDD-ordered technical specification. This skill specifies work only; it never
implements code, creates a task list, invokes `plan-tasks`, or changes external
tickets.

## Pipeline Position

`brainstorm` → `brainstorm/Epic_PRD.md` → `write-spec` →
`specs/<epic-slug>/spec.md` → `plan-tasks`

Every pipeline transition is explicit.

## File Layout

```
{project root}/
├── brainstorm/Epic_PRD.md         # Input (primary — produced by brainstorm)
├── .claude/_architecture-reference.md (input, from set-standards, if present)
├── .claude/_coding-guidelines.md  (input, from set-standards, if present)
├── .claude/_documentation-standards.md (input, from set-standards, if present)
├── specs/<epic-slug>/spec.md      # Output (Technical Spec)
└── .claude/skills/write-spec/
    └── SKILL.md                   # This file
```

## Input

`Epic name` is required. Match an `EPIC-NN` ID or its unambiguous title in
`brainstorm/Epic_PRD.md`'s Epic Breakdown. If the argument is missing, absent,
or ambiguous, stop and list the available epic IDs and titles instead of
guessing.

The PRD must assign stable requirement IDs and acceptance-criterion IDs to each
epic. Accept either of these equivalent mappings:

- IDs listed directly under the epic's `Requirements` and `Acceptance Criteria`
  fields; or
- globally defined ID entries carrying an explicit `Epic` tag.

If the named epic has no reliable ID mapping, stop and report that the PRD must
be regenerated or corrected with the brainstorm Epic Breakdown contract. Never
infer scope from prose proximity.

## Workflow — state machine

### Phase 0 — Preflight

Read context in this **exact fixed order** — never directory-listing order,
since filesystem iteration order isn't guaranteed alphabetical and an
inconsistent order across runs busts prompt-cache reuse even when the
content hasn't changed:

```
1. .claude/_architecture-reference.md   (if present)
2. .claude/_coding-guidelines.md        (if present)
3. .claude/_documentation-standards.md  (if present)
4. brainstorm/Epic_PRD.md
-- CACHE BOUNDARY: everything above is stable within this run; content
   below (the epic's own spec, if updating) is what changes turn to turn --
5. specs/<epic-slug>/spec.md (if this is a Phase 2 re-run)
```

If any of items 1–3 is missing, continue, but add a Risk recommending that
`set-standards` be run before further epics are specified.

Confirm `brainstorm/Epic_PRD.md` exists. There is no fallback PRD path. If it is
missing, stop and report:

`No Epic PRD found. Run /brainstorm first.`

Resolve and scope the named epic before drafting:

1. Parse the Epic Breakdown's `EPIC-NN: <title>` entries.
2. Match the supplied epic by ID or unambiguous title. If no unique match exists,
   stop and list available IDs and titles.
3. Derive or reuse the epic slug using **Epic Slug Derivation** below.
4. Collect only the requirement IDs and AC IDs explicitly assigned to the named
   epic. Shared IDs may appear in more than one epic and remain in scope for each
   one that explicitly claims them.
5. Resolve those IDs to their detailed definitions elsewhere in the PRD. Do not
   incorporate another epic's requirements, ACs, open questions, or design
   choices merely because they occur nearby. Project-wide constraints and
   decisions apply only when their text is explicitly global or their IDs are
   mapped to this epic.
6. Validate that every collected ID resolves to one authoritative detailed
   definition and every mapped AC has its complete criterion text. The Epic
   Breakdown may repeat an ID and summary text solely as its mapping declaration;
   compare that summary with the detailed definition rather than counting it as a
   second definition. Stop and report exact missing, genuinely duplicate, or
   conflicting IDs rather than inventing scope.

Record the PRD Confidence Level in the spec header for traceability, but do not
use it as a generation gate. Follow applicable conventions from
`.claude/_architecture-reference.md`, `.claude/_coding-guidelines.md`, and
`.claude/_documentation-standards.md`; cite the file and section that supports a
specific design choice instead of restating a remembered rule.

### Phase 1 — Draft

This phase always runs after valid epic scoping and never blocks the whole spec
merely because technical information is incomplete.

#### Step 1 — Build the scoped source set

Create a working set containing the named epic's ID/title, mapped requirement
IDs and definitions, mapped AC IDs and full criteria, epic-specific open issues,
and applicable global constraints and decisions. This working set is the only
PRD content from which epic design may be derived.

Keep source IDs unchanged. Do not renumber PRD requirements or ACs, and do not
copy requirements from a different epic to make the design seem complete.

#### Step 2 — Specify each acceptance criterion test-first

Walk every mapped AC exactly once. For each criterion, write these three entries
in this order:

1. **Test strategy** — the failing test or smallest coherent failing test set,
   test layer, setup/input, observable assertion, and why the unimplemented
   behavior makes it fail.
2. **Design approach** — components, interfaces, data flow, boundaries, and
   applicable standards needed to satisfy that test.
3. **Implementation approach** — the minimum production change that can make the
   test pass, followed by the allowed behavior-preserving refactor scope.

Do not decide implementation first and retrofit a test. Keep each entry scoped
to the criterion while cross-referencing shared components rather than
silently merging ACs. Preserve exact response values, limits, messages, and
other observable behavior when the scoped source defines them.

#### Step 3 — Populate the cross-cutting sections

Populate every Required Section to the extent supported by the scoped source
set and applicable standards. Every fact, figure, library/protocol choice,
schema detail, or SLA must trace to a scoped requirement, mapped AC, answered
Technical Question, existing project decision, or cited `.claude/_*.md`
convention. Never invent specificity to fill a section.

When a decision required by a section or AC cannot be resolved from those
sources:

1. assign the next stable `TQ-n` ID;
2. add a Technical Question tagged `(high-impact)` if it blocks a core AC path,
   otherwise `(standard)`;
3. add `[blocked by TQ-n]` at every affected test, design, implementation, or
   cross-cutting location; and
4. continue drafting unaffected content.

Carry an unresolved PRD question assigned to this epic into the spec as a
Technical Question. Offer 2–5 choices only when the source supports real
alternatives; otherwise use a free-text answer field and do not fabricate
options. A missing technical detail never prevents writing the partial spec.

#### Step 4 — Compute and write

After all AC mappings, cross-cutting sections, blockers, and Technical Questions
are complete, compute Confidence using the formula below. Write exactly one
artifact at `specs/<epic-slug>/spec.md` regardless of score. Never create an
alternate draft path or a second spec for the same epic.

### Phase 2 — Re-run

When the target spec already exists, reconcile it in place rather than
regenerating it:

1. Reuse the existing folder selected by the Epic Slug rule. Compare the current
   scoped PRD source set and standards with the source IDs, content, and blockers
   already represented in the spec.
2. Parse each Technical Question literally:
   - select-one is answered only by exactly one `[x]` option;
   - select-all is answered by at least one `[x]` option;
   - a recommended option is only a label and is never implicitly selected;
   - multiple checks on select-one are ambiguous and remain unresolved;
   - any checkbox other than `[ ]` or `[x]`, including `[]` or `[X]`, is
     malformed and remains unanswered; and
   - a free-text question is answered only by non-placeholder human text in its
     answer field.
3. Report ambiguous and malformed questions by ID and leave their blockers
   intact. Never normalize malformed syntax in a way that chooses an answer.
4. For each newly answered question, fold the selected answer into every
   location tagged `[blocked by TQ-n]`, then remove only that question's markers.
   If the selected answer differs from the single labeled recommendation, append
   one Decision Log entry containing the question, recommendation, choice, and
   why the divergence matters. Do not log a choice that follows the
   recommendation.
5. Apply scoped PRD or standards changes only where their source changed. Add
   new mapped ACs, requirements, and questions; update changed ones; and flag a
   removed or conflicting source for human review instead of silently deleting
   human-authored design or historical decisions.
6. Preserve human edits, existing Technical Question IDs, Decision Log entries,
   and unaffected wording exactly. Assign new questions the next unused TQ
   suffix; never renumber or reuse IDs.
7. Revalidate every mapped AC in test → design → implementation order, recompute
   Confidence from the reconciled state, and append a dated Iteration History
   entry describing substantive changes. If nothing substantive changed, do not
   append a misleading change entry.
8. Before appending, trim Iteration History only under the rule in Hard Rule 9.
   Never collapse or rewrite Decision Log.

## Technical Question Format

Same conventions as `brainstorm`:

```
**TQ-1: Question text here? (select one)**

[ ] Option A ← recommended
[ ] Option B
[ ] Option C
[ ] Option D

Blocks: Architecture, Data Flow
Impact: (high-impact)
```

- 2-5 options, tagged `(select one)` or `(select all that apply)`
- Exactly one option may carry `← recommended` as a text label — never a pre-checked box
- Free text allowed where the answer is a specific value (a library version, a numeric SLA) rather than a closed choice
- Always write unchecked boxes as `[ ]` — a literal space between the brackets, never `[]`

## Confidence Level Formula

Compute from distinct mapped AC IDs after drafting or reconciliation:

```
confidence = (
  acceptance criteria with fully resolved test + design + implementation
  / total acceptance criteria mapped to this epic
) × 100
− (5 × open Technical Questions tagged high-impact)
```

An AC counts as fully resolved only when all three entries are actionable and
none contains an unresolved blocker. Do not double-count a shared AC or multiple
questions affecting one AC. If the epic has no mapped ACs, confidence is `0%`
and the missing mapping is an input defect, not a divide-by-zero case. Clamp the
final result to 0–100 and report the numerator, denominator, penalty, percentage,
and one-line reason for any shortfall, for example:

`Confidence Level: 70% — 4/5 ACs resolved (80%) − 10 points for 2 open high-impact TQs.`

## Epic Slug Derivation

`<epic-slug>` must be derived the same deterministic way on every run — this is what lets Phase 2 find and update the same file instead of forking a new one each time:

1. Take the epic ID exactly as it appears in Epic Breakdown (e.g. `EPIC-02`), lowercase it → `epic-02`
2. Take the epic name following the colon (e.g. `Message Validation & Transformation`), lowercase it, strip punctuation, collapse whitespace to single hyphens → `message-validation-transformation`
3. Join the two with a hyphen: `epic-02-message-validation-transformation`
4. Before creating a new folder, check `specs/` for any existing folder starting with the same epic ID prefix (`epic-02-*`). If one exists — even under a slightly different name (e.g. the epic was renamed in the PRD since the last run) — treat that as the target for update, not a new folder, and note the rename in that spec's Iteration History rather than creating a duplicate

## Required spec.md Sections

Write sections in this order:

1. **Title and metadata** — epic ID/name, epic slug, source PRD path, scoped
   requirement IDs, scoped AC IDs, PRD Confidence Level, and generated/updated
   date.
2. **Confidence Level** — computed numerator, denominator, high-impact penalty,
   final percentage, and reason for shortfall.
3. **Architecture** — how the epic fits into the platform, citing applicable PRD
   constraints and `.claude/_architecture-reference.md` sections.
4. **Components** — each component's name, responsibility, exposed interface, and
   consumed interface.
5. **Dependencies** — internal components/epics and external libraries/services,
   with versions only where a source establishes them.
6. **Boundaries** — explicit in-scope/out-of-scope behavior and system-context
   edges: what calls in and what this epic calls out to.
7. **Data Flow** — success, validation, error, and persistence/integration paths
   supported by this epic's source set.
8. **Testing Strategy** — unit/integration/end-to-end responsibilities and any
   sourced non-functional or security test approach.
9. **Unit Tests**, **Integration Tests**, and **End-to-End Tests** — concrete
   sourced scenarios; write `None required by the scoped criteria` where a layer
   genuinely has no scenario instead of inventing one.
10. **Acceptance Criteria Mapping** — one subsection per mapped PRD AC ID, each
    containing **Test strategy**, **Design approach**, and **Implementation
    approach** in exactly that order. Include requirement IDs satisfied by the
    criterion.
11. **Implementation Plan** — dependency-ordered RED → GREEN → REFACTOR steps
    that reference AC IDs. It is a technical sequence, not the task list owned by
    `plan-tasks`.
12. **Technical Questions** — open and answered questions with stable IDs,
    impact, and blocked sections/ACs; write `None` if none arose.
13. **Risks** — unresolved questions, missing standards, source conflicts, and
    other sourced delivery risks; write `None` if none remain.
14. **Decision Log** — divergences from recommended Technical Question choices;
    write `None` if no divergence exists and never discard prior entries.
15. **Iteration History** — initial generation and substantive re-runs, dated and
    newest first or in the existing spec's established order.

## Validation Before Exit

Validate the generated or reconciled spec before completing:

1. The epic resolves to exactly one Epic Breakdown entry and one reused/new slug.
2. Scoped requirement and AC IDs exactly match the PRD's explicit epic mapping.
3. Every scoped ID resolves to one authoritative detailed definition; repeated
   Epic Breakdown mapping summaries agree with it, and no unassigned epic content
   leaked into the design.
4. Every mapped AC appears exactly once in Acceptance Criteria Mapping and uses
   test → design → implementation order.
5. Every concrete design claim is traceable to a source or answered TQ.
6. Every unresolved decision has a stable TQ ID, impact tag, blocked-location
   list, and inline marker at each affected location.
7. Every question uses valid selection/free-text syntax; malformed or ambiguous
   answers remain unresolved.
8. Confidence arithmetic matches current AC and question states.
9. Human additions, stable TQ IDs, Decision Log, and unaffected content were
   preserved on re-run.
10. The only output path is `specs/<epic-slug>/spec.md`.

Fix structural defects before exit. Do not answer an unresolved question merely
to make validation pass.

## Hard Rules

1. Never pre-check a Technical Question's recommended option; the arrow is a
   text label only.
2. Never fabricate, estimate, or round up Confidence. Compute it only after AC
   mappings and Technical Questions are complete.
3. Never resolve an ambiguous or malformed answer. Report its TQ ID and preserve
   its blockers.
4. Never silently remove `[blocked by TQ-n]`; remove it only after that exact TQ
   is unambiguously answered and folded into every affected location.
5. Never regenerate a human-edited spec from scratch. Reconcile changed source
   material and preserve unaffected additions and wording.
6. Always write unchecked boxes as `[ ]`, never `[]`, and recognize checked
   boxes only as lowercase `[x]`.
7. Never introduce a fact, figure, library/protocol choice, schema detail, or SLA
   without a traceable scoped source, answered TQ, existing project decision, or
   applicable standards citation.
8. Derive/reuse `<epic-slug>` only by the rule above. Never create a second spec
   folder for an existing epic ID, including after a rename.
9. On re-run, before appending Iteration History, count its entries. Only when it
   exceeds approximately 15 entries may entries older than the 5 most recent be
   replaced by one line: `entries 1–N collapsed — net effect: <one-line
   summary>`. Preserve the 5 most recent entries. Never collapse, trim, reorder,
   or otherwise rewrite Decision Log.
10. Always read Phase 0 inputs in the fixed order and place the cache boundary
    between stable standards/PRD content and the evolving epic spec. Never use a
    directory glob or listing to choose read order.
11. Scope PRD content by explicit epic-to-ID mapping. Never absorb another
    epic's requirement, AC, or question by proximity or thematic similarity.
12. Every mapped AC must be specified in test → design → implementation order.
    Never retrofit a test after selecting implementation.
13. Always write/update exactly `specs/<epic-slug>/spec.md`. Never implement
    code, create `tasks.md`, invoke another pipeline skill, call Jira, or change
    external tickets.

## Exit Criteria

The run is complete when `specs/<epic-slug>/spec.md` has been written or updated
with:

- exact epic-scoped requirement and AC traceability;
- one TDD-ordered mapping for every mapped AC;
- architecture, components, dependencies, boundaries, and data flow grounded in
  sources or visibly blocked by stable Technical Questions;
- every open question listed with impact and affected locations;
- preserved human edits and histories on re-run; and
- accurate Confidence arithmetic.

Confidence is informational and never gates output. Downstream work may choose
its own readiness threshold; this skill does not invoke it automatically.

Report to the user:

`EPIC-NN <name> specified at <confidence>% confidence. Spec: specs/<epic-slug>/spec.md`

Mention unresolved and malformed/ambiguous TQ IDs concisely when present.

## Example Invocation

```
/write-spec EPIC-01

Phase 0: Loaded .claude/_architecture-reference.md
         Loaded .claude/_coding-guidelines.md
         Loaded .claude/_documentation-standards.md
         Loaded brainstorm/Epic_PRD.md
         Found EPIC-01: Message Creation Service

Phase 1: Generated 5 acceptance criteria mappings
         Identified 2 technical questions
         Computed Confidence Level from resolved ACs and penalties
         Wrote specs/epic-01-message-creation-service/spec.md

EPIC-01 Message Creation Service specified at <computed>% confidence.
Spec: specs/epic-01-message-creation-service/spec.md
```