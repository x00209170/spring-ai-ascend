#!/usr/bin/env bash
# Auto-extracted from gate/check_architecture_sync.sh by gate/lib/extract_rules.sh
# Rule 28 — release_note_baseline_truth. DO NOT HAND-EDIT — re-run extract_rules.sh to refresh.
# Authority: PR-E5.

# Rule 28 — release_note_baseline_truth
# ADR-0049 (whitepaper-alignment remediation P0-1): every docs/logs/releases/*.md
# baseline table MUST match the canonical architecture_sync_gate.allowed_claim
# counts, UNLESS the release note declares itself a historical artifact via
# the marker "Historical artifact frozen at SHA". Closes GATE-SCOPE-GAP for
# release-note baseline drift (Gate Rule 27 only covers README.md).
# ---------------------------------------------------------------------------
_r28_fail=0
if [[ -f docs/governance/architecture-status.yaml ]]; then
  _claim28=$(awk '/^[[:space:]]+architecture_sync_gate:/{flag=1} flag && /allowed_claim:/{print; exit}' docs/governance/architecture-status.yaml)
  if [[ -n "$_claim28" ]]; then
    while IFS= read -r _rf28; do
      [[ -z "$_rf28" ]] && continue
      if grep -qE 'Historical artifact frozen at SHA' "$_rf28"; then
        continue
      fi
      _rfcontent28=$(cat "$_rf28")
      _check_baseline28() {
        _label="$1"; _yaml_re="$2"; _rf_re="$3"
        _expected=$(printf '%s' "$_claim28" | grep -oE "$_yaml_re" | head -1 | grep -oE '^[0-9]+' | head -1)
        [[ -z "$_expected" ]] && return 0
        _rfmatches=$(printf '%s' "$_rfcontent28" | grep -oE "$_rf_re")
        if [[ -z "$_rfmatches" ]]; then
          fail_rule "release_note_baseline_truth" "$_rf28 missing baseline count for '$_label'. Per Gate Rule 28 active release notes must contain a table row matching '$_label | $_expected' or declare 'Historical artifact frozen at SHA <sha>'."
          _r28_fail=1
          return 0
        fi
        while IFS= read -r _rmline; do
          # Release notes use markdown-table format: '| <label> | <number> ... |'.
          # The number appears AFTER the label, so extract the trailing number.
          _actual=$(printf '%s' "$_rmline" | grep -oE '[0-9]+' | tail -1)
          if [[ "$_actual" != "$_expected" ]]; then
            fail_rule "release_note_baseline_truth" "$_rf28 asserts '$_actual' for '$_label' but canonical baseline is '$_expected $_label'. Per Gate Rule 28 active release notes must match the canonical baseline or declare 'Historical artifact frozen at SHA <sha>'."
            _r28_fail=1
          fi
        done <<< "$_rfmatches"
      }
      # Release-note table format: '| §4 constraints | 50 (#1–#50) |', etc.
      _check_baseline28 '§4 constraints' '[0-9]+[[:space:]]+§4[[:space:]]+constraints' '§4[[:space:]]+constraints[[:space:]]*\|[[:space:]]*[0-9]+'
      _check_baseline28 'ADRs' '[0-9]+[[:space:]]+ADRs' '(Active[[:space:]]+)?ADRs[[:space:]]*\|[[:space:]]*[0-9]+'
      _check_baseline28 'gate rules' '[0-9]+[[:space:]]+active[[:space:]]+gate[[:space:]]+rules' '(Active[[:space:]]+)?gate[[:space:]]+rules[[:space:]]*\|[[:space:]]*[0-9]+'
      _check_baseline28 'self-tests' '[0-9]+[[:space:]]+gate[[:space:]]+self-tests' '(Gate[[:space:]]+)?self-test[[:space:]]+cases[[:space:]]*\|[[:space:]]*[0-9]+'
    done < <(find docs/logs/releases -maxdepth 1 -name '*.md' -type f 2>/dev/null | sort || true)
  fi
fi
if [[ $_r28_fail -eq 0 ]]; then pass_rule "release_note_baseline_truth"; fi

# ---------------------------------------------------------------------------
