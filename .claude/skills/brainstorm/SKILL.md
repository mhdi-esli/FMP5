---
name: brainstorm
description: >
  Generates a structured questionnaire to scope a new epic, feature, platform,
  or system, then converts the answered questionnaire into an Epic PRD with
  functional/non-functional requirements, acceptance criteria, risks, and a
  decision log. Use this whenever the user wants to brainstorm requirements
  for something substantial (a new platform, a major feature, an enterprise
  system), asks for a PRD or requirements doc, or says things like "let's
  scope this out" or "help me define what we're building" — even if they
  don't use the words "questionnaire" or "PRD" explicitly. Prefer this over
  ad-hoc interactive questioning for anything with more than a handful of
  open decisions.
---

# Brainstorm Skill

Turns a one-line idea into a reviewable **Epic PRD** through an asynchronous,
document-based Q&A cycle. The user fills out checkbox questionnaires at their
own pace; **this skill never guesses or fills in an answer on their behalf.**

## Where files live

Two separate locations — do not conflate them:

- **Skill files (this package, read-only):** `.claude/skills/brainstorm/SKILL.md`
  and `.claude/skills/brainstorm/references/`. Load a reference only at the
  phase that needs it, so this file is the only thing read on most invocations.
- **Runtime artifacts (what you create/read):** the project-root `brainstorm/`
  directory — `00_Scope_Questionnaire.md`, the questionnaire body file(s), and
  `Epic_PRD.md`. Create `brainstorm/` if it does not exist. `Epic_PRD.md` here
  is exactly what the downstream `write-spec` skill consumes.

## Dispatch — run this selector first, every invocation

1. Glob `brainstorm/*Questionnaire*.md`. Sort matches by the leading numeric
   filename prefix (`00_`, `01_`, `02_`, …), NOT directory-listing order.
   Files without a numeric prefix (e.g. `Questionnaire.md`) sort after prefixed
   ones. This fixed order keeps the prompt prefix stable across runs.
