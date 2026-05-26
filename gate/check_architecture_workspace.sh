#!/usr/bin/env bash
# Wave 1 advisory gate for the Structurizr workspace authority migration.
#
# Authority: ADR-0147 (Structurizr Workspace Authority); plan at
# D:\.claude\plans\structurizr-d-chao-workspace-spring-ai-adaptive-hellman.md.
#
# Wave 1 mode: ADVISORY. The script reports violations but exits 0 even when
# the profile validator finds them. Wave 5 flips this to blocking — the
# script begins to exit non-zero on profile violations or byte-drift in
# the generated zone.
#
# Invocation (run via WSL per Rule G-7):
#   wsl -d Ubuntu -- bash -lc "cd /mnt/d/chao_workspace/spring-ai-ascend && bash gate/check_architecture_workspace.sh"
#
# Or directly inside a Linux/WSL shell:
#   bash gate/check_architecture_workspace.sh

set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WORKSPACE="$REPO_ROOT/architecture/workspace.dsl"
TOOL_POM="$REPO_ROOT/tools/architecture-workspace/pom.xml"
NORMALIZED_OUT="$REPO_ROOT/out/architecture/normalized-model.json"

# Wave 1 advisory mode — toggle via env var; Wave 5 commit will set this to 1.
ADVISORY="${ARCHITECTURE_WORKSPACE_ADVISORY:-1}"

# Wave 5 blocking flag (default 0; flipped to 1 in Wave 5 commit).
BLOCKING="${ARCHITECTURE_WORKSPACE_BLOCKING:-0}"

red()    { printf '\033[31m%s\033[0m\n' "$1"; }
green()  { printf '\033[32m%s\033[0m\n' "$1"; }
yellow() { printf '\033[33m%s\033[0m\n' "$1"; }
note()   { printf '%s\n' "$1"; }

if [[ ! -f "$WORKSPACE" ]]; then
  yellow "ARCHITECTURE WORKSPACE: workspace.dsl missing at $WORKSPACE — Wave 1 advisory PASS (skeleton may be pending)"
  exit 0
fi

if [[ ! -f "$TOOL_POM" ]]; then
  yellow "ARCHITECTURE WORKSPACE: tools/architecture-workspace pom missing — Wave 1 advisory PASS (tooling pending)"
  exit 0
fi

mkdir -p "$REPO_ROOT/out/architecture"

note "ARCHITECTURE WORKSPACE: running ProfileValidator tests..."
mvn_exit=0
# `clean` first to avoid Windows/WSL cross-platform file-ownership conflicts
# in target/ when the Windows-side build had previously written there.
( cd "$REPO_ROOT" && ./mvnw -B -q -f "$TOOL_POM" clean test ) || mvn_exit=$?

if [[ $mvn_exit -ne 0 ]]; then
  if [[ "$ADVISORY" == "1" ]]; then
    yellow "ARCHITECTURE WORKSPACE (ADVISORY): tool tests failed (exit $mvn_exit) — would block at Wave 5"
    exit 0
  else
    red "ARCHITECTURE WORKSPACE: tool tests failed (exit $mvn_exit)"
    exit $mvn_exit
  fi
fi

note "ARCHITECTURE WORKSPACE: validating workspace.dsl against profile..."
validate_exit=0
( cd "$REPO_ROOT" && ./mvnw -B -q -f "$TOOL_POM" exec:java -Dexec.args="validate $WORKSPACE" ) || validate_exit=$?

if [[ $validate_exit -ne 0 ]]; then
  if [[ "$ADVISORY" == "1" ]]; then
    yellow "ARCHITECTURE WORKSPACE (ADVISORY): profile violations present (exit $validate_exit) — would block at Wave 5"
    exit 0
  else
    red "ARCHITECTURE WORKSPACE: profile violations (exit $validate_exit)"
    exit $validate_exit
  fi
fi

# Wave 3 will land the fragment idempotency check.
FRAGMENT_CHECK="$REPO_ROOT/gate/lib/check_workspace_fragment_idempotency.py"
if [[ -f "$FRAGMENT_CHECK" ]]; then
  note "ARCHITECTURE WORKSPACE: checking architecture/generated/* idempotency..."
  frag_exit=0
  ( cd "$REPO_ROOT" && python "$FRAGMENT_CHECK" ) || frag_exit=$?
  if [[ $frag_exit -ne 0 ]]; then
    if [[ "$BLOCKING" == "1" ]]; then
      red "ARCHITECTURE WORKSPACE: generated-zone drift (exit $frag_exit)"
      exit $frag_exit
    else
      yellow "ARCHITECTURE WORKSPACE (ADVISORY): generated-zone drift (exit $frag_exit) — would block at Wave 5"
      exit 0
    fi
  fi
fi

green "ARCHITECTURE WORKSPACE: PASS (W1 advisory mode; gate flips to blocking at Wave 5)"
exit 0
