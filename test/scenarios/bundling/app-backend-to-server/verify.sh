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
APP_PID=""

cleanup() {
  if [ -n "${APP_PID:-}" ] && kill -0 "$APP_PID" 2>/dev/null; then
    kill "$APP_PID" 2>/dev/null || true
    wait "$APP_PID" 2>/dev/null || true
  fi
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
  if [ "$code" -eq 0 ] && [ -n "$output" ]; then
    echo "$output"
    echo "✅ $name passed (got response)"
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

# Helper: start an HTTP server, curl it, stop it
run_http_test() {
  local name="$1"
  local start_cmd="$2"
  local app_port="$3"
  local max_retries="${4:-15}"

  printf "━━━ %s ━━━\n" "$name"

  # Start the HTTP server in the background
  eval "$start_cmd" &
  APP_PID=$!

  # Wait for server to be ready
  local ready=false
  for i in $(seq 1 "$max_retries"); do
    if curl -sf "http://localhost:${app_port}/chat" -X POST \
       -H "Content-Type: application/json" \
       -d '{"prompt":"ping"}' >/dev/null 2>&1; then
      ready=true
      break
    fi
    if ! kill -0 "$APP_PID" 2>/dev/null; then
      break
    fi
    sleep 1
  done

  if [ "$ready" = false ]; then
    echo "Server did not become ready"
    echo "❌ $name failed (server not ready)"
    FAIL=$((FAIL + 1))
    ERRORS="$ERRORS\n  - $name (server not ready)"
    kill "$APP_PID" 2>/dev/null || true
    wait "$APP_PID" 2>/dev/null || true
    APP_PID=""
    echo ""
    return
  fi

  # Send the real test request with timeout
  local output=""
  local code=0
  if [ -n "$TIMEOUT_CMD" ]; then
    output=$($TIMEOUT_CMD "$TIMEOUT" curl -sf "http://localhost:${app_port}/chat" \
      -X POST -H "Content-Type: application/json" \
      -d '{"prompt":"What is the capital of France?"}' 2>&1) && code=0 || code=$?
  else
    output=$(curl -sf "http://localhost:${app_port}/chat" \
      -X POST -H "Content-Type: application/json" \
      -d '{"prompt":"What is the capital of France?"}' 2>&1) && code=0 || code=$?
  fi

  # Stop the HTTP server
  kill "$APP_PID" 2>/dev/null || true
  wait "$APP_PID" 2>/dev/null || true
  APP_PID=""

  if [ "$code" -eq 0 ] && [ -n "$output" ]; then
    echo "$output"
    if echo "$output" | grep -qi 'Paris\|capital\|France'; then
      echo "✅ $name passed (got response with expected content)"
      PASS=$((PASS + 1))
    else
      echo "❌ $name failed (response missing expected content)"
      FAIL=$((FAIL + 1))
      ERRORS="$ERRORS\n  - $name (no expected content)"
    fi
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

# Kill any stale processes on the test ports from previous interrupted runs
for test_port in 18081 18082 18083 18084; do
  stale_pid=$(lsof -ti ":$test_port" 2>/dev/null || true)
  if [ -n "$stale_pid" ]; then
    echo "Killing stale process on port $test_port (PID $stale_pid)"
    kill $stale_pid 2>/dev/null || true
  fi
done

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
echo " Verifying app-backend-to-server samples"
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
check "Go (build)" bash -c "cd '$SCRIPT_DIR/go' && go build -o app-backend-to-server-go . 2>&1"

# C#: build
check "C# (build)" bash -c "cd '$SCRIPT_DIR/csharp' && dotnet build --nologo -v q 2>&1"


echo "══════════════════════════════════════"
echo " Phase 2: E2E Run (timeout ${TIMEOUT}s each)"
echo "══════════════════════════════════════"
echo ""

# TypeScript: start server, curl, stop
run_http_test "TypeScript (run)" \
  "cd '$SCRIPT_DIR/typescript' && PORT=18081 CLI_URL=$COPILOT_CLI_URL node dist/index.js" \
  18081

# Python: start server, curl, stop
run_http_test "Python (run)" \
  "cd '$SCRIPT_DIR/python' && PORT=18082 CLI_URL=$COPILOT_CLI_URL python3 main.py" \
  18082

# Go: start server, curl, stop
run_http_test "Go (run)" \
  "cd '$SCRIPT_DIR/go' && PORT=18083 CLI_URL=$COPILOT_CLI_URL ./app-backend-to-server-go" \
  18083

# C#: start server, curl, stop (extra retries for JIT startup)
run_http_test "C# (run)" \
  "cd '$SCRIPT_DIR/csharp' && PORT=18084 COPILOT_CLI_URL=$COPILOT_CLI_URL dotnet run --no-build" \
  18084 \
  30

echo "══════════════════════════════════════"
echo " Results: $PASS passed, $FAIL failed"
echo "══════════════════════════════════════"
if [ "$FAIL" -gt 0 ]; then
  echo -e "Failures:$ERRORS"
  exit 1
fi
