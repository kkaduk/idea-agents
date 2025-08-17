#!/usr/bin/env bash
set -euo pipefail

services=(
  "human-agent"
  "idea-creator-agent"
  "idea-critic-agent"
  "idea-finalizer-agent"
  "orchestration-service"
  "risk-estimator-agent"
)

mkdir -p logs pids
rm -f pids/*.pid

for s in "${services[@]}"; do
  echo "Starting $s ..."
  (
    cd "$s"
    nohup mvn -q -DskipTests spring-boot:run > "../logs/$s.out" 2>&1 & 
    mvn_pid=$!
    # Give Maven a moment to spawn the Java app
    sleep 2
    # Try to find the child Java PID of Maven
    child="$(pgrep -P "$mvn_pid" java | head -n1 || true)"
    # Fallback: best-effort match by command line containing module dir
    if [ -z "$child" ]; then
      child="$(pgrep -f "java .* $s" | head -n1 || true)"
    fi
    echo "${child:-$mvn_pid}" > "../pids/$s.pid"
  )
  echo "→ PID $(cat pids/$s.pid) for $s"
done