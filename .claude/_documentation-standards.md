# Documentation Standards

**Last Updated:** 2026-08-16
**Completeness:** 100% (6/6 required documentation-related questions answered: Arch Q16–Q18/Q21, Coding Q21–Q22)
**Sources:** `standards/reference/SAW_102-arch-doc-standards.md` (all sections), `standards/00_Architecture_Questionnaire.md`, `standards/00_Coding_Guidelines_Questionnaire.md`

Authoritative source for documentation conventions. All advisors and skills must consult this before generating or modifying documentation. Every rule traces to a cited SAW_102 section or a confirmed answer.

---

## Required Artifacts

Adoption profile: **existing product — phased rollout** (Arch Q16), per SAW_102 §1.

| Artifact | This project | Source |
|----------|--------------|--------|
| C4 Model (L1+L2 mandatory, L3 recommended) | Phase 1 | SAW_102 §1 |
| ADRs (all key decisions) | Phase 2 | SAW_102 §1 |
| DDD Context Map | Phase 3 | SAW_102 §1 |
| OpenAPI spec | Phase 3 | SAW_102 §1 |
| AsyncAPI spec | Phase 3 — only once async is introduced (Arch Q14) | SAW_102 §1 |
| Data Catalog | Data-driven products only; ERD + data dictionary in the interim | SAW_102 §8 |

---

## Repository Structure

All docs live in-repo under `/documents/` per SAW_102 §2. `docs/` and `specs/` are kept as SDD working artifacts (Arch Q17). Doc changes commit in the **same PR** as the related code (SAW_102 §2).

```
/documents/
├── c4/         README.md → links to draw.io diagrams on Confluence
├── adr/        README.md (index) + ADR-NNNN.short-title.md
├── cmap/       context_map.md (Mermaid)
├── openapi/    openapi.yaml (OpenAPI 3.x)
├── asyncapi/   asyncapi.yaml (AsyncAPI 3.0.0)
└── datamodels/ README.md → ERD links + data dictionary
```

---

## C4 Model Rules

| Level | Required? | Tool | Source |
|-------|-----------|------|--------|
| L1 System Context | Mandatory | **draw.io on Confluence** | SAW_102 §3 |
| L2 Containers | Mandatory | draw.io on Confluence | SAW_102 §3 |
| L3 Components | Mandatory per §3 (recommended per §1) | draw.io on Confluence | SAW_102 §3 |
| L4 Code | Optional (critical logic only) | draw.io on Confluence | SAW_102 §3 |

- Diagrams are drawn in **draw.io via the Confluence plugin — NOT Mermaid, NOT PlantUML** (SAW_102 §3). *(Corrects earlier PlantUML/`.puml` claim.)*
- `c4/README.md` holds the Confluence links. Confluence is available (Arch Q18).
- Container diagrams with data stores link to the Data Catalog entry in their description (SAW_102 §3).

---

## ADR Format & Immutability Rule

- **File name:** `ADR-NNNN.short-title-in-kebab-case.md` — number from `0001`, `.` separator, kebab title (SAW_102 §4). *(Corrects earlier `0001-title.md`.)*
- **Template:** MADR, **Persian**, exactly per SAW_102 §4 (Coding Q21). Persian status values: `پیشنهادی` / `پذیرفته شده` / `رد شده` / `منسوخ شده` / `جایگزین شده توسط ADR-XXXX`.
- **Immutability (SAW_102 §4):** never edit or delete an **accepted** ADR. To change a decision: write a **new** ADR, reference the old one, set the old status to `جایگزین شده توسط ADR-[new]`. Editing is allowed only while status is `پیشنهادی`.
- Diagrams **inside** ADRs use **Mermaid** (SAW_102 §4).

---

## Context Map Conventions

- Location `/documents/cmap/context_map.md`; tool **Mermaid**, embedded (SAW_102 §5).
- Every relationship between Bounded Contexts is labeled with a **standard DDD pattern** (SAW_102 §5): **OHS** · **Partnership** · **SK** (Shared Kernel) · **C/S** (Customer-Supplier) · **CF** (Conformist) · **ACL** (Anticorruption Layer) · **SW** (Separate Ways). *(Corrects the earlier non-DDD Antagonistic/Conforming/Cooperative/Waiter labels.)*

---

## OpenAPI/AsyncAPI Structure Rules

**OpenAPI** (`openapi/openapi.yaml`, SAW_102 §6): version **3.x** (`3.0.3`/`3.1.0`); every path/param/field/schema has a `description`; all responses (success **and** error) defined per path; request/response bodies `$ref` to `components/schemas` (no inline schemas); `securitySchemes` + `security` defined; `info.version` matches the deployed `/vN`.
**Generation:** produced from **SpringDoc annotations, then exported to `/documents/openapi/openapi.yaml` in CI** (Coding Q22).

**AsyncAPI** (`asyncapi/asyncapi.yaml`, SAW_102 §7): version **3.0.0**; message payloads `$ref` to `components/schemas`; every channel/operation/message/field has a `description`; channels follow the **SWA_101 §7** naming convention; broker auth in `securitySchemes`. Applies once async is introduced (Arch Q14).

---

## Tool Selection Matrix

| Content | Tool | Location | Source |
|---------|------|----------|--------|
| C4 diagrams (all levels) | **draw.io** (Confluence) | linked from `c4/README.md` | SAW_102 §9 |
| ERD diagrams | **draw.io** (Confluence) | linked from `datamodels/README.md` | SAW_102 §9 |
| Context Map | **Mermaid** | `cmap/context_map.md` | SAW_102 §9 |
| Diagrams inside ADRs | **Mermaid** | the ADR `.md` | SAW_102 §9 |
| Prose docs | **Markdown** | `/documents/` | SAW_102 §9 |
| OpenAPI / AsyncAPI | **YAML** | `/documents/` | SAW_102 §9 |

**Never** draw.io for Context Maps or ADR diagrams; **never** Mermaid for C4 or ERD (SAW_102 §9).

---

## Open Issues

- **AsyncAPI + Context Map** are deferred to Phase 3 / first async epic (Arch Q14) — no artifact required yet.
- **Data Catalog** applicability `[TBD]` — SAW_102 §8 scopes it to "data-driven products"; confirm whether this service's data assets require registration, else use interim ERD + data dictionary under `datamodels/`.
- No UX/design reference — headless REST API; Swagger UI is a developer tool, not a product surface (Arch Q21).

---

## Iteration History

- **2026-08-16** — Rebuilt from SAW_102 as ground truth (Arch Q19). Corrected substantive drift: C4 tool PlantUML/`.puml` → **draw.io on Confluence** (§3); Context Map labels replaced with the **standard DDD patterns** OHS/Partnership/SK/C-S/CF/ACL/SW (§5); ADR file naming `0001-title.md` → **`ADR-NNNN.kebab-title.md`** and immutability rule made explicit (§4); ADR language set to **Persian per the MADR template** (Coding Q21); repo layout aligned to SAW_102 §2 (`cmap/`, `datamodels/` added); OpenAPI generation via SpringDoc-export-in-CI recorded (Coding Q22). Added phased-adoption profile (Arch Q16), Confluence availability (Arch Q18), and Data Catalog / async Open Issues.
- **2026-08-12** — Created from SAW_102 (first pass).
