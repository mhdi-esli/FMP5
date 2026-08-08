# Large file handling

- Hard-blocked by the check-large-file.sh hook: .log files, target/, build/,
  node_modules/, dist/ over 500 lines. Use Grep or a bounded Read for these.
- Everything else — docs/, spec/, brainstorm/ output, or any reference
  document handed to you for conformance checking — is NOT hard-blocked.
  Use judgment: Grep first if you're checking something specific, full Read
  when you genuinely need whole-document context (traceability checks,
  confidence computation, conformance verification against a reference doc).
- Never use cat/less/more as a workaround for a file the hook blocks — the
  answer is Grep or a bounded Read, not bypassing the same check.
