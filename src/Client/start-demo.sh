#!/bin/sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$ROOT"
export CLIENT_HOME="$ROOT" APP_MODE=demo
command -v java >/dev/null || { printf 'Java 21 is required.\n'; exit 1; }
command -v curl >/dev/null || { printf 'curl is required for health checks.\n'; exit 1; }
mkdir -p logs .run
for module in client-gateway narration-server audio-server client-server; do
  [ -f "$module/$module.jar" ] || sh "$ROOT/build.sh"
  case "$module" in client-gateway) port=${GATEWAY_PORT:-8085};; narration-server) port=${NARRATION_PORT:-8083};; audio-server) port=${AUDIO_PORT:-8084};; client-server) port=${WEB_PORT:-8080};; esac
  health=$(curl -fsS "http://127.0.0.1:$port/health" 2>/dev/null || true)
  if [ -n "$health" ]; then
    printf '%s' "$health" | grep -q "\"service\":\"$module\"" || { printf 'Port %s belongs to another service.\n' "$port"; exit 1; }
    printf '%s' "$health" | grep -q '"mode":"demo"' || { printf 'Wrong mode on port %s. Stop that service first.\n' "$port"; exit 1; }
    printf 'Already running: %s :%s\n' "$module" "$port"
  else
    java "-Dclient.home=$ROOT" -jar "$ROOT/$module/$module.jar" > "logs/$module.log" 2>&1 &
    pid=$!; printf '%s\n' "$pid" > ".run/$module.pid"
    tries=0
    while :; do
      kill -0 "$pid" 2>/dev/null || { tail -n 12 "logs/$module.log"; printf 'Could not start %s.\n' "$module"; exit 1; }
      health=$(curl -fsS "http://127.0.0.1:$port/health" 2>/dev/null || true)
      if printf '%s' "$health" | grep -q "\"service\":\"$module\"" && printf '%s' "$health" | grep -q '"mode":"demo"'; then break; fi
      tries=$((tries+1)); [ "$tries" -lt 40 ] || { printf 'Not ready: %s. Check logs.\n' "$module"; exit 1; }; sleep 1
    done
  fi
done
printf 'Open http://localhost:%s/ (DEMO)\n' "${WEB_PORT:-8080}"
