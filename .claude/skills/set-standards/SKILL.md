---
name: set-standards
description: >
  Establishes or updates the project's shared architecture, coding, and
  documentation standards through an async, checkbox-based questionnaire
  plus ingestion of authoritative company standards docs — never by
  unilaterally writing opinionated standards as if they were settled
  policy. Every other skill in the SDD pipeline (write-spec, plan-tasks,
  implement-epic, verify-epic) consults these documents before acting.
  Use this once, early in a project, before the first write-spec run —
  e.g. "set up our coding standards," "establish the architecture
  reference," or "let's document our engineering conventions."
---

# Set Standards Skill

Establishes project-wide engineering standards that every other skill in the SDD pipeline consults.

## Objective

Produce three durable reference documents — architecture principles, coding guidelines, and documentation standards — that reflect what the team actually decided, never what the agent assumes is good practice. Anything not traceable to a confirmed answer becomes `[TBD — needs input]`, not a free-written opinion presented as policy.

## File Layout

```
├── standards/
│   ├── reference/
│   │   ├── SWA_101-comm-standards.md      (full source doc, stored verbatim)
│   │   └── SAW_102-arch-doc-standards.md  (full source doc, stored verbatim)
│   ├── 00_Architecture_Questionnaire.md
│   └── 00_Coding_Guidelines_Questionnaire.md
├── .claude/_architecture-reference.md
├── .claude/_coding-guidelines.md
├── .claude/_documentation-standards.md    (SAW_102-derived)
```

No UX/design reference by default — drop it unless the project has an actual user-facing interface. If it does, add `.claude/_ux-reference.md` and `standards/00_UX_Questionnaire.md` following the identical pattern below.

## Workflow — state machine

### Dispatch — select the current phase

Every invocation starts with Phase 0 and then Phase 0b. After those checks,
select exactly one continuation:

1. If either questionnaire is missing, run Phase 1 for only the missing file(s),
   then stop.
2. If both questionnaires exist, run Phase 2.
3. If Phase 2 reports any required unanswered, ambiguous, or malformed answer,
   stop without generating or updating reference documents.
4. Only when Phase 2 passes, run Phase 3.

Do not regenerate an existing questionnaire merely because the other one is
missing, and do not chain a Phase 1 creation directly into Phase 2 in the same
invocation. This is an asynchronous, file-based workflow.

### Phase 0 — Seed check (runs first, every invocation)

If `brainstorm/Epic_PRD.md` exists and has a populated Agentic Decisions section (implementation stack, testing strategy, CI/CD approach, ADR practice, branching strategy): note which answers can be legitimately carried over. This is different from a recommendation — it's citing a decision that already went through human confirmation in `brainstorm`, so it's fine to pre-fill it as an actual answer (not a recommended-but-unchecked option), clearly labeled `(carried over from Epic_PRD.md)` so it's visibly distinct from a fresh suggestion.

### Phase 0b — External standards check (runs once, after Phase 0)

Check the two exact paths below in this fixed order on every invocation; never use
a glob or directory listing to discover or order them:

1. `standards/reference/SWA_101-comm-standards.md`
2. `standards/reference/SAW_102-arch-doc-standards.md`

Read each file if it exists. Treat its content as a third legitimate source,
with the same epistemic standing as a PRD carryover — not agent judgment, not a
recommendation, but already-authorized policy. Rules traced to these files are
confirmed content labeled `(from SWA_101 §N)` / `(from SAW_102 §N)`; never turn
them into questionnaire choices or `← recommended` suggestions. A missing file
contributes no rules and is not a questionnaire-gate failure.

Do not inline a full source document into the ~150-line outputs. Extract only
specific enforceable rules relevant to each output's scope. Every output that
uses SWA_101 content must include:
`See standards/reference/SWA_101-comm-standards.md for the complete standard.`
Every output that uses SAW_102 content must include the corresponding link to
`standards/reference/SAW_102-arch-doc-standards.md`. This keeps the line cap
intact while preserving full ground truth.

**Routing (section-level, not whole-document):**
- `_architecture-reference.md` ← SWA_101 §9 Security, §10 Distributed Tracing, §11 Protocol Selection, and the channel-naming-convention part of §7 only (these shape system topology / integration contracts and would warrant an ADR if changed).
- `_coding-guidelines.md` ← SWA_101 §1 URL Structure, §2 Request Format, §3 Response Format, §4 HTTP Status Codes, §5 Paginated Responses, §6 Data Formatting Rules, the header/body-contract part of §7, and §8 Idempotency Implementation (fits the existing Design Patterns section). These are implementation-level rules applied inside one service, not cross-service topology decisions.
- `_documentation-standards.md` ← all of SAW_102 — it doesn't fit either of the other two files' section lists without distorting them.

