#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PASS=0
FAIL=0
ERRORS=""
TIMEOUT=180

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
echo " Verifying auth/gh-app scenario 1"
echo "══════════════════════════════════════"
echo ""

check "TypeScript (install)" bash -c "cd '$SCRIPT_DIR/typescript' && npm install --ignore-scripts 2>&1"
check "TypeScript (build)" bash -c "cd '$SCRIPT_DIR/typescript' && npm run build 2>&1"
check "Python (install)" bash -c "python3 -c 'import copilot' 2>/dev/null || (cd '$SCRIPT_DIR/python' && pip3 install -r requirements.txt --quiet 2>&1)"
check "Python (syntax)" bash -c "python3 -c \"import ast; ast.parse(open('$SCRIPT_DIR/python/main.py').read()); print('Syntax OK')\""
check "Go (build)" bash -c "cd '$SCRIPT_DIR/go' && go mod tidy && go build -o gh-app-go . 2>&1"

# C#: build
check "C# (build)" bash -c "cd '$SCRIPT_DIR/csharp' && dotnet build --nologo -v q 2>&1"

if [ -n "${GITHUB_OAUTH_CLIENT_ID:-}" ] && [ "${AUTH_SAMPLE_RUN_INTERACTIVE:-}" = "1" ]; then
  run_with_timeout "TypeScript (run)" bash -c "
    cd '$SCRIPT_DIR/typescript' && \
    output=\$(printf '\\n' | node dist/index.js 2>&1) && \
    echo \"\$output\" && \
    echo \"\$output\" | grep -qi 'device\|code\|http\|login\|verify\|oauth\|github'
  "
  run_with_timeout "Python (run)" bash -c "
    cd '$SCRIPT_DIR/python' && \
    output=\$(printf '\\n' | python3 main.py 2>&1) && \
    echo \"\$output\" && \
    echo \"\$output\" | grep -qi 'device\|code\|http\|login\|verify\|oauth\|github'
  "
  run_with_timeout "Go (run)" bash -c "
    cd '$SCRIPT_DIR/go' && \
    output=\$(printf '\\n' | ./gh-app-go 2>&1) && \
    echo \"\$output\" && \
    echo \"\$output\" | grep -qi 'device\|code\|http\|login\|verify\|oauth\|github'
  "
  run_with_timeout "C# (run)" bash -c "
    cd '$SCRIPT_DIR/csharp' && \
    output=\$(printf '\\n' | dotnet run --no-build 2>&1) && \
    echo \"\$output\" && \
    echo \"\$output\" | grep -qi 'device\|code\|http\|login\|verify\|oauth\|github'
  "
else
  echo "⚠️  WARNING: E2E run was SKIPPED — only build was verified, not runtime behavior."
  echo "   To run fully: set GITHUB_OAUTH_CLIENT_ID and AUTH_SAMPLE_RUN_INTERACTIVE=1."
  echo ""
fi

echo "══════════════════════════════════════"
echo " Results: $PASS passed, $FAIL failed"
echo "══════════════════════════════════════"
if [ "$FAIL" -gt 0 ]; then
  echo -e "Failures:$ERRORS"
  exit 1
fi
