#!/bin/sh
set -eu
cd "$(dirname "$0")"
mkdir -p build/classes
find src tests -name '*.java' > build/sources.txt
javac -encoding UTF-8 -d build/classes @build/sources.txt
java -ea -cp build/classes com.sgu.client.ClientTests
