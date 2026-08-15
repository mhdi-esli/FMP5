# FMP5 — Financial Messaging Platform
Spring Boot service creating/validating SWIFT MT200 inter-bank transfer messages.

## Commands
- Build: `mvn clean package`
- Run: `mvn spring-boot:run`
- Test (quiet, use this by default): `mvn test -q -Dstyle.color=never`
- Test, single class: `mvn test -q -Dstyle.color=never -Dtest=<TestClass>`
- Build (no tests): `mvn clean package -DskipTests`

Quiet flags suppress Maven's own build-lifecycle chatter and ANSI escape
codes. Combined with `src/test/resources/logback-test.xml` (root level
WARN), this keeps test-run output to a pass/fail summary instead of full
Spring/Testcontainers startup logs — every SDD skill that runs tests
(`implement-epic`, `verify-epic`) should invoke Maven this way, not with
bare `mvn test`.

## Stack
Java 21 · Spring Boot 3.3.2 · PostgreSQL + Flyway · Caffeine cache · SpringDoc OpenAPI 2.6.0 · Testcontainers 1.20.1 · Lombok

## Structure
```
src/main/java/com/bank/messaging/
├── config/       Security, Cache, OpenAPI
├── controller/   Message, MessageDefinition, Institution
├── dto/
├── entity/       Message, MessageDefinitionMapping, Institution
├── enums/        MessageType, Network, Status, ErrorCodes
├── exception/
├── repository/
└── service/
```

## API
- `POST /api/v1/messages` — create MT200 transfer message
- `/api-docs`, `/swagger-ui.html`

## Message flow
Controller → Validation Service (required fields) → MessageDefinition Service (type/network active) → Institution Service (sender + network compat) → Repository (status: DRAFT | VALIDATION_FAILED)

## Error codes (responses are in Persian)
MSG-001 missing/invalid fields · MSG-003 invalid network · MSG-004 definition not found · MSG-005 definition inactive · MSG-010/011/012 institution validation

## DB
Flyway migrations: `src/main/resources/db/migration/`. Config via `SPRING_DATASOURCE_URL/_USERNAME/_PASSWORD`. Schema introspection available via postgres-mcp — always confirm live structure via MCP before writing migrations or Entity/Repository code (see `.claude/rules/database.md`). Use the read-only `mcp_reader` role for inspection; only the separate `postgres-writer` connection may mutate data, and only when explicitly invoked.

## Security
OAuth2 resource server. JWK: `https://sso.tps.ir/.well-known/openid-configuration/jwks`

## Cache
Caffeine — max 1000 entries, TTL 300s

## Conventions
- Message ID: `MSG-YYYYMMDD-NNNNNN` (daily sequential)
- Validation errors always in Persian
- Integration tests use Testcontainers (needs DB)
- Dev: `spring.jpa.show-sql=true` for SQL logging (test profile keeps this at WARN — see logback-test.xml)
- Large files (logs, build output): Grep/bounded Read, not full Read (see `.claude/rules/large-files.md`, enforced by `.claude/hooks/check-large-file.sh`)

## Epics
EPIC-01 Message Creation ✓ · EPIC-02 Message Definition Mgmt ✓ · EPIC-03 Institution Mgmt ✓

All three were implemented before `set-standards`/`plan-tasks` existed —
none has been checked against `.claude/_architecture-reference.md` /
`_coding-guidelines.md` / `_documentation-standards.md`. Re-run
`/verify-epic` on each once `set-standards` has produced those three
files, to surface any drift before starting new epics.

## SDD

Spec-driven workflow via `.claude/skills/`:

```
set-standards (once, early)  ─┐
init-docs (as code drifts)   ─┼─→ read by every epic's inner loop below
                              ─┘
brainstorm → for each epic: write-spec → plan-tasks → implement-epic → verify-epic
```

`set-standards` and `init-docs` are not fixed pipeline positions — they
run once (or as needed) and their output is consulted by every epic's
inner loop, not produced by it. Only `write-spec → plan-tasks →
implement-epic → verify-epic` is a strict per-epic sequence, always
invoked with an epic argument (e.g. `/write-spec epic-04`), never bare.

Each phase reads from written files, not conversation history — each is
safe to run in a separate Claude Code session/container. File reads
within each skill's Phase 0/Preflight follow a fixed, explicitly-ordered
sequence (see each skill's own SKILL.md) rather than directory-listing
order — this keeps the stable-content prefix identical across runs so
prompt caching actually engages.

## Jira

Self-hosted Jira Data Center at `https://jira.dotin.ir` via the
`mcp-atlassian` MCP server (PAT auth) — not the Atlassian Rovo Cloud
connector, which doesn't support self-hosted instances. Ticket creation
happens as an explicit, human-triggered step after `plan-tasks`, never
automatically. Status transitions ("Ready for Review", never "Done") are
handled by `verify-epic`'s Phase 6, gated on verdict.
