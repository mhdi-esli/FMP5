---
name: init-docs
description: >
  Generates domain-driven documentation by analyzing the actual codebase —
  code, APIs, tests, components, business logic — never by inferring intent
  from the spec. Produces a navigable domain index and one detailed
  document per domain, flagging any behavior that isn't clearly evidenced
  in code or tests rather than asserting it as settled fact. Use this after
  an epic has real, implemented code and the user wants documentation of
  what's actually there — e.g. "document the messaging domain," "generate
  docs for what we've built," or "init-docs messaging."
---

# Init Docs

Generate living, domain-driven documentation from demonstrable implementation reality. Treat source code, API definitions, configuration, and tests as evidence. Do not use specifications, PRDs, task lists, or Jira as evidence of implemented behavior.

## Output layout

```text
{project root}/
├── docs/
│   ├── domain-index.md
│   └── domain-<name>.md
└── .claude/skills/init-docs/
    └── SKILL.md
```

Create one `docs/domain-<name>.md` per documented domain. Use a stable lowercase kebab-case domain name for the filename.

## Input

Invocation: `init-docs <domain>`.

- If `$ARGUMENTS` names a domain, document only that domain.
- If `$ARGUMENTS` is empty, discover every domain represented by the codebase's top-level module or package structure.
- Resolve names against the actual structure; do not assume domain names correspond to epic names.
- Process domains alphabetically by normalized package or module name.
- Within each domain, inventory and read files in lexicographic path order. Never rely on filesystem listing order.

## Evidence policy

A statement about implemented behavior is valid only when it cites concrete evidence from the current codebase, such as a method, validation branch, route declaration, schema, configuration value, or test assertion. Cite the source as `path:line` and name the relevant symbol or test when available.

Use evidence with these limits:

- Source code and runtime configuration demonstrate implemented behavior.
- Tests demonstrate asserted examples and exercised paths; a test name alone does not prove behavior unless its setup and assertions were inspected.
- Comments may clarify code but do not prove behavior that executable code does not enforce.
- API schemas or generated definitions describe a contract only to the extent that the running code binds to or enforces them.
- Specs, PRDs, task lists, Jira tickets, and conversation history may provide a traceability label, but never evidence that behavior exists.
- If evidence conflicts, is incomplete, or leaves enforcement unclear, put the item under **Unverified / needs review** instead of resolving it by inference.

## Workflow — state machine

Run phases in order. Complete all phases for one domain before moving to the next domain.

### Phase 0 — Preflight

1. Find the project root and identify the actual source and test roots.
2. Determine the requested domain set using the input rules above.
3. Build a complete, alphabetically sorted inventory of relevant files for each domain before analysis. Include domain source, tests, controllers or handlers, request/response types, API definitions, services, repositories, persistence mappings, and domain-specific configuration. Exclude generated output, vendored dependencies, build artifacts, and files unrelated to the domain. Record the inclusion boundary so the coverage denominator is reproducible.
4. If an existing `docs/domain-index.md` or target domain document exists, read it before generating changes. Identify existing generated sections, version markers, iteration history, and human-authored content.
5. A domain-to-`specs/<epic-slug>/` mapping may be recorded only when naming, a code comment, a test name, or another explicit cross-reference establishes it. Do not read a spec to discover behavior or force a mapping based on subject similarity.
6. Respect `.claude/hooks/check-large-file.sh`. For source files over approximately 500 lines, locate relevant sections with Grep and then use bounded Read with `offset` and `limit`. Do not bypass the hook or silently omit the file.

### Phase 1 — Analyze (per domain)

Read every inventoried file in lexicographic path order and extract only evidenced facts:

- **Domain Model** — entities, value objects, aggregates, identifiers, relationships, persistence constraints, and lifecycle states actually represented in code.
- **API Endpoints** — method and path, authentication or authorization enforced in code, request fields, response fields, status codes, validation, and error contracts.
- **Key Behaviors / Business Rules / Workflows** — validation branches, state transitions, calculations, ordering, side effects, and failure paths.
- **Key Components** — controllers or handlers, services, repositories, adapters, scheduled jobs, and their demonstrated responsibilities.
- **Features** — user-visible or system-visible capabilities that can be traced to implementation evidence.
- **Tests** — exercised examples and assertions, including important behavior with no observed test coverage.

For every asserted rule, behavior, or workflow, retain at least one evidence citation for the generated document. Put ambiguity, conflicting evidence, apparently dead code, unenforced constraints, and untested claims under **Unverified / needs review**.

Cross-reference an acceptance criterion ID such as `AC-NN` only when that exact ID is traceable through a code symbol, comment, test name, annotation, or explicit cross-reference. Do not infer mappings from similar wording. Reading a spec is permitted only to resolve an already explicit mapping or link; its prose must not become implementation evidence.

