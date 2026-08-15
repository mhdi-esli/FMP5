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

### Phase 0 — Seed check (runs first, every invocation)

If `brainstorm/Epic_PRD.md` exists and has a populated Agentic Decisions section (implementation stack, testing strategy, CI/CD approach, ADR practice, branching strategy): note which answers can be legitimately carried over. This is different from a recommendation — it's citing a decision that already went through human confirmation in `brainstorm`, so it's fine to pre-fill it as an actual answer (not a recommended-but-unchecked option), clearly labeled `(carried over from Epic_PRD.md)` so it's visibly distinct from a fresh suggestion.

### Phase 0b — External standards check (runs once, after Phase 0)

If `standards/reference/*.md` files are present, treat them as a third legitimate source, same epistemic standing as a PRD carryover — not agent judgment, not a recommendation, an already-authorized policy document. Rules that trace to these files are pre-filled as confirmed content, labeled `(from SWA_101 §N)` / `(from SAW_102 §N)`, never presented as a questionnaire option or `← recommended` suggestion.

Read the two source files in this fixed order, always — never directory-listing order: `SWA_101-comm-standards.md`, then `SAW_102-arch-doc-standards.md`. This isn't arbitrary — it keeps the prompt prefix identical run to run so prompt caching engages on this (the most expensive, longest-lived) input.

Do not inline the full source documents into the ~150-line output files — extract only the specific enforceable rules relevant to each file's scope, and link back to the full source: `See standards/reference/SWA_101-comm-standards.md for the complete standard.` This keeps the 150-line cap intact while the full text stays available as ground truth if a later skill needs to check something not summarized.

**Routing (section-level, not whole-document):**
- `_architecture-reference.md` ← SWA_101 §9 Security, §10 Distributed Tracing, §11 Protocol Selection, and the channel-naming-convention part of §7 only (these shape system topology / integration contracts and would warrant an ADR if changed).
- `_coding-guidelines.md` ← SWA_101 §1 URL Structure, §2 Request Format, §3 Response Format, §4 HTTP Status Codes, §5 Paginated Responses, §6 Data Formatting Rules, the header/body-contract part of §7, and §8 Idempotency Implementation (fits the existing Design Patterns section). These are implementation-level rules applied inside one service, not cross-service topology decisions.
- `_documentation-standards.md` ← all of SAW_102 — it doesn't fit either of the other two files' section lists without distorting them.

### Phase 1 — Generate questionnaires (if not yet created)

Create `standards/00_Architecture_Questionnaire.md` covering: architecture style (Layered / Hexagonal — Ports & Adapters / Clean Architecture / Modular Monolith), module boundaries, dependency-direction rules, and any cross-cutting decision not already captured in the PRD.

Create `standards/00_Coding_Guidelines_Questionnaire.md` covering: naming conventions, package structure, testing-pyramid targets, code review/PR conventions, static analysis/linting tools, documentation conventions.

Follow the exact same formatting rules as `brainstorm`: 2-5 options, `(select one)`/`(select all that apply)`, `(required)`/`(optional)`, exactly one `← recommended` label as text (never pre-checked, except for genuinely carried-over prior decisions as described in Phase 0), always `[ ]` never `[]`.

Stop. Tell the user which files were created and that the skill will wait for them to be filled in.

### Phase 2 — Answer gate

Identical rules to `brainstorm`: a select-one question is answered only with exactly one `[x]`; two or more is ambiguous, report separately; a malformed checkbox (`[]`) is unanswered and flagged separately; a recommended label is never treated as an answer unless it's a Phase-0 carryover, explicitly labeled as such. Stop and report if anything required is unanswered, ambiguous, or malformed.

### Phase 3 — Generate/update the reference documents

Step 1 — Generate content strictly from confirmed answers (checked options, carried-over decisions, Phase 0b external-standards citations, or free-text fields). Never add a convention, pattern, or rule that wasn't actually confirmed — if a section would read better with a detail nobody decided, write `[TBD — needs input]` and list it under Open Issues instead.

Step 2 — Compute:
```
completeness = (answered required questions / total required questions) × 100
```
Report it plainly, per document. Not a gate — a low score with clear Open Issues is a legitimate, useful output, same as everywhere else in this pipeline.

**Update vs. rewrite** (if the reference files already exist): preserve any content a human added directly, compare against an embedded version marker, append a dated Iteration History entry describing what changed, update the marker. Standards evolve — this isn't a one-shot file.

## Required Sections (all three documents)

### `.claude/_architecture-reference.md`

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

Sections:
- **Coding Standards** — language-specific conventions
- **Naming Conventions** — classes, methods, variables, packages
- **Testing Practices** — pyramid targets, coverage, tools
- **Design Patterns** — accepted patterns, when to use
- **Open Issues** — `[TBD — needs input]` items
- **Iteration History** — dated entries on each update

Same length cap.

### `.claude/_documentation-standards.md`

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
2. Never pre-check a recommended option — the only exception is a Phase-0 carryover from `Epic_PRD.md`, and that must be visibly labeled as a carryover, never indistinguishable from a fresh recommendation.
3. Never fabricate the completeness score — compute it from the parsed questionnaire files, per document.
4. Never silently rewrite a human-edited reference document — preserve non-generated content and log what changed.
5. Always write unchecked boxes as `[ ]` — never `[]`.
6. A rule sourced from `standards/reference/*.md` is written into an output file only with its `(from SWA_101 §N)` / `(from SAW_102 §N)` citation intact — never merged into prose indistinguishably from a questionnaire-derived rule. This preserves traceability the same way `write-spec`'s Decision Log traces answers back to technical questions.
7. Always read `standards/reference/*.md` in the fixed order given in Phase 0b (SWA_101, then SAW_102), and always emit the Wiring instruction's file list in the fixed order given above (architecture, coding, documentation) — never let either be determined by a directory listing. A stable read/emit order is what lets prompt caching engage across runs.
