#!/usr/bin/env bash
# .claude/hooks/warn-growing-docs.sh
input=$(cat)
file_path=$(echo "$input" | jq -r '.tool_input.file_path // empty')

case "$file_path" in
  *spec/*|*brainstorm/*|*docs/*)
    if [[ -f "$file_path" ]]; then
      lines=$(wc -l < "$file_path")
      if [[ "$lines" -gt 1500 ]]; then
        echo "Note: $file_path is now $lines lines — consider archiving resolved Decision Log / Open Issues entries into a dated history file." >&2
        exit 1
      fi
    fi
    ;;
esac
exit 0
