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

# Init Docs Skill

Generates domain-driven documentation from the actual codebase — not from specs or intent.

## File Layout

```
{project root}/
├── docs/
│   ├── domain-index.md
│   └── domain-<name>.md              (one per domain)
└── .claude/skills/init-docs/
    └── SKILL.md
```

## Input

`init-docs <domain>` — if a domain is named, scope to it. If omitted, process every domain found in the codebase's top-level module/package structure.

## Workflow

### Phase 0 — Preflight

1. Determine the domain(s) to document
   - If domain is named: scope to that domain
   - If omitted: enumerate domains from actual package structure
   - Don't assume domains match `EPIC-NN` names
2. Where a domain corresponds to a `specs/<epic-slug>/` directory, note the mapping for traceability
   - Don't force a mapping that isn't actually there

### Phase 1 — Analyze (per domain)

Read and analyze:
- Source code (entities, services, controllers, repositories)
- Tests (unit, integration, E2E)
- API definitions (annotations, contracts)
- Configuration files

Extract:
- **Domain Model**: Entities, Value Objects, Aggregates
- **API Endpoints**: Requests, Responses, Contracts
- **Key Components**: Services, Repositories, Controllers
- **Features**: Documented business capabilities
- **Business Rules**: Validations, constraints, workflows

**Evidence Requirements:**
- Every documented behavior must be evidenced by actual code
- A specific method, validation, or test assertion
- If behavior is ambiguous or not clearly enforced → note under "Unverified / needs review"
- Never assert what isn't in the code

**Acceptance Criteria Cross-Reference:**
- Cross-reference AC IDs where traceable via:
  - Naming conventions
  - Code comments
  - Test names
- Never guess a mapping that isn't there

### Phase 2 — Compute Coverage

```
coverage = (source files actually read and reflected
            / total relevant source files in this domain) × 100
```

Flag any skipped files under Open Issues, by name.

### Phase 3 — Generate/Update Documentation

**`docs/domain-index.md`:**
- Navigation map
- One-line overview and link per domain
- Last updated timestamp

**`docs/domain-<name>.md`:**
- Domain Model (entities, value objects, aggregates)
- API Endpoints/Requests/Responses/Contracts
- Key Behaviors/Business Rules/Workflows
- Key Components (services, repositories, controllers)
- Features
- Acceptance Criteria (where traceable)
- Open Issues (unverified items, skipped files)
- Iteration History

**Update Rules:**
- Preserve human-added content
- Embed version marker (hash of analyzed files)
- Append dated Iteration History entry
- Log what actually changed (new endpoints, changed entities, etc.)
- Never silently regenerate entire file

---

## Document Template

```markdown
# Domain: <name>

**Generated:** YYYY-MM-DD
**Coverage:** XX%
**Version Hash:** <hash of analyzed files>

---

## Domain Model

### Entities

| Entity | Description | Key Fields |
|--------|-------------|------------|
| ... | ... | ... |

### Value Objects

| Value Object | Description |
|--------------|-------------|
| ... | ... |

### Aggregates

| Aggregate | Root Entity | Members |
|-----------|-------------|---------|
| ... | ... | ... |

---

## API Endpoints

### <Endpoint Path>

| Method | Path | Request | Response |
|--------|------|---------|----------|
| POST | /api/v1/messages | MessageRequest | MessageResponse |

**Contract:**
- Request fields: ...
- Response fields: ...
- Error codes: ...

---

## Key Behaviors / Business Rules

### <Behavior Name>

**Evidence:** `ClassName.methodName()` line XX

**Rule:** <description of what the code does>

**Tests:** `TestClassName.testMethodName()`

---

## Key Components

| Component | Type | Responsibility |
|-----------|------|----------------|
| MessageCreationService | Service | Orchestrates message creation |
| MessageRepository | Repository | Persists messages |
| MessageController | Controller | REST API endpoint |

---

## Features

| Feature | Status | Evidence |
|---------|--------|----------|
| Create MT200 Message | Implemented | `MessageController.createMessage()` |

---

## Acceptance Criteria

| AC ID | Description | Status | Evidence |
|-------|-------------|--------|----------|
| AC-001 | Create Valid Message | ✅ Verified | `MessageCreationServiceTest.createMessage_validRequest_returnsDraftStatus()` |

---

## Unverified / Needs Review

| Item | Issue | Location |
|------|-------|----------|
| ... | Behavior not clearly tested | ... |

---

## Open Issues

- Skipped files: (list by name)
- Ambiguous behaviors: (list)
- Missing tests: (list)

---

## Iteration History

| Date | Changes |
|------|---------|
| YYYY-MM-DD | Initial documentation generated |
```

---

## Hard Rules

1. **Never assert unevidenced behavior** — Flag ambiguous/unverified instead of asserting
2. **Never fabricate Coverage** — Compute from actual files analyzed
3. **Never overwrite human edits** — Preserve non-generated content, log changes
4. **Never guess AC mappings** — Only trace via naming, comments, or tests
5. **Read-only** — Never modify code, tests, or specs

---

## Example Usage

```
/init-docs messaging

Phase 0: Found 1 domain: messaging
         Maps to specs/epic-01-message-creation-service/

Phase 1: Analyzing 15 source files...
         - 3 entities
         - 2 services
         - 1 controller
         - 2 repositories
         - 4 test classes

Phase 2: Coverage: 100% (15/15 files analyzed)

Phase 3: Generated docs/domain-index.md
         Generated docs/domain-messaging.md

Documentation complete: 100% coverage, 0 unverified items
```
