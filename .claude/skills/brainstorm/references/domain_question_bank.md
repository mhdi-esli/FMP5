# Domain Question Bank — starter questions

This is a **seed** the brainstorm skill copies and adapts when generating
questionnaire files. It is NOT a questionnaire itself — never hand this file
to the user to fill in, and never write answers into it.

How the skill uses it:
- **Large/enterprise or Multi-year scope** → each domain below becomes its own
  file in `brainstorm/`, keeping the numeric prefix: `01_Business_Questionnaire.md`
  … `06_Agentic_Decisions_Questionnaire.md`. Within a file, questions are
  numbered `## 1.`, `## 2.`, …
- **Single feature or Medium scope** → all six domains go into ONE
  `brainstorm/Questionnaire.md` as numbered sections (`## 1. Business`,
  `## 2. Messaging Standards`, …), with questions renumbered `### 1.1`, `### 1.2`, …

Rules that MUST survive the copy (see SKILL.md → Questionnaire formatting rules):
- Every option box is written `[ ]` — a literal space, never `[]`, never pre-checked.
- `← recommended` is a text label on at most one option per question. It is a
  suggestion, not an answer. Never check it.
- Every question is tagged `(select one)` or `(select all that apply)` AND
  `(required)` or `(optional)`.
- Recommendations below reflect THIS project's context (a universal financial
  messaging hub for a Java/Spring team, adaptable to Iran's domestic rails).
  Re-judge them against the user's actual one-line idea; drop or move a
  `← recommended` label if the default no longer fits, but never add a second one.
- Adapt freely: add a project-specific question the idea clearly needs, or drop
  a seed question that plainly doesn't apply — but keep 2–5 options each, and
  don't pad or omit legitimate options to hit a count.
- A `Free-text (…): ______` line captures a specific named entity, number, or
  SLA value that no closed choice can. Add one to any question that needs it.

---

# 01 — Business

## 1. What is the primary business objective for this platform? (select one, required)
[ ] Build a new universal routing/validation hub across multiple networks ← recommended
[ ] Modernize / replace an existing legacy SWIFT gateway
[ ] Add ISO 20022 (MX) capability alongside an existing MT stack
[ ] Consolidate compliance, audit, and reporting for financial messaging
[ ] Other: ______________________________________

## 2. Which institution types must the platform serve? (select all that apply, required)
[ ] Commercial / retail banks ← recommended
[ ] Central bank
[ ] Payment system operators / clearing houses
[ ] Corporates connecting directly
[ ] Fintechs / PSPs
Free-text (optional): any other institution type ______________________________________

## 3. Which networks / rails must be reachable AT LAUNCH? (select all that apply, required)
[ ] SWIFT (FIN / InterAct) ← recommended
[ ] Domestic RTGS (high-value; e.g. Iran SATNA)
[ ] Domestic ACH / retail batch (e.g. Iran PAYA)
[ ] Instant / card scheme rails
[ ] Central bank reporting channel
Free-text (required): name the exact systems/rails in scope for launch ______________________________________

## 4. Which rails or networks are explicitly deferred to a later phase? (select all that apply, optional)
[ ] Cross-border SWIFT
[ ] Additional domestic rails
[ ] Instant payments
[ ] None — everything is launch scope
Free-text (optional): ______________________________________

## 5. What is the expected message volume? (select one, required)
[ ] < 10k messages/day
[ ] 10k – 100k messages/day ← recommended
[ ] 100k – 1M messages/day
[ ] > 1M messages/day
Free-text (required): target peak throughput ______ messages/second

## 6. What is the geographic and regulatory scope? (select one, required)
[ ] Single country, domestic only ← recommended
[ ] Single country + cross-border SWIFT
[ ] Multi-country
[ ] Global
Free-text (required): which jurisdiction(s) / regulator(s) ______________________________________

## 7. Which business KPIs define success? (select all that apply, optional)
[ ] Straight-through-processing (STP) rate
[ ] End-to-end message delivery latency
[ ] Cost per message
[ ] Time to onboard a new institution
[ ] Other: ______________________________________

---

# 02 — Messaging Standards

## 1. Which ISO 15022 (MT) message categories are needed at launch? (select all that apply, required)
[ ] MT2xx — financial-institution transfers (incl. MT202 / MT205) ← recommended
[ ] MT1xx — customer payments (incl. MT103)
[ ] MT3xx — treasury / FX / derivatives
[ ] MT5xx — securities
[ ] MT9xx — cash management & statements (MT900/910/940/950)

