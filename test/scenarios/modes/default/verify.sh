#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../../.." && pwd)"
PASS=0
FAIL=0
ERRORS=""
TIMEOUT=60

# COPILOT_CLI_PATH is optional — the SDK discovers the bundled CLI automatically.
# Set it only to override with a custom binary path.
if [ -n "${COPILOT_CLI_PATH:-}" ]; then
  echo "Using CLI override: $COPILOT_CLI_PATH"
fi

# Ensure GITHUB_TOKEN is set for auth
if [ -z "${GITHUB_TOKEN:-}" ]; then
  if command -v gh &>/dev/null; then
    export GITHUB_TOKEN=$(gh auth token 2>/dev/null)
  fi
fi
if [ -z "${GITHUB_TOKEN:-}" ]; then
  echo "⚠️  GITHUB_TOKEN not set and gh auth not available. E2E runs will fail."
fi
echo ""

# Use gtimeout on macOS, timeout on Linux
if command -v gtimeout &>/dev/null; then
  TIMEOUT_CMD="gtimeout"
elif command -v timeout &>/dev/null; then
  TIMEOUT_CMD="timeout"
else
  echo "⚠️  No timeout command found. Install coreutils (brew install coreutils)."
  echo "   Running without timeouts."
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

  echo "$output"

  # Check that the response shows evidence of tool usage or SDK-related content
  if [ "$code" -eq 0 ] && [ -n "$output" ]; then
    if echo "$output" | grep -qi "SDK\|readme\|grep\|match\|search"; then
      echo "✅ $name passed (confirmed tool usage or SDK content)"
      PASS=$((PASS + 1))
    else
      echo "⚠️  $name ran but response may not confirm tool usage"
      echo "❌ $name failed (expected pattern not found)"
      FAIL=$((FAIL + 1))
      ERRORS="$ERRORS\n  - $name"
    fi
  elif [ "$code" -eq 124 ]; then
    echo "❌ $name failed (timed out after ${TIMEOUT}s)"
    FAIL=$((FAIL + 1))
    ERRORS="$ERRORS\n  - $name (timeout)"
  else
    echo "❌ $name failed (exit code $code)"
    FAIL=$((FAIL + 1))
    ERRORS="$ERRORS\n  - $name"
  fi
  echo ""
}

echo "══════════════════════════════════════"
echo " Verifying modes/default samples"
echo " Phase 1: Build"
echo "══════════════════════════════════════"
echo ""

# TypeScript: install + compile
check "TypeScript (install)" bash -c "cd '$SCRIPT_DIR/typescript' && npm install --ignore-scripts 2>&1"
check "TypeScript (build)"   bash -c "cd '$SCRIPT_DIR/typescript' && npm run build 2>&1"

# Python: install + syntax
check "Python (install)" bash -c "python3 -c 'import copilot' 2>/dev/null || (cd '$SCRIPT_DIR/python' && pip3 install -r requirements.txt --quiet 2>&1)"
check "Python (syntax)"  bash -c "python3 -c \"import ast; ast.parse(open('$SCRIPT_DIR/python/main.py').read()); print('Syntax OK')\""

# Go: build
check "Go (build)" bash -c "cd '$SCRIPT_DIR/go' && go build -o default-go . 2>&1"

# C#: build
check "C# (build)" bash -c "cd '$SCRIPT_DIR/csharp' && dotnet build --nologo -v q 2>&1"


echo "══════════════════════════════════════"
echo " Phase 2: E2E Run (timeout ${TIMEOUT}s each)"
echo "══════════════════════════════════════"
echo ""

# TypeScript: run
run_with_timeout "TypeScript (run)" bash -c "cd '$SCRIPT_DIR/typescript' && node dist/index.js"

# Python: run
run_with_timeout "Python (run)" bash -c "cd '$SCRIPT_DIR/python' && python3 main.py"

# Go: run
run_with_timeout "Go (run)" bash -c "cd '$SCRIPT_DIR/go' && ./default-go"

# C#: run
run_with_timeout "C# (run)" bash -c "cd '$SCRIPT_DIR/csharp' && dotnet run --no-build 2>&1"


echo "══════════════════════════════════════"
echo " Results: $PASS passed, $FAIL failed"
echo "══════════════════════════════════════"
if [ "$FAIL" -gt 0 ]; then
  echo -e "Failures:$ERRORS"
  exit 1
fi
