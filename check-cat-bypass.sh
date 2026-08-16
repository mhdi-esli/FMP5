#!/usr/bin/env bash
# .claude/hooks/check-cat-bypass.sh
input=$(cat)
command=$(echo "$input" | jq -r '.tool_input.command // empty')

target=$(echo "$command" | grep -oE '(cat|less|more)\s+\S+' | awk '{print $2}')

if [[ -n "$target" ]]; then
  case "$target" in
    *brainstorm/*|*spec/*|*.md) exit 0 ;;
  esac
  if [[ -f "$target" ]]; then
    lines=$(wc -l < "$target")
    if [[ "$lines" -gt 500 ]]; then
      echo "That file has $lines lines. Use Grep instead of cat/less to inspect it." >&2
      exit 2
    fi
  fi
fi
exit 0
