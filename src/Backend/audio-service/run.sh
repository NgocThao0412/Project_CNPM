#!/bin/sh
set -eu
cd "$(dirname "$0")"
exec mvn spring-boot:run "$@"
