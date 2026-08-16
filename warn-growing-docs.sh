#!/usr/bin/env bash
# .claude/hooks/warn-growing-docs.sh
input=$(cat)
file_path=$(echo "$input" | jq -r '.tool_input.file_path // empty')

case "$file_path" in
  *spec/*|*brainstorm/*|*docs/*|*standards/*|*decisions.md|*.claude/_*.md)
    if [[ -f "$file_path" ]]; then
      lines=$(wc -l < "$file_path")
      # standards/.claude reference docs have a tighter ~150-line cap by
      # design (set-standards); everything else uses the general 1500 cap.
      case "$file_path" in
        *.claude/_*.md) threshold=150 ;;
        *decisions.md) threshold=30 ;;  # roughly matches the 30-entry archival trigger
        *) threshold=1500 ;;
      esac
      if [[ "$lines" -gt "$threshold" ]]; then
        echo "Note: $file_path is now $lines lines (cap: ~$threshold) — consider archiving resolved Decision Log / Open Issues / Iteration History entries into a dated history or archive file." >&2
        exit 1
      fi
    fi
    ;;
esac
exit 0