### Phase 1 — Generate questionnaires (if not yet created)

Create only the missing questionnaire file(s).

Create `standards/00_Architecture_Questionnaire.md` covering: architecture style
(Layered / Hexagonal — Ports & Adapters / Clean Architecture / Modular Monolith),
module boundaries, dependency-direction rules, and cross-cutting decisions not
already captured in the PRD or external standards.

Create `standards/00_Coding_Guidelines_Questionnaire.md` covering: naming
conventions, package structure, testing-pyramid targets, code review/PR
conventions, static analysis/linting tools, and documentation conventions not
already settled by the PRD or external standards.

Follow the same question shape as `brainstorm`: every question has 2–5 options
and one parenthetical combining mode and necessity, such as `(select one,
required)` or `(select all that apply, optional)`. Exactly one option carries
`← recommended` as text unless the question is already settled by a genuine
Phase-0 carryover. Fresh questions always use `[ ]`, never `[]` or `[x]`.

For a genuine carryover, pre-fill the corresponding answer as `[x]` and append
`(carried over from Epic_PRD.md)` to that option. It is the only pre-check
exception and is an actual answer, not a recommendation. Do not also label a
different option recommended. Omit questions whose only possible purpose would
be to reconfirm an external standard; place those cited rules directly into the
Phase 3 output instead.

Stop. Tell the user exactly which files were created and that the skill will
wait for them to fill in or review the questionnaires and re-invoke it.

### Phase 2 — Answer gate

Read the architecture questionnaire first and coding questionnaire second.
Parse each required question literally:

- `select one` is answered only with exactly one `[x]`; two or more is
  ambiguous.
- `select all that apply` is answered with at least one `[x]`.
- Any checkbox other than `[ ]` or lowercase `[x]`, including `[]` or `[X]`, is
  malformed. Record the question as malformed even if another valid box is
  checked; do not silently normalize it.
- A recommendation label is never an answer.
- A checked answer explicitly labeled `(carried over from Epic_PRD.md)` is a
  valid Phase-0 carryover answer.
- Required free text is answered only by real non-placeholder content. A
  selected `Other` with blank text is unanswered.

Partition required questions into answered, unanswered, ambiguous, and
malformed. Stop and report every problem grouped by questionnaire and category
if any required answer is unanswered, ambiguous, or malformed. Optional gaps do
not block Phase 3; carry them into Open Issues. Never select or repair an answer
for the user.

### Phase 3 — Generate/update the reference documents

Step 1 — Generate content strictly from confirmed answers (checked options,
carried-over decisions, Phase 0b external-standards citations, or filled
free-text fields). Never add a convention, pattern, or rule that was not
confirmed. If a required section needs an undecided detail, write
`[TBD — needs input]` and repeat the gap under Open Issues.

Route questionnaire answers by the document they govern. Architecture
questionnaire answers contribute to `_architecture-reference.md`; coding
questionnaire answers contribute to `_coding-guidelines.md`. Documentation
standards sourced from SAW_102 contribute to `_documentation-standards.md`.
If a questionnaire question explicitly governs documentation policy, count and
route it to the documentation document rather than counting it twice. Record
applicable cross-cutting carryovers only in the document(s) they actually govern.

Step 2 — Compute completeness separately for each document:

```
completeness = round(
  answered required questions routed to this document
  / total required questions routed to this document
  × 100
)
```

The documentation document's denominator is the number of required
questionnaire questions explicitly routed to documentation; externally sourced
SAW_102 sections are authoritative inputs, not questionnaire questions, so they
are not counted in numerator or denominator. If no required questionnaire
question is routed to a document, report `Completeness: N/A — 0 required
questionnaire questions apply; content is sourced from confirmed external
standards/carryovers.` Do not invent `100%` for a zero denominator. Include the
numerator, denominator, percentage (or N/A), and reason for shortfall in each
file. Optional unanswered questions remain Open Issues but do not affect the
score.

Because Phase 2 gates Phase 3 on required answers, a below-100 questionnaire
score during generation indicates a parsing/routing defect; do not paper it over.
Completeness remains informational and is never a threshold gate.

**Update vs. rewrite** — every generated file begins with a deterministic marker:

`<!-- generated from standards state: <fingerprint> -->`

Build `<fingerprint>` from the fixed-order checked-answer positions, filled
free-text values, carried-over answer identifiers, and cited external source
section identifiers. Do not use timestamps or randomness. If a reference file
already exists, read it before editing, compare its marker and generated
sections, preserve human-authored content and unaffected wording, and update
only answer/source-driven content. Append a dated Iteration History entry only
for a substantive change, describing the actual changed decisions or sources;
do not append when state and generated content are unchanged. Then update the
marker. Never use the marker alone as permission to overwrite the file.