### Phase 2 — Compute coverage

For each domain, compute:

```text
coverage = (relevant inventoried files actually read and reflected
            / total relevant inventoried files) × 100
```

Rules:

1. Count files, not classes, methods, or excerpts.
2. A file counts in the numerator only when its relevant content was actually inspected and its evidence or lack of relevant behavior was accounted for in the analysis.
3. Bounded reads may count when all relevant sections found through search were inspected; record that the file was read in bounded sections.
4. Show the numerator, denominator, and percentage, for example `93.3% (14/15 files)`.
5. Never round in a way that implies full coverage: only `N/N` may display `100%`.
6. List every unread or partially unresolved relevant file by path under **Open Issues**, with the reason.
7. If the inventory boundary is uncertain, state that uncertainty under **Open Issues** rather than claiming exhaustive coverage.

### Phase 3 — Generate or update documentation

#### `docs/domain-index.md`

Maintain a navigation map containing one alphabetically ordered entry per known documented domain:

```markdown
- [Domain name](domain-<name>.md) — one-line overview grounded in implementation
```

When scoped to one domain, update that entry without deleting entries for other domains. Preserve human-authored introductions, notes, and organization unless they are demonstrably stale; flag suspected stale content rather than silently removing it.

#### `docs/domain-<name>.md`

Use this section order:

1. Domain Model
2. API Endpoints
   - Requests
   - Responses
   - Contracts
3. Key Behaviors / Business Rules / Workflows
4. Key Components
5. Features
6. Acceptance Criteria (where traceable)
7. Unverified / needs review
8. Coverage
9. Open Issues
10. Iteration History

Attach evidence citations to factual implementation claims. Prefer concise tables when they make endpoint, component, or evidence relationships easier to navigate.

#### Drift marker

Embed a machine-readable marker near the top of each domain document:

```html
<!-- init-docs: domain=<name> source-hash=<sha256> -->
```

Compute `source-hash` from the sorted list of analyzed file paths and their exact contents, including path separators between entries. Use the same deterministic hashing procedure on every run. Do not include timestamps, filesystem metadata, generated documentation, specs, or unread files in the hash. The marker detects source drift; it is not a substitute for analysis.

#### Update safely

- Update existing generated facts surgically rather than replacing the entire document.
- Preserve human-authored content verbatim. If generated and human-authored text cannot be distinguished safely, do not overwrite the uncertain section; add an Open Issue explaining what needs review.
- Never delete an existing claim solely because the current run did not find it. Remove or revise it only when current code provides evidence that it is stale; otherwise move or copy the concern to **Unverified / needs review**.
- Compare the previous marker and documented evidence with the current analysis to identify actual changes.
- On a changed run, append a dated `YYYY-MM-DD` Iteration History entry describing concrete observed changes, such as added endpoints, changed entities, removed validations, or newly evidenced test behavior. Do not describe inferred intent.
- If the source hash and generated facts are unchanged, do not create a cosmetic rewrite or append a misleading change entry.
- When creating a new document, record an initial dated entry stating what was documented and the coverage achieved.

### Phase 4 — Validate

Before reporting completion:

1. Recheck that every asserted business rule, behavior, and workflow has at least one current evidence citation.
2. Recheck every acceptance-criterion mapping for an exact, traceable reference.
3. Reconcile each coverage numerator and denominator against the Phase 0 inventory.
4. Confirm every skipped or unresolved relevant file appears under Open Issues.
5. Confirm human-authored content from existing documents remains present and unchanged unless current evidence justified a clearly logged revision.
6. Confirm the index links to each domain document touched in this run and remains alphabetically ordered.
7. Confirm only files under `docs/` were created or modified by this skill.

## Exit criteria

The skill is done only when every requested domain document reflects current code, includes a reproducible coverage score, identifies all skipped or ambiguous evidence, and makes every asserted behavior traceable to code, runtime configuration, or inspected test assertions.

Report the domains processed, documents created or updated, coverage as `read/total`, unverified-item count, skipped-file count, and whether each source hash changed.

## Hard rules

1. Never assert a business rule, behavior, or workflow that is not directly evidenced in current code, runtime configuration, tests, or an enforcing API definition. Flag ambiguity instead.
2. Never fabricate or estimate coverage. Compute it from the explicit relevant-file inventory and files actually analyzed.
3. Never silently overwrite, normalize, relocate, or delete human-authored documentation.
4. Never guess an acceptance-criterion or epic mapping.
5. This skill may read the repository and create or update documentation under `docs/`; it must never modify source code, tests, specifications, migrations, configuration, or tickets.
6. Always enumerate domains and process files in deterministic alphabetical order.
7. Do not claim completion while any assertion lacks evidence or any skipped file is unreported.
