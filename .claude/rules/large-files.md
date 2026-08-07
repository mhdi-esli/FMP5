---
paths:
  - "**/*.log"
  - "target/**"
  - "brainstorm/**"
  - "spec/**"
---
# Large file handling

- Never Read an entire file that's large (logs, generated specs, build
  output). Use Grep first to locate the relevant pattern or section, and
  only read the specific lines you actually need.
- If a full read is genuinely necessary, use Read with offset/limit to pull
  a bounded range rather than the whole file.
- Treat anything over ~500 lines as "large" for this rule, even outside the
  paths above.
- If Grep alone answers the question — confirming an error string exists,
  finding a specific Decision Log entry — stop there. Don't follow up with
  a full Read just to double-check.
