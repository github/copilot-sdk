#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../../.." && pwd)"
PASS=0
FAIL=0
ERRORS=""
TIMEOUT=60

# Skip if copilot-core doesn't support the relay subcommand
if [ -n "${COPILOT_CLI_PATH:-}" ]; then
  if ! "$COPILOT_CLI_PATH" relay --help &>/dev/null; then
    echo "SKIP: copilot-core binary does not support the relay subcommand"
    exit 0
  fi
fi

cleanup() {
  echo ""
  if [ -n "${RELAY_PID:-}" ] && kill -0 "$RELAY_PID" 2>/dev/null; then
    echo "Stopping relay (PID $RELAY_PID)..."
    kill "$RELAY_PID" 2>/dev/null || true
  fi
  echo "Stopping Docker container..."
  docker compose -f "$SCRIPT_DIR/docker-compose.yml" down --timeout 5 2>/dev/null || true
}
trap cleanup EXIT

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

# Kill any stale processes on test ports from previous interrupted runs
for test_port in 3000 4000; do
  stale_pid=$(lsof -ti ":$test_port" 2>/dev/null || true)
  if [ -n "$stale_pid" ]; then
    echo "Cleaning up stale process on port $test_port (PID $stale_pid)"
    kill $stale_pid 2>/dev/null || true
  fi
done
docker compose -f "$SCRIPT_DIR/docker-compose.yml" down --timeout 5 2>/dev/null || true

# ── Resolve GITHUB_TOKEN (prefer env, fall back to gh CLI) ───────────
if [ -z "${GITHUB_TOKEN:-}" ]; then
  if command -v gh &>/dev/null; then
    echo "No GITHUB_TOKEN set, using 'gh auth token'..."
    GITHUB_TOKEN=$(gh auth token 2>/dev/null) || true
  fi
  if [ -z "${GITHUB_TOKEN:-}" ]; then
    echo "❌ No GitHub token found."
    echo "   Either: export GITHUB_TOKEN=your-token"
    echo "   Or:     gh auth login"
    exit 1
  fi
fi
export GITHUB_TOKEN

# ── Resolve copilot-core binary (needed for relay) ───────────────────
echo "══════════════════════════════════════"
echo " Resolving copilot-core"
echo "══════════════════════════════════════"
echo ""

if [ -z "${COPILOT_CLI_PATH:-}" ]; then
  # Try to resolve from the TypeScript sample's node_modules
  TS_DIR="$SCRIPT_DIR/typescript"
  if [ -d "$TS_DIR/node_modules/@github/copilot" ]; then
    COPILOT_CLI_PATH="$(node -e "console.log(require.resolve('@github/copilot'))" 2>/dev/null || true)"
  fi
  # Fallback: check PATH
  if [ -z "${COPILOT_CLI_PATH:-}" ]; then
    COPILOT_CLI_PATH="$(command -v copilot-core 2>/dev/null || true)"
  fi
fi
if [ -z "${COPILOT_CLI_PATH:-}" ]; then
  echo "❌ Could not find copilot-core binary."
  echo "   Set COPILOT_CLI_PATH or run: cd typescript && npm install"
  exit 1
fi
COPILOT_CORE="$COPILOT_CLI_PATH"
echo "✅ copilot-core binary ready: $COPILOT_CORE"
echo ""

# ── Start the relay ──────────────────────────────────────────────────
RELAY_PORT=4000
RELAY_PID=""

echo "══════════════════════════════════════"
echo " Starting relay on port $RELAY_PORT"
echo "══════════════════════════════════════"
echo ""

"$COPILOT_CORE" relay --port "$RELAY_PORT" &
RELAY_PID=$!
sleep 2

if kill -0 "$RELAY_PID" 2>/dev/null; then
  echo "✅ Relay running (PID $RELAY_PID)"
else
  echo "❌ Relay failed to start"
  exit 1
fi

# Verify relay health
if curl -sf http://localhost:$RELAY_PORT/health > /dev/null 2>&1; then
  echo "✅ Relay health check passed"
else
  echo "⚠️  Relay health check failed (may still be starting)"
fi
echo ""

# ── Build and start container ────────────────────────────────────────
echo "══════════════════════════════════════"
echo " Building and starting copilot-core container"
echo "══════════════════════════════════════"
echo ""

docker compose -f "$SCRIPT_DIR/docker-compose.yml" up -d --build

# Wait for copilot-core to be ready
echo "Waiting for copilot-core to be ready..."
for i in $(seq 1 30); do
  if (echo > /dev/tcp/localhost/3000) 2>/dev/null; then
    echo "✅ copilot-core is ready on port 3000"
    break
  fi
  if [ "$i" -eq 30 ]; then
    echo "❌ copilot-core did not become ready within 30 seconds"
    docker compose -f "$SCRIPT_DIR/docker-compose.yml" logs
    exit 1
  fi
  sleep 1
done
echo ""

export COPILOT_CLI_URL="localhost:3000"

echo "══════════════════════════════════════"
echo " Phase 1: Build client samples"
echo "══════════════════════════════════════"
echo ""

# TypeScript: install + compile
check "TypeScript (install)" bash -c "cd '$SCRIPT_DIR/typescript' && npm install --ignore-scripts 2>&1"
check "TypeScript (build)"   bash -c "cd '$SCRIPT_DIR/typescript' && npm run build 2>&1"

# Python: install + syntax
check "Python (install)" bash -c "cd '$SCRIPT_DIR/python' && pip3 install -r requirements.txt --quiet 2>&1"
check "Python (syntax)"  bash -c "python3 -c \"import ast; ast.parse(open('$SCRIPT_DIR/python/main.py').read()); print('Syntax OK')\""

# Go: build
check "Go (build)" bash -c "cd '$SCRIPT_DIR/go' && go build -o container-relay-go . 2>&1"


echo "══════════════════════════════════════"
echo " Phase 2: E2E Run (timeout ${TIMEOUT}s each)"
echo "══════════════════════════════════════"
echo ""

# TypeScript: run
run_with_timeout "TypeScript (run)" bash -c "cd '$SCRIPT_DIR/typescript' && node dist/index.js"

# Python: run
run_with_timeout "Python (run)" bash -c "cd '$SCRIPT_DIR/python' && python3 main.py"

# Go: run
run_with_timeout "Go (run)" bash -c "cd '$SCRIPT_DIR/go' && ./container-relay-go"


echo "══════════════════════════════════════"
echo " Results: $PASS passed, $FAIL failed"
echo "══════════════════════════════════════"
if [ "$FAIL" -gt 0 ]; then
  echo -e "Failures:$ERRORS"
  exit 1
fi
