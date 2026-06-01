#!/usr/bin/env bash
# Auto-extracted from gate/check_architecture_sync.sh by gate/lib/extract_rules.sh
# Rule 29.c — quickstart_smoke_job_present. DO NOT HAND-EDIT — re-run extract_rules.sh to refresh.
# Authority: PR-E5.

# Rule 29.c — quickstart_smoke_job_present (enforcer E107)
# .github/workflows/ci.yml MUST contain a job named quickstart-smoke that
# polls /v1/health.
# ---------------------------------------------------------------------------
_r29c_fail=0
_r29c_path='.github/workflows/ci.yml'
if [[ ! -f "$_r29c_path" ]]; then
  fail_rule "quickstart_smoke_job_present" "$_r29c_path missing — Rule 29.c requires a CI workflow"
  _r29c_fail=1
elif ! grep -qE '^[[:space:]]*quickstart-smoke:' "$_r29c_path" 2>/dev/null; then
  fail_rule "quickstart_smoke_job_present" "$_r29c_path missing job 'quickstart-smoke' — Rule 29.c"
  _r29c_fail=1
elif ! grep -qF '/v1/health' "$_r29c_path" 2>/dev/null; then
  fail_rule "quickstart_smoke_job_present" "$_r29c_path quickstart-smoke job does not poll /v1/health"
  _r29c_fail=1
fi
if [[ $_r29c_fail -eq 0 ]]; then pass_rule "quickstart_smoke_job_present"; fi

# ===========================================================================
# SPI metadata integrity wave (2026-05-18)
# Authority: docs/governance/rules/rule-{75..78}.md
# Rules 75-78 with enforcer rows E108-E111. Prevents the SPI declaration vs
# physical layout drift surfaced by the 2026-05-18 SPI integrity audit
# (T2.B2 extraction left engine.spi empty + orchestration.spi double-claimed
# across two Maven modules + dfx yaml omitting/mis-nesting spi_packages).
# ===========================================================================

# ===========================================================================
# 2026-05-18 rc4 cross-constraint review response prevention wave -- Rule 83
# Authority: docs/governance/rules/rule-83.md
#            + docs/logs/reviews/2026-05-18-l0-rc4-cross-constraint-architecture-review.en.md
#            + docs/logs/reviews/2026-05-18-l0-rc4-cross-constraint-architecture-review-response.en.md
# Closes finding families:
#   P1-3 design-only contracts unregistered / dangling auth  -> Rule 83
# ===========================================================================

# ===========================================================================
# 2026-05-18 rc5 post-response review response prevention wave -- Rules 84-85
# Authority: docs/governance/rules/rule-84.md + rule-85.md
#            + docs/logs/reviews/2026-05-18-l0-rc5-post-response-architecture-review.en.md
#            + docs/logs/reviews/2026-05-18-l0-rc5-post-response-architecture-review-response.en.md
# Closes finding families:
#   P0-1 module-level ARCHITECTURE.md path claim drift after refactor   -> Rule 84
#   P1-2 catalog SPI row not backed by module spi_packages metadata    -> Rule 85
# ===========================================================================


# ---------------------------------------------------------------------------
# Wave history (rc6 -> rc7 -> rc8 prevention waves)
# ===========================================================================
# 2026-05-18 rc6 post-response wave -- Rules 86-87 (E119, E120)
# 2026-05-18 rc8 post-corrective wave -- Rules 88-89 (E121, E122) + Rule 86 fenced-tree-block extension
# Authority cards: docs/governance/rules/rule-86.md, rule-87.md, rule-88.md, rule-89.md
# Reviews:    docs/logs/reviews/2026-05-18-l0-rc6-post-response-architecture-review.en.md
#             docs/logs/reviews/2026-05-18-l0-rc7-post-corrective-architecture-review.en.md
# Responses:  docs/logs/reviews/2026-05-18-l0-rc6-post-response-architecture-review-response.en.md
#             docs/logs/reviews/2026-05-18-l0-rc7-post-corrective-architecture-review-response.en.md
# Closes finding families:
#   rc6 P0-2 root ARCHITECTURE.md 8-module + stale path claims  -> Rule 86 (rc7)
#   rc6 P1-2 status_yaml allowed_claim stale module names        -> Rule 87 (rc7)
#   rc7 P0-1 GraphMemoryRepository ownership corpus drift        -> Rule 86 fenced-tree-block extension (rc8)
#   rc7 P0-2 check_parallel.sh skips Rules 86/87                 -> Rule 88 (rc8)
#   rc7 P1-1 test harness fail-open + hardcoded TOTAL            -> Rule 89 (rc8)
# ===========================================================================

