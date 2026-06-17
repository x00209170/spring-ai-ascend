#\!/usr/bin/env bash
# Research-report web playground — config page + live agent progress + report preview.
#   ./financial/play-web.sh            # http://localhost:8088
#   RESEARCH_WEB_PORT=9000 ./financial/play-web.sh
#   TUSHARE_TOKEN=xxxx ./financial/play-web.sh   # use real Tushare A-share data
set -euo pipefail
: "${JAVA_HOME:=/Library/Java/JavaVirtualMachines/openjdk-21.jdk/Contents/Home}"
export JAVA_HOME
ROOT="$(cd "$(dirname "$0")/.." && pwd)"; cd "$ROOT"
./mvnw -q -o -f financial/pom.xml \
  -Dexec.mainClass=com.bank.financial.research.web.ResearchWebServer \
  exec:java
