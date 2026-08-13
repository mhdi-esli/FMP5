# Documentation Standards

**Last Updated:** 2026-08-12
**Project:** Financial Messaging Platform

This document is the authoritative source for documentation conventions. All advisors and skills must consult this before generating or modifying documentation.

---

## Required Artifacts

| Artifact | New Products | Existing Products |
|----------|--------------|-------------------|
| C4 Model (L1 + L2 mandatory, L3 recommended) | ✅ Required from day 1 | Phase 1 |
| ADRs (all key decisions) | ✅ Required from day 1 | Phase 2 |
| DDD Context Map | ✅ Required from day 1 | Phase 3 |
| OpenAPI spec | ✅ Required from day 1 | Phase 3 |
| AsyncAPI spec (if async comms exist) | ✅ Required from day 1 | Phase 3 |

---

## Repository Structure

```
/documents/
├── openapi/
│   └── openapi.yaml          # OpenAPI 3.0 spec (SpringDoc → export)
├── asyncapi/                 # AsyncAPI specs (if applicable)
├── c4/                       # C4 model diagrams (PlantUML)
├── adr/                      # Architecture Decision Records (MADR format)
└── context-map/              # DDD Context Maps (Mermaid)
```

---

## C4 Model Rules

| Level | Tool | Naming |
|-------|------|--------|
| L1 (System Context) | PlantUML | `system-context.puml` |
| L2 (Container) | PlantUML | `container.puml` |
| L3 (Component) | PlantUML | `component-*.puml` |

**Required:** L1 + L2 from day 1; L3 recommended.

---

## ADR Format & Immutability Rule

| Rule | Details |
|------|---------|
| Format | MADR (Markdown Anywhere) or Persian headings |
| Location | `/documents/adr/` |
| Versioning | `0001-descriptive-title.md` |
| Immutability | Never delete ADRs — archive instead |

---

## Context Map Conventions

| Relationship | Mermaid Pattern |
|--------------|-----------------|
| Antagonistic | `A -x B` |
| Conforming | `A -> B` |
| Cooperative | `A <-> B` |
| Waiter | `A => B` |

---

## OpenAPI/AsyncAPI Structure Rules

### OpenAPI (`openapi/openapi.yaml`)
- Valid OpenAPI 3.0.0 YAML
- `info` section (title, version) complete
- All possible responses defined for every path
- Parameters, bodies, and responses use `$ref` to `components`
- Security requirements defined (`securitySchemes` + `security`)

### AsyncAPI (`asyncapi/asyncapi.yaml`)
- Valid AsyncAPI 3.0.0 YAML
- Security requirements defined
- Error handling on separate channels or structured fields

---

## Tool Selection Matrix

| Purpose | Recommended Tool |
|---------|------------------|
| API Specification | SpringDoc (annotations) → export YAML |
| Architecture Decisions | MADR format in `/documents/adr/` |
| C4 Diagrams | PlantUML |
| Context Map | Mermaid |
| Database ERD | draw.io on Confluence |

---

## Generated Spec Workflow

**SpringDoc → OpenAPI YAML:**
1. Annotate controllers with SpringDoc annotations
2. Build app and fetch `/v3/api-docs`
3. Export to `/documents/openapi/openapi.yaml`
4. Commit YAML as single source of truth

---

## Open Issues

None. All required questions answered.

---

## Iteration History

- **2026-08-12** — Created from SAW_102. Added OpenAPI export workflow, C4 model requirements, and MADR format specification.
