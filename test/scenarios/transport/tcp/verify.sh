#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../../.." && pwd)"
PASS=0
FAIL=0
ERRORS=""
TIMEOUT=60
SERVER_PID=""
SERVER_PORT_FILE=""

cleanup() {
  if [ -n "$SERVER_PID" ] && kill -0 "$SERVER_PID" 2>/dev/null; then
    echo ""
    echo "Stopping Copilot CLI server (PID $SERVER_PID)..."
    kill "$SERVER_PID" 2>/dev/null || true
    wait "$SERVER_PID" 2>/dev/null || true
  fi
  [ -n "$SERVER_PORT_FILE" ] && rm -f "$SERVER_PORT_FILE"
}
trap cleanup EXIT

# Resolve Copilot CLI binary: use COPILOT_CLI_PATH env var or find the SDK bundled CLI.
if [ -z "${COPILOT_CLI_PATH:-}" ]; then
  # Try to resolve from the TypeScript sample node_modules
  TS_DIR="$SCRIPT_DIR/typescript"
  if [ -d "$TS_DIR/node_modules/@github/copilot" ]; then
    COPILOT_CLI_PATH="$(node -e "console.log(require.resolve('@github/copilot'))" 2>/dev/null || true)"
  fi
  # Fallback: check PATH
  if [ -z "${COPILOT_CLI_PATH:-}" ]; then
    COPILOT_CLI_PATH="$(command -v copilot 2>/dev/null || true)"
  fi
fi
if [ -z "${COPILOT_CLI_PATH:-}" ]; then
  echo "❌ Could not find Copilot CLI binary."
  echo "   Set COPILOT_CLI_PATH or run: cd typescript && npm install"
  exit 1
fi
echo "Using CLI: $COPILOT_CLI_PATH"

# Ensure GITHUB_TOKEN is set for auth
if [ -z "${GITHUB_TOKEN:-}" ]; then
  if command -v gh &>/dev/null; then
    export GITHUB_TOKEN=$(gh auth token 2>/dev/null)
  fi
fi
if [ -z "${GITHUB_TOKEN:-}" ]; then
  echo "⚠️  GITHUB_TOKEN not set and gh auth not available. E2E runs will fail."
fi

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
  if [ "$code" -eq 0 ] && [ -n "$output" ] && echo "$output" | grep -qi "Paris\|capital\|France\|response"; then
    echo "$output"
    echo "✅ $name passed (content validated)"
    PASS=$((PASS + 1))
  elif [ "$code" -eq 0 ] && [ -n "$output" ]; then
    echo "$output"
    echo "❌ $name failed (no meaningful content in response)"
    FAIL=$((FAIL + 1))
    ERRORS="$ERRORS\n  - $name (no content match)"
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
echo " Starting Copilot CLI TCP server"
echo "══════════════════════════════════════"
echo ""

SERVER_PORT_FILE=$(mktemp)
"$COPILOT_CLI_PATH" --headless --auth-token-env GITHUB_TOKEN > "$SERVER_PORT_FILE" 2>&1 &
SERVER_PID=$!

# Wait for server to announce its port
echo "Waiting for server to be ready..."
PORT=""
for i in $(seq 1 30); do
  if ! kill -0 "$SERVER_PID" 2>/dev/null; then
    echo "❌ Server process exited unexpectedly"
    cat "$SERVER_PORT_FILE" 2>/dev/null
    exit 1
  fi
  PORT=$(grep -o 'listening on port [0-9]*' "$SERVER_PORT_FILE" 2>/dev/null | grep -o '[0-9]*' || true)
  if [ -n "$PORT" ]; then
    break
  fi
  if [ "$i" -eq 30 ]; then
    echo "❌ Server did not announce port within 30 seconds"
    exit 1
  fi
  sleep 1
done
export COPILOT_CLI_URL="localhost:$PORT"
echo "Server is ready on port $PORT (PID $SERVER_PID)"
echo ""

echo "══════════════════════════════════════"
echo " Verifying TCP transport samples"
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
check "Go (build)" bash -c "cd '$SCRIPT_DIR/go' && go build -o tcp-go . 2>&1"

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
run_with_timeout "Go (run)" bash -c "cd '$SCRIPT_DIR/go' && ./tcp-go"

# C#: run
run_with_timeout "C# (run)" bash -c "cd '$SCRIPT_DIR/csharp' && dotnet run --no-build 2>&1"


echo "══════════════════════════════════════"
echo " Results: $PASS passed, $FAIL failed"
echo "══════════════════════════════════════"
if [ "$FAIL" -gt 0 ]; then
  echo -e "Failures:$ERRORS"
  exit 1
fi
