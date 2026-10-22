#!/bin/sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
for f in "$ROOT"/.run/*.pid; do
 [ -f "$f" ] || continue
 pid=$(cat "$f")
 if ps -p "$pid" -o command= 2>/dev/null | grep -F "$ROOT" >/dev/null; then kill "$pid"; fi
 rm -f "$f"
done