## 2. Which ISO 20022 (MX) message families are needed at launch? (select all that apply, required)
[ ] pacs.008 / pacs.009 — customer & FI credit transfer ← recommended
[ ] pain.001 / pain.002 — payment initiation & status
[ ] camt.05x — cash management / reporting
[ ] head.001 — business application header
[ ] Other: ______________________________________

## 3. Which domestic message formats must be ingested or emitted? (select all that apply, required)
[ ] Domestic RTGS / ACH formats (e.g. Iran SATNA / PAYA) ← recommended
[ ] National ISO 20022 variant / local usage guidelines
[ ] Proprietary core-banking host format
[ ] None — SWIFT standards only
Free-text (required): name the exact formats / specification versions ______________________________________

## 4. What is the canonical internal representation? (select one, required)
[ ] Normalize everything to ISO 20022 as the canonical model ← recommended
[ ] Preserve native format inside a metadata envelope
[ ] Dual-canonical — maintain MT and MX side by side
[ ] Other: ______________________________________

## 5. How strict is inbound/outbound validation? (select one, required)
[ ] Strict — schema + network/usage rules; reject on any violation ← recommended
[ ] Schema / syntax only
[ ] Configurable strictness per channel or counterparty

## 6. Is MT↔MX transformation required? (select one, required)
[ ] Yes — bidirectional, CBPR+-style translation ← recommended
[ ] One direction only (specify below)
[ ] No transformation — route messages as-is
Free-text (optional): which direction(s) / mapping guideline ______________________________________

## 7. How is standards versioning and migration handled over time? (select one, required)
[ ] Support multiple versions concurrently, pin version per message ← recommended
[ ] Single active version with a scheduled hard cutover
[ ] Latest version only
Free-text (required): baseline release to target (e.g. SWIFT SR2025, CBPR+ vX) ______________________________________

---

# 03 — Architecture

## 1. What is the deployment model? (select one, required)
[ ] On-premise data center ← recommended
[ ] Private cloud
[ ] Public cloud
[ ] Hybrid
Free-text (optional): specific platform / region constraints ______________________________________

## 2. How is multi-tenancy handled across institutions? (select one, required)
[ ] Shared platform with logical isolation per institution ← recommended
[ ] Dedicated instance per institution
[ ] Single-tenant (one operating institution only)

## 3. What is the core processing paradigm? (select one, required)
[ ] Event-driven with a message-broker backbone ← recommended
[ ] Synchronous request/response
[ ] Hybrid — synchronous ingress, asynchronous core

## 4. What message store and replay capability is required? (select all that apply, required)
[ ] Immutable, append-only message store ← recommended
[ ] Replay by correlation / business identifier
[ ] Point-in-time state reconstruction
[ ] No replay — fire-and-forget

## 5. What are the HA / DR targets? (select one, required)
[ ] Active-active across sites ← recommended
[ ] Active-passive with warm standby
[ ] Single site with backups
Free-text (required): RTO ______ / RPO ______

## 6. What are the throughput and latency SLAs? (select one, required)
[ ] Near-real-time, p99 end-to-end < 1s ← recommended
[ ] Seconds-range
[ ] Batch windows
Free-text (required): sustained ______ msg/s, peak ______ msg/s, p99 latency ______ ms

## 7. Which protocol adapters are needed? (select all that apply, required)
[ ] SWIFT-style gateway (Alliance / SAG-SNL) ← recommended
[ ] IBM MQ / JMS
[ ] REST / JSON ISO 20022 API
[ ] Kafka ingress
[ ] File / SFTP batch
Free-text (optional): any other adapter / protocol ______________________________________

---

# 04 — Security & Compliance

## 1. What message-level signing / PKI is required? (select one, required)
[ ] Per-message digital signatures backed by PKI ← recommended
[ ] Channel-level authentication (mTLS) only
[ ] Both message signatures and channel authentication
[ ] None

## 2. How is data encrypted in transit? (select all that apply, required)
[ ] TLS 1.3 with mutual authentication ← recommended
[ ] Network-level (IPSec / VPN)
[ ] Application-layer payload encryption

## 3. How is data encrypted at rest? (select one, required)
[ ] Full store + DB encryption with HSM/KMS-managed keys ← recommended
[ ] Field-level encryption for sensitive data only
[ ] Disk-level encryption only
Free-text (optional): HSM / KMS product or standard ______________________________________