## Required Sections (all three documents)

### `.claude/_architecture-reference.md`

Begin with the version marker and a **Completeness** line using Phase 3's
per-document calculation.

Sections:
- **System Architecture** — top-level style and major components
- **Major Design Decisions** — with ADR references where applicable
- **Constraints** — technical, regulatory, operational
- **Key Dependencies** — external systems, libraries, services
- **Development Patterns** — accepted patterns for this project
- **Open Issues** — `[TBD — needs input]` items
- **Iteration History** — dated entries on each update

Keep under ~150 lines — reference material an agent reliably reads in full, not skims.

### `.claude/_coding-guidelines.md`

Begin with the version marker and a **Completeness** line using Phase 3's
per-document calculation.

Sections:
- **Coding Standards** — language-specific conventions
- **Naming Conventions** — classes, methods, variables, packages
- **Testing Practices** — pyramid targets, coverage, tools
- **Design Patterns** — accepted patterns, when to use
- **Open Issues** — `[TBD — needs input]` items
- **Iteration History** — dated entries on each update

Same length cap.

### `.claude/_documentation-standards.md`

Begin with the version marker and a **Completeness** line using Phase 3's
per-document calculation. If SAW_102 is absent and no confirmed questionnaire or
carryover answer supplies a section, retain the section with `[TBD — needs
input]` and list the missing source/decision under Open Issues.

Sections:
- **Required Artifacts** — by product type
- **Repository Structure** — where docs live
- **C4 Model Rules** — levels, tooling, naming
- **ADR Format & Immutability Rule** — MADR/Persian, no deletion
- **Context Map Conventions** — Mermaid patterns, relationships
- **OpenAPI/AsyncAPI Structure Rules** — required sections, versioning
- **Tool Selection Matrix** — when to use which tool
- **Open Issues** — `[TBD — needs input]` items
- **Iteration History** — dated entries on each update

Same length cap and completeness-score treatment.

## Wiring into the rest of the pipeline

This skill's output is inert unless the other four actually read it. Add this to the Phase 0/Preflight of `write-spec`, `plan-tasks`, `implement-epic`, and `verify-epic`, and always list the three files in this exact fixed order — never directory-listing order, so the stable prefix stays identical across every skill's runs and prompt caching engages consistently project-wide:

```
Read these shared standards first, in this fixed order (never directory-listing
order — a stable read order keeps the prompt prefix identical across runs so
prompt caching engages):
  1. .claude/_architecture-reference.md
  2. .claude/_coding-guidelines.md
  3. .claude/_documentation-standards.md
Follow their conventions if they exist. If any are missing, proceed but add a
note under Risks recommending set-standards be run before further epics are built.
```

`brainstorm` doesn't need this — its questionnaire is business-level and predates these documents by design.

## Exit Criteria

Done when both questionnaires (where required) are fully answered or explicitly flagged back to the user, and all three reference documents have been generated or updated with an accurate, computed completeness score.

## Hard Rules

1. Never write a coding convention or architecture rule into any of the three documents unless it traces to a confirmed answer, a carried-over prior decision, a Phase 0b external-standards citation, or explicit free text — never the agent's own judgment presented as settled policy.
2. Never pre-check a fresh recommended option. The only pre-check exception is a Phase-0 carryover from `Epic_PRD.md`; label it `(carried over from Epic_PRD.md)` and treat it as a confirmed answer, not as a recommendation.
3. Never fabricate the completeness score — compute it from the parsed questionnaire files, per document.
4. Never silently rewrite a human-edited reference document — preserve non-generated content and log what changed.
5. Always write unchecked boxes as `[ ]` — never `[]`.
6. A rule sourced from `standards/reference/*.md` is written into an output file only with its `(from SWA_101 §N)` / `(from SAW_102 §N)` citation intact — never merged into prose indistinguishably from a questionnaire-derived rule. This preserves traceability the same way `write-spec`'s Decision Log traces answers back to technical questions.
7. Always check/read the two exact source paths in Phase 0b order (SWA_101, then SAW_102), and always emit the Wiring list in architecture → coding → documentation order. Never let either order come from a glob or directory listing.
8. Never generate reference documents in the same invocation that creates a missing questionnaire; stop for human review first.
9. Never treat external-standard rules as questionnaire questions or count them in completeness arithmetic.
10. Never overwrite human-authored reference content based only on a changed version marker; reconcile source-driven sections and log substantive changes.
