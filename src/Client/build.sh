#!/bin/sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$ROOT"
for module in client-core client-gateway narration-server audio-server client-server; do
  mkdir -p "$module/target"
  rm -rf "$module/target/classes"
  mkdir -p "$module/target/classes"
  find "$module/src/main/java" -name '*.java' | sed 's/^/"/;s/$/"/' > "$module/target/sources.txt"
  if [ "$module" = client-core ]; then
    javac --release 21 -encoding UTF-8 -d "$module/target/classes" "@$module/target/sources.txt"
  else
    javac --release 21 -encoding UTF-8 -cp client-core/client-core.jar -d "$module/target/classes" "@$module/target/sources.txt"
  fi
  if [ -d "$module/src/main/resources" ]; then cp -R "$module/src/main/resources/." "$module/target/classes/"; fi
  printf 'Manifest-Version: 1.0\n' > "$module/target/MANIFEST.MF"
  case "$module" in client-gateway) pkg=gateway;; narration-server) pkg=narration;; audio-server) pkg=audio;; client-server) pkg=web;; *) pkg=;; esac
  if [ -n "$pkg" ]; then printf 'Main-Class: com.sgu.%s.Application\nClass-Path: ../client-core/client-core.jar ../lib/mysql-connector-j-9.7.0.jar\n' "$pkg" >> "$module/target/MANIFEST.MF"; fi
  printf '\n' >> "$module/target/MANIFEST.MF"
  jar --create --file "$module/$module.jar" --manifest "$module/target/MANIFEST.MF" -C "$module/target/classes" .
  printf 'Built %s\n' "$module"
done
