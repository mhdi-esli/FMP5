#!/usr/bin/env bash
# .claude/hooks/check-large-file.sh
input=$(cat)
file_path=$(echo "$input" | jq -r '.tool_input.file_path // empty')
offset=$(echo "$input" | jq -r '.tool_input.offset // empty')
limit=$(echo "$input" | jq -r '.tool_input.limit // empty')

# Hard-block ONLY this explicit allowlist of disposable content.
# Everything else — docs/, spec/, brainstorm/, or anything you hand Claude
# for conformance checking — is never touched by this block, by default.
case "$file_path" in
  *.log|target/*|build/*|node_modules/*|dist/*|*.out)
    if [[ -n "$file_path" && -z "$offset" && -z "$limit" && -f "$file_path" ]]; then
      lines=$(wc -l < "$file_path")
      if [[ "$lines" -gt 500 ]]; then
        echo "This file has $lines lines. Use Grep to find the relevant section first, or Read with offset/limit for a bounded range instead of reading the whole file." >&2
        exit 2
      fi
    fi
    ;;
esac
exit 0