## 4. What access-control model applies? (select one, required)
[ ] Role-based access control (RBAC) ← recommended
[ ] Attribute-based access control (ABAC)
[ ] RBAC + ABAC hybrid

## 5. What are the audit-trail requirements? (select all that apply, required)
[ ] Immutable, append-only audit log ← recommended
[ ] Tamper-evident (hash-chained) entries
[ ] Full message lineage — who touched what, when
[ ] Long-term regulatory retention
Free-text (required): retention period ______________________________________

## 6. Which regulatory reporting and screening touchpoints are in scope? (select all that apply, required)
[ ] Real-time sanctions screening ← recommended
[ ] AML transaction monitoring
[ ] Central-bank regulatory reporting
[ ] Fraud screening hook
Free-text (required): which lists / regimes (e.g. OFAC, domestic CB) ______________________________________

## 7. What data-residency constraints apply? (select one, required)
[ ] All data must remain in-country ← recommended
[ ] Region-restricted
[ ] No residency constraint
Free-text (optional): jurisdiction specifics ______________________________________

---

# 05 — Operations

## 1. What monitoring and alerting stack will be used? (select one, required)
[ ] Prometheus + Grafana + Alertmanager ← recommended
[ ] ELK / EFK stack
[ ] Vendor APM (Datadog / Dynatrace / New Relic)
[ ] Existing enterprise NMS
Free-text (optional): ______________________________________

## 2. What reconciliation approach is required? (select all that apply, required)
[ ] End-of-day reconciliation against the network ← recommended
[ ] Real-time positional reconciliation
[ ] Nostro / settlement reconciliation
[ ] None

## 3. How are retries and undeliverable messages handled? (select one, required)
[ ] Bounded retries + dead-letter queue with manual replay ← recommended
[ ] Retry with exponential backoff, no hard cap
[ ] Immediate fail to an operations queue
Free-text (required): max retries / backoff policy ______________________________________

## 4. What are the operational SLA targets? (select one, required)
[ ] 24/7 follow-the-sun operations ← recommended
[ ] Business hours + on-call
[ ] Best-effort
Free-text (required): availability target (e.g. 99.95%) ______, support hours ______

## 5. What observability standards apply? (select all that apply, required)
[ ] Distributed tracing (OpenTelemetry) ← recommended
[ ] Structured JSON logging with a correlation id
[ ] Business-level metrics and dashboards
[ ] End-to-end message-flow visualization

## 6. Who owns incident response? (select one, required)
[ ] Dedicated platform operations team ← recommended
[ ] Shared with existing infrastructure operations
[ ] Outsourced / vendor follow-the-sun
Free-text (optional): escalation ownership ______________________________________

---

# 06 — Agentic Decisions

## 1. What is the implementation stack? (select one, required)
[ ] Java 21 + Spring Boot 3.x ← recommended
[ ] Kotlin + Spring
[ ] Another JVM stack
[ ] Non-JVM stack
Free-text (optional): ______________________________________

## 2. Where do coding standards come from? (select one, required)
[ ] This repo's `/standards/*.md`, maintained via the set-standards skill ← recommended
[ ] An external corporate style guide
[ ] Ad-hoc / PR review only
Free-text (optional): ______________________________________

## 3. What specification workflow will the project follow? (select one, required)
[ ] SDD skills: brainstorm → write-spec → plan-tasks → implement-epic → verify-epic ← recommended
[ ] Lightweight design docs
[ ] Ticket-driven, no formal spec

## 4. What is the ADR (architecture decision record) practice? (select one, required)
[ ] One ADR per significant decision, kept in-repo ← recommended
[ ] Decision Log inside the PRD / spec only
[ ] No formal decision records

## 5. What is the testing strategy? (select all that apply, required)
[ ] TDD per unit — red / green / refactor ← recommended
[ ] Integration tests with Testcontainers
[ ] Contract tests for message schemas
[ ] End-to-end tests across adapters
[ ] Load / soak testing

## 6. What CI/CD pipeline is expected? (select one, required)
[ ] Automated build + test + security scan on PR, gated merge ← recommended
[ ] Build + test only
[ ] Manual build / deploy
Free-text (required): platform (GitHub Actions / GitLab CI / Jenkins) ______________________________________

## 7. How is AI-assisted development used and reviewed? (select one, required)
[ ] AI-assisted, with mandatory human review of every change ← recommended
[ ] AI for scaffolding and tests only
[ ] Human-written only
