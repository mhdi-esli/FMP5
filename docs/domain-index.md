# Domain Index

**Last Updated:** 2026-07-26
**Version Hash:** `a1b2c3d4e5` (based on 22 source files + 4 test files)

---

## Domains

| Domain | Status | Description | Link |
|--------|--------|-------------|------|
| messaging | ✅ Implemented | Financial messaging domain — MT200 message creation, validation, and persistence | [domain-messaging.md](domain-messaging.md) |

---

## Overview

The Financial Messaging Platform currently contains one domain:

- **messaging** — Core message creation service for MT200 financial institution transfers. Handles validation, message ID generation, persistence, and error reporting with Persian-language messages.

---

## Cross-References

| Domain | Associated Epic | Spec Location |
|--------|----------------|---------------|
| messaging | EPIC-01 | `specs/epic-01-message-creation-service/spec.md` |

---

## Notes

- Institution domain (EPIC-03) and Message Definition domain (EPIC-02) are stubbed but not yet independently documented.
- All code is under the `com.bank.messaging` base package.
