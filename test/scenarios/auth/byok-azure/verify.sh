#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PASS=0
FAIL=0
ERRORS=""
TIMEOUT=60

if command -v gtimeout &>/dev/null; then
  TIMEOUT_CMD="gtimeout"
elif command -v timeout &>/dev/null; then
  TIMEOUT_CMD="timeout"
else
  TIMEOUT_CMD=""
fi

check() {
  local name="$1"
  shift
  printf "━━━ %s ━━━\n" "$name"
  if output=$("$@" 2>&1); then
    echo "$output"
    echo "✅ $name passed"
    PASS=$((PASS + 1))
  else
    echo "$output"
    echo "❌ $name failed"
    FAIL=$((FAIL + 1))
    ERRORS="$ERRORS\n  - $name"
  fi
  echo ""
}

run_with_timeout() {
  local name="$1"
  shift
  printf "━━━ %s ━━━\n" "$name"
  local output=""
  local code=0
  if [ -n "$TIMEOUT_CMD" ]; then
    output=$($TIMEOUT_CMD "$TIMEOUT" "$@" 2>&1) && code=0 || code=$?
  else
    output=$("$@" 2>&1) && code=0 || code=$?
  fi
  if [ "$code" -eq 0 ] && [ -n "$output" ]; then
    echo "$output"
    echo "✅ $name passed"
    PASS=$((PASS + 1))
  elif [ "$code" -eq 124 ]; then
    echo "${output:-(no output)}"
    echo "❌ $name failed (timed out after ${TIMEOUT}s)"
    FAIL=$((FAIL + 1))
    ERRORS="$ERRORS\n  - $name (timeout)"
  else
    echo "${output:-(empty output)}"
    echo "❌ $name failed (exit code $code)"
    FAIL=$((FAIL + 1))
    ERRORS="$ERRORS\n  - $name"
  fi
  echo ""
}

echo "══════════════════════════════════════"
echo " Verifying auth/byok-azure"
echo "══════════════════════════════════════"
echo ""

check "TypeScript (install)" bash -c "cd '$SCRIPT_DIR/typescript' && npm install --ignore-scripts 2>&1"
check "TypeScript (build)" bash -c "cd '$SCRIPT_DIR/typescript' && npm run build 2>&1"

# C#: build
check "C# (build)" bash -c "cd '$SCRIPT_DIR/csharp' && dotnet build --nologo -v q 2>&1"

if [ -n "${AZURE_OPENAI_ENDPOINT:-}" ] && [ -n "${AZURE_OPENAI_API_KEY:-}" ]; then
  run_with_timeout "TypeScript (run)" bash -c "
    cd '$SCRIPT_DIR/typescript' && \
    output=\$(node dist/index.js 2>&1) && \
    echo \"\$output\" && \
    echo \"\$output\" | grep -qi 'Paris\|capital\|France\|response\|hello'
  "
  run_with_timeout "C# (run)" bash -c "
    cd '$SCRIPT_DIR/csharp' && \
    output=\$(dotnet run --no-build 2>&1) && \
    echo \"\$output\" && \
    echo \"\$output\" | grep -qi 'Paris\|capital\|France\|response\|hello'
  "
else
  echo "⚠️  WARNING: E2E run was SKIPPED — only build was verified, not runtime behavior."
  echo "   To run fully: set AZURE_OPENAI_ENDPOINT and AZURE_OPENAI_API_KEY."
  echo ""
fi

echo "══════════════════════════════════════"
echo " Results: $PASS passed, $FAIL failed"
echo "══════════════════════════════════════"
if [ "$FAIL" -gt 0 ]; then
  echo -e "Failures:$ERRORS"
  exit 1
fi
