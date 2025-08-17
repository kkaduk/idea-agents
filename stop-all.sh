#!/usr/bin/env bash
set -euo pipefail

shopt -s nullglob
for f in pids/*.pid; do
  s="$(basename "$f" .pid)"
  pid="$(cat "$f" || true)"
  if [ -n "${pid:-}" ] && kill -0 "$pid" 2>/dev/null; then
    echo "Stopping $s (PID $pid)"
    kill "$pid" || true
    # graceful wait up to 10s
    for i in {1..10}; do
      kill -0 "$pid" 2>/dev/null || break
      sleep 1
    done
    kill -0 "$pid" 2>/dev/null && { echo "Force kill $s"; kill -9 "$pid" || true; }
  else
    echo "No running PID for $s"
  fi
  rm -f "$f"
done