#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

echo "══════════════════════════════════════════════════════════════════"
echo " SDK Scenario Verification"
echo "══════════════════════════════════════════════════════════════════"
echo ""

# ── CLI path (optional) ──────────────────────────────────────────────
# COPILOT_CLI_PATH is optional for most scenarios — the SDK discovers
# the bundled CLI automatically.  Set it only to override, or for
# server-mode scenarios (TCP, multi-user) that spawn copilot-core directly.
if [ -n "${COPILOT_CLI_PATH:-}" ]; then
  echo "Using CLI override: $COPILOT_CLI_PATH"
else
  echo "No COPILOT_CLI_PATH set — SDKs will use their bundled CLI."
fi

# ── Auth ────────────────────────────────────────────────────────────
if [ -z "${GITHUB_TOKEN:-}" ]; then
  if command -v gh &>/dev/null; then
    export GITHUB_TOKEN=$(gh auth token 2>/dev/null || true)
  fi
fi
if [ -z "${GITHUB_TOKEN:-}" ]; then
  echo "⚠️  GITHUB_TOKEN not set"
fi
echo ""

# ── Discover verify scripts ────────────────────────────────────────
VERIFY_SCRIPTS=()
while IFS= read -r script; do
  VERIFY_SCRIPTS+=("$script")
done < <(find "$SCRIPT_DIR" -mindepth 3 -maxdepth 3 -name verify.sh -type f | sort)

echo "Found ${#VERIFY_SCRIPTS[@]} scenarios"
echo ""

# ── Run all ─────────────────────────────────────────────────────────
TOTAL=0; PASSED=0; FAILED=0; SKIPPED=0
declare -a NAMES=()
declare -a STATUSES=()

for script in "${VERIFY_SCRIPTS[@]}"; do
  rel="${script#"$SCRIPT_DIR"/}"
  name="${rel%/verify.sh}"
  log_file="$TMP_DIR/${name//\//__}.log"

  NAMES+=("$name")
  TOTAL=$((TOTAL + 1))

  printf "Running %-40s " "$name..."

  if bash "$script" >"$log_file" 2>&1; then
    # Check if output contains SKIP
    if grep -q "^SKIP:" "$log_file"; then
      printf "⏭  SKIP\n"
      STATUSES+=("SKIP")
      SKIPPED=$((SKIPPED + 1))
    else
      printf "✅ PASS\n"
      STATUSES+=("PASS")
      PASSED=$((PASSED + 1))
    fi
  else
    # Even on failure, check for SKIP (e.g., build failed but skip message present)
    if grep -q "^SKIP:" "$log_file"; then
      printf "⏭  SKIP\n"
      STATUSES+=("SKIP")
      SKIPPED=$((SKIPPED + 1))
    else
      printf "❌ FAIL\n"
      STATUSES+=("FAIL")
      FAILED=$((FAILED + 1))
    fi
  fi
done

echo ""

# ── Summary ─────────────────────────────────────────────────────────
echo "══════════════════════════════════════════════════════════════════"
echo " Summary"
echo "══════════════════════════════════════════════════════════════════"
printf '%-40s | %-6s\n' "Scenario" "Status"
printf '%-40s-+-%-6s\n' "----------------------------------------" "------"
for i in "${!NAMES[@]}"; do
  printf '%-40s | %-6s\n' "${NAMES[$i]}" "${STATUSES[$i]}"
done
echo ""
echo "Total: $TOTAL | Passed: $PASSED | Failed: $FAILED | Skipped: $SKIPPED"

if [ "$FAILED" -gt 0 ]; then
  echo ""
  echo "══════════════════════════════════════════════════════════════════"
  echo " Failed Scenario Logs"
  echo "══════════════════════════════════════════════════════════════════"
  for i in "${!NAMES[@]}"; do
    if [ "${STATUSES[$i]}" = "FAIL" ]; then
      echo ""
      echo "━━━ ${NAMES[$i]} ━━━"
      tail -20 "$TMP_DIR/${NAMES[$i]//\//__}.log"
    fi
  done
  exit 1
fi
