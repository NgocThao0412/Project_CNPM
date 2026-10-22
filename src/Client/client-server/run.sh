#!/bin/sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$ROOT"
exec java "-Dclient.home=$ROOT" -jar "$ROOT/client-server/client-server.jar"