2. Classify state and jump to the matching phase:
   - **No `*Questionnaire*.md` files at all** → **Phase 0**.
   - **Only `00_Scope_Questionnaire.md` present** (no body file) → parse the
     scope answer. Unanswered/ambiguous → report it (per Phase 2's rules) and
     stop. Answered → **Phase 1**.
   - **Any body file present** (a `*Questionnaire*.md` other than the scope
     file) → **Phase 2**. If its gate passes → **Phase 3**.

Each phase does its work and STOPS with a clear hand-back to the user. This is
an asynchronous cycle — never chain phases past a stop, and never answer a
question yourself to keep moving.

---

## Phase 0 — Scope check

Create `brainstorm/00_Scope_Questionnaire.md` with exactly ONE question:

```
# Scope

## 1. What's the scale of this initiative? (select one, required)
[ ] Single feature
[ ] Medium product initiative
[ ] Large / enterprise platform
[ ] Multi-year program
```

- Append `← recommended` to exactly one option, chosen from the user's one-line
  idea. A hub described as "universal", spanning multiple institutions,
  networks, or standards, defaults to **Large / enterprise platform** (or
  Multi-year program if it explicitly spans years/phases). Label only — no box
  is checked, including this one.
- Stop. Tell the user to open the file, check one box, and re-invoke.

## Phase 1 — Generate the full questionnaire

Re-read the answered scope. Load `references/domain_question_bank.md` and follow
its copy rules. Then branch on the chosen scale:

- **Single feature** or **Medium product initiative** → one file
  `brainstorm/Questionnaire.md`. All six domains become numbered sections
  (`## 1. Business`, `## 2. Messaging Standards`, …); renumber questions
  `### 1.1`, `### 1.2`, ….
- **Large / enterprise platform** or **Multi-year program** → six separate
  files, one per domain, keeping the numeric prefixes so different stakeholders
  can own their own file:
  `01_Business_Questionnaire.md`, `02_Messaging_Standards_Questionnaire.md`,
  `03_Architecture_Questionnaire.md`, `04_Security_Compliance_Questionnaire.md`,
  `05_Operations_Questionnaire.md`, `06_Agentic_Decisions_Questionnaire.md`.

Adapt the seed questions to the specific idea (add a clearly-needed question,
drop a plainly-irrelevant one, re-judge each `← recommended`), but keep every
formatting rule below intact. Stop. Tell the user which file(s) were created and
that the skill will wait for them to be filled in, then re-invoked.

## Phase 2 — Answer gate

Read every questionnaire file in the fixed numeric order from Dispatch. Parse
each question with the answer-parsing rules below. Then partition mandatory
(`(required)`) questions into: **answered**, **ambiguous**, **incomplete**,
**unanswered**.

- If any mandatory question is **unanswered, ambiguous, or incomplete**: STOP.
  Report the full list grouped by file → section, keeping the three problem
  buckets separate so the user knows whether to *fix* (ambiguous/incomplete) or
  *finish* (unanswered) each one. Do not proceed to Phase 3. Do not fill in an
  answer yourself — not even an "obviously recommended" one.
- If every mandatory question is answered → proceed to **Phase 3**.

(Optional questions never block the gate, but note unanswered ones — they become
Open Issues in Phase 3.)

## Phase 3 — Generate / update the PRD

Runs only when Phase 2's gate passed for every mandatory question.

1. Load `references/epic_prd_template.md`. Populate every section from the
   parsed answers. Give each requirement a stable ID (BR-/FR-/NFR-/SEC-/CMP-/AC-).
2. **Decision Log:** for every question whose selected answer differs from its
   labeled `← recommended` option, add a row — the question (file § number), the
   recommendation, the choice made, and why it matters. (Questions with no
   recommendation, or where the user picked the recommendation, add no row.)
3. **Confidence Level:** compute it per the formula below — never assert a number.
4. **Open Issues:** list every gap that holds confidence below 100% — each open
   high-impact risk, each unanswered optional question, each load-bearing
   unconfirmed assumption. Confidence < 90% does **not** block generation; it is
   reported, with the shortfall itemized here.
5. Compute the version marker signature and write it into the top-of-file HTML
   comment.
6. Write `brainstorm/Epic_PRD.md` — via the **update rules** below if it already
   exists.
7. Report the computed Confidence Level and where the file was written.

---

## Questionnaire formatting rules

- Each question offers **2–5 options**. Don't pad to a fixed count; don't omit a
  legitimate option to stay under one.
- Tag every question with ONE parenthetical combining select-mode and
  necessity: `(select one, required)`, `(select all that apply, optional)`, etc.
- At most **one** option per question may carry `← recommended`, as a text label
  appended to the option — **never** a pre-checked box.
- Write every box as `[ ]` — a literal space between the brackets, never `[]`,
  never `[x]` at creation time.
- Add a `Free-text (required|optional): ______` line to any question that needs
  a specific named entity, number, or SLA value no closed choice can capture
  (e.g. which domestic clearing systems, target throughput, RTO/RPO).

Canonical shape:

```
## 2. Messaging standards
Which ISO 20022 (MX) message families must the platform support at launch? (select all that apply, required)
[ ] pacs.008 / pacs.009 (customer/FI credit transfer) ← recommended
[ ] pain.001 / pain.002 (payment initiation/status)
[ ] camt.05x (cash management/reporting)
[ ] Other: ______________________________________
```

## Answer-parsing rules

- **select one** — answered iff **exactly one** box is `[x]`. Two or more checked
  → **ambiguous** (never resolve it yourself). Zero checked → **unanswered**.
- **select all that apply** — answered iff **at least one** box is `[x]`. Zero
  → **unanswered**.
- A `← recommended` label is never a checkbox. An untouched recommendation is
  **not** an answer.
- A malformed box (anything other than `[ ]` or `[x]`, e.g. `[]`, `[X ]`) counts
  as **not** a checked box; if that leaves the question unsatisfied, it is
  unanswered.
- **Free-text:** a blank is "filled" only when the underscores/placeholder are
  replaced with real content (whitespace or leftover underscores = empty).
  - A question whose only input is a required free-text line is answered iff that
    line is filled.
  - A question with boxes **and** a `Free-text (required)` line needs both a valid
    box selection and a filled blank; box-checked-but-blank-empty →
    **incomplete**.
  - `[x] Other: ______` checked with an empty blank → **incomplete** (a checked
    "Other" with no text is not a usable answer).

## Confidence formula

```
confidence = round( (answered_mandatory / total_mandatory) × 100 )
                    − 5 × (risks with Impact=high AND Status=open)
```

Clamp to `[0, 100]`. `total_mandatory` counts every `(required)` question across
all questionnaire files, including the scope question. Because Phase 2 gates on
all mandatory questions being answered, the ratio is normally 100% when Phase 3
runs — so open high-impact risks are what actually move the number. Compute it
from the parsed files every time; if the ratio is ever below 100% here, that is a
bug in the gate, not something to paper over. Report as
`Confidence Level: 85% — 3 high-impact risks open`.

## Version marker & update rules

- **Signature:** a deterministic fingerprint of the *answer set* — sensitive to
  *which* options are checked, not just how many, so swapping one answer for
  another changes it (a bare total would not). Practical encoding: for each
  required question in fixed file/number order, append the checked option
  position(s) plus a `+` if a required free-text is filled, e.g.
  `1a·2ac·3b+…`; record that string (or a short checksum of it) as the
  signature. No timestamps or randomness — an unchanged answer set yields the
  same signature.
- Write it into the top HTML comment:
  `<!-- generated from questionnaire state: <fingerprint> -->`.
- **If `Epic_PRD.md` already exists, this is an UPDATE, not a rewrite:**
  1. Read the existing file, including its stored signature.
  2. **Preserve any content a human added** that does not derive from a
     questionnaire answer — extra prose, hand-written requirements, edited
     wording. Refresh only what the answers drive; never clobber human edits.
  3. Recompute the signature. If it equals the stored one AND the regenerated
     sections match what's on disk, the answers are unchanged — say so and make
     no substantive edit.
  4. Otherwise, append a dated entry (newest first) to **Iteration History**
     describing what changed — derive the description by comparing the
     regenerated sections against the existing ones (which requirements/answers
     moved), not from the fingerprint alone. Use today's date from the
     environment context. Never rewrite existing Iteration History or Decision
     Log entries — only append.

---

## Hard rules

1. **Never pre-check a checkbox** when creating a questionnaire — including the
   `← recommended` option. Every box is created as `[ ]`.
2. **Never answer a question on the user's behalf**, even under time pressure or
   when the answer seems obvious. A human makes every call.
3. **Never fabricate a Confidence Level** — always compute it from the parsed
   files.
4. **Never silently rewrite a human-edited `Epic_PRD.md`** — preserve
   non-generated content and log what changed in Iteration History.
5. **Always read questionnaire files in fixed numeric filename order**, never
   directory-listing order.
6. Never resolve a multi-checked select-one — report it as ambiguous.
7. Never treat an untouched recommendation, an empty free-text, or a malformed
   box as an answer.

## Exit criteria

A run is done when **either**:
- a questionnaire was created/left for the user and the run stopped with a clear
  hand-back (Phase 0, Phase 1, or a Phase 2 gate that found gaps); **or**
- every mandatory question across every file is answered (or explicitly flagged
  back to the user), and `brainstorm/Epic_PRD.md` has been generated/updated with
  an accurate, computed Confidence Level.

Crossing 90% confidence is **reported, not required** — an honest 70% PRD that
itemizes its gaps under Open Issues is a valid, complete run.
